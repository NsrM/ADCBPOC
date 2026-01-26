//package org.adcb.poc.jweecdhkms.controller;
//
////import jakarta.websocket.HandshakeResponse;
////import jakarta.websocket.server.HandshakeRequest;
//import org.adcb.poc.jweecdhkms.model.HandshakeRequest;
//import org.adcb.poc.jweecdhkms.model.HandshakeResponse;
//import org.adcb.poc.jweecdhkms.util.ClientKeyHolder;
////import org.apache.commons.codec.digest.DigestUtils;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//
//import javax.crypto.KeyAgreement;
//import java.security.KeyFactory;
//import java.security.PublicKey;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
//@RestController
//@RequestMapping("/client")
//public class ClientHandshakeController {
//
//    private final ClientKeyHolder keyHolder;
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public ClientHandshakeController(ClientKeyHolder keyHolder) {
//        this.keyHolder = keyHolder;
//    }
//
//    @PostMapping("/handshake")
//    public ResponseEntity<String> handshake() throws Exception {
//
//        // 1. Generate ephemeral client key pair
//        keyHolder.generate();
//
//        String clientPublicKeyBase64 =
//                Base64.getEncoder()
//                        .encodeToString(keyHolder.getPublicKeyBytes());
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
//        return ResponseEntity.ok(
//                "Session established. SessionId=" + response.getSessionId()
//        );
//    }
//}
