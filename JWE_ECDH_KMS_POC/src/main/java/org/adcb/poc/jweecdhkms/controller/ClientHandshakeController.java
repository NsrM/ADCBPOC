package org.adcb.poc.jweecdhkms.controller;

//import jakarta.websocket.HandshakeResponse;
//import jakarta.websocket.server.HandshakeRequest;
import com.nimbusds.jose.*;
import org.adcb.poc.jweecdhkms.model.FixedKeyECDHEncrypter;
import org.adcb.poc.jweecdhkms.model.HandshakeRequest;
import org.adcb.poc.jweecdhkms.model.HandshakeResponse;
import org.adcb.poc.jweecdhkms.util.ClientKeyHolder;
import org.adcb.poc.jweecdhkms.util.HkdfUtil;
import org.adcb.poc.jweecdhkms.util.ServerKeyLoader;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.crypto.KeyAgreement;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/client")
public class ClientHandshakeController {

    private final ClientKeyHolder keyHolder;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ServerKeyLoader serverKeyLoader;

    public ClientHandshakeController(ClientKeyHolder keyHolder, ServerKeyLoader serverKeyLoader) {
        this.keyHolder = keyHolder;
        this.serverKeyLoader = serverKeyLoader;
    }

    @PostMapping("/handshake")
    public ResponseEntity<String> handshake() throws Exception {

        // 1. Generate ephemeral client key pair
        keyHolder.generate();

        String clientPublicKeyBase64 =
                Base64.getEncoder()
                        .encodeToString(keyHolder.getPublicKeyBytes());

        // 2. Call backend handshake
        HandshakeRequest request = new HandshakeRequest();
        request.setClientPublicKey(clientPublicKeyBase64);

        HandshakeResponse response =
                restTemplate.postForObject(
                        "http://localhost:8081/backend/handshake",
                        request,
                        HandshakeResponse.class
                );

        // 3. Decode backend public key
        byte[] backendPublicKeyBytes =
                Base64.getDecoder().decode(response.getBackendPublicKey());

        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey backendPublicKey =
                kf.generatePublic(
                        new X509EncodedKeySpec(backendPublicKeyBytes));

        // 4. Client derives shared secret
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(keyHolder.getPrivateKey());
        ka.doPhase(backendPublicKey, true);

        byte[] sharedSecret = ka.generateSecret();

        System.out.println(
                "Client shared secret hash: " +
                        DigestUtils.sha256Hex(sharedSecret)
        );

        byte[] clientPub = keyHolder.getPublicKeyBytes();
        byte[] backendPub = backendPublicKeyBytes;

// Derive CEK
        byte[] cek = HkdfUtil.deriveCek(
                sharedSecret,
                clientPub,
                backendPub
        );

// Log ONLY hash
        System.out.println(
                "Client CEK hash: " + DigestUtils.sha256Hex(cek)
        );

        return ResponseEntity.ok(
                "Session established. SessionId=" + response.getSessionId()
        );
    }

    @PostMapping("/handshake/v2")
    public ResponseEntity<Map<String, Object>> handshakev2(@RequestBody Map<String, Object> request) throws Exception {
        // 1. Generate ephemeral EC keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp521r1"));
        KeyPair clientEphemeralKP = kpg.generateKeyPair();

        // 2. Load server public key (bundled)
        ECPublicKey serverPub = serverKeyLoader.getServerPublicKey();

        // 3. Build JWE
        JWEHeader header = new JWEHeader.Builder(
                JWEAlgorithm.ECDH_ES_A256KW,
                EncryptionMethod.A256GCM
        )
                .contentType("handshake")
                .build();

        Payload payload = new Payload(request);

        JWEObject jwe = new JWEObject(header, payload);

        FixedKeyECDHEncrypter encrypter =
                new FixedKeyECDHEncrypter(serverPub, clientEphemeralKP);

        jwe.encrypt(encrypter);

        // DEBUG: shared secret verification (optional)
        System.out.println("CLIENT EPHEMERAL PUB: " +
                Base64.getEncoder().encodeToString(clientEphemeralKP.getPublic().getEncoded()));


        // 3. Decode backend public key
        byte[] backendPublicKeyBytes =
                serverPub.getEncoded();

        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey backendPublicKey =
                kf.generatePublic(
                        new X509EncodedKeySpec(backendPublicKeyBytes));

        // 4. Client derives shared secret
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(clientEphemeralKP.getPrivate());
        ka.doPhase(backendPublicKey, true);

        byte[] sharedSecret = ka.generateSecret();

        System.out.println(
                "Client shared secret hash: " +
                        DigestUtils.sha256Hex(sharedSecret)
        );

        byte[] clientPub = clientEphemeralKP.getPublic().getEncoded();
        byte[] backendPub = backendPublicKeyBytes;

// Derive CEK
        byte[] cek = HkdfUtil.deriveCek(
                sharedSecret,
                clientPub,
                backendPub
        );

// Log ONLY hash
        System.out.println(
                "Client CEK hash: " + DigestUtils.sha256Hex(cek)
        );


        return ResponseEntity.ok(
                Map.of("encryptedJwe", jwe.serialize())
        );
    }


//        // 1. Generate YOUR own ephemeral KeyPair
//        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
//        gen.initialize(Curve.P_256.toECParameterSpec());
//        KeyPair myKeyPair = gen.generateKeyPair();
//
//// 2. Prepare the JWE Header with YOUR public key in 'epk'
//        ECKey myPublicKeyJWK = new ECKey.Builder(Curve.P_256, (ECPublicKey) myKeyPair.getPublic()).build();
//        JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
//                .ephemeralPublicKey(myPublicKeyJWK) // This attaches the key inside the JWE
//                .build();
//
//// 3. Manually derive the shared secret using THEIR public key
//// 'theirPublicKey' is the server's long-term static key
//        SecretKey sharedSecret = ECDH.deriveSharedSecret(
//                (ECPublicKey) theirPublicKey,
//                (ECPrivateKey) myKeyPair.getPrivate(),
//                null
//        );
//
//// 4. Encrypt using DirectEncrypter with your derived secret
//        JWEObject jweObject = new JWEObject(header, new Payload("Confidential Data"));
//        jweObject.encrypt(new DirectEncrypter(sharedSecret));
//
//        String finalJWE = jweObject.serialize(); // No separate JSON public key needed
//
//
//        // 2. Call backend handshake
//        HandshakeRequest request = new HandshakeRequest();
//        request.setClientPublicKey(clientPublicKeyBase64);
//
//        HandshakeResponse response =
//                restTemplate.postForObject(
//                        "http://localhost:8081/backend/handshake",
//                        request,
//                        HandshakeResponse.class
//                );
//
//        // 3. Decode backend public key
//        byte[] backendPublicKeyBytes =
//                Base64.getDecoder().decode(response.getBackendPublicKey());
//
//        KeyFactory kf = KeyFactory.getInstance("EC");
//        PublicKey backendPublicKey =
//                kf.generatePublic(
//                        new X509EncodedKeySpec(backendPublicKeyBytes));
//
//        // 4. Client derives shared secret
//        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
//        ka.init(keyHolder.getPrivateKey());
//        ka.doPhase(backendPublicKey, true);
//
//        byte[] sharedSecret = ka.generateSecret();
//
//        System.out.println(
//                "Client shared secret hash: " +
//                        DigestUtils.sha256Hex(sharedSecret)
//        );
//
//        byte[] clientPub = keyHolder.getPublicKeyBytes();
//        byte[] backendPub = backendPublicKeyBytes;
//
//// Derive CEK
//        byte[] cek = HkdfUtil.deriveCek(
//                sharedSecret,
//                clientPub,
//                backendPub
//        );
//
//// Log ONLY hash
//        System.out.println(
//                "Client CEK hash: " + DigestUtils.sha256Hex(cek)
//        );
//
//        return ResponseEntity.ok(
//                "Session established. SessionId=" + response.getSessionId()
//        );
//    }



}
