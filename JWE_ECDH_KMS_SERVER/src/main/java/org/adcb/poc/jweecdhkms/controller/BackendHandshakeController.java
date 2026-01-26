package org.adcb.poc.jweecdhkms.controller;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.impl.AESGCM;
import com.nimbusds.jose.crypto.impl.ECDH;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jca.JWEJCAContext;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import org.adcb.poc.jweecdhkms.model.HandshakeRequest;
import org.adcb.poc.jweecdhkms.model.HandshakeResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import com.nimbusds.jose.crypto.impl.ConcatKDF;
import software.amazon.awssdk.services.kms.model.DeriveSharedSecretResponse;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyAgreementAlgorithmSpec;


import javax.crypto.SecretKey;
import com.nimbusds.jose.crypto.impl.AESKW;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;


import static org.adcb.poc.jweecdhkms.util.HkdfUtil.deriveCek;

@RestController
@RequestMapping("/backend")
public class BackendHandshakeController {

    private static final String KMS_KEY_ID = "arn:aws:kms:eu-north-1:220236885184:key/cc83c38c-6fc7-4491-b340-768f2ec52526";

    private final KmsClient kmsClient;

    public BackendHandshakeController(KmsClient kmsClient) {
        this.kmsClient = kmsClient;
    }

    @PostMapping("/handshake")
    public HandshakeResponse handshake(@RequestBody HandshakeRequest request) {

        // 1. Decode client public key
        byte[] clientPublicKey =
                Base64.getDecoder().decode(request.getClientPublicKey());

        // 2. Derive shared secret using KMS private key
        DeriveSharedSecretResponse secretResponse =
                kmsClient.deriveSharedSecret(r -> r
                        .keyId(KMS_KEY_ID)
                        .publicKey(SdkBytes.fromByteArray(clientPublicKey))
                        .keyAgreementAlgorithm(
                                KeyAgreementAlgorithmSpec.ECDH));


        byte[] sharedSecret =
                secretResponse.sharedSecret().asByteArray();

        // ⚠️ TEMP: just log hash to prove derivation
//        System.out.println(
//                "Backend shared secret hash: " +
//                        DigestUtils.sha256Hex(sharedSecret)
//        );

        // 3. Fetch backend public key
        GetPublicKeyResponse pubKeyResponse =
                kmsClient.getPublicKey(r -> r.keyId(KMS_KEY_ID));

        String backendPublicKey = Base64.getEncoder()
                .encodeToString(pubKeyResponse.publicKey().asByteArray());

        // After sharedSecret is obtained

// Decode client public key again if needed
        byte[] clientPub = clientPublicKey;

        byte[] backendPub =
                pubKeyResponse.publicKey().asByteArray();

// Derive CEK
        byte[] cek = deriveCek(
                sharedSecret,
                clientPub,
                backendPub
        );

        System.out.println(
                "Backend shared secret hash: " +
                        DigestUtils.sha256Hex(sharedSecret)
        );

// Log ONLY hash for verification
        System.out.println(
                "Backend CEK hash: " + DigestUtils.sha256Hex(cek)
        );

// (Later: encrypt & store cek using symmetric KMS key)


        // 4. Generate session id
        String sessionId = UUID.randomUUID().toString();



        // (Later we will store sharedSecret mapped to sessionId)

        return new HandshakeResponse(sessionId, backendPublicKey);
    }

    @PostMapping("/handshake/v2")
    public ResponseEntity<?> decryptJwe(@RequestBody Map<String, Object> body) throws Exception {

        // 1. Parse JWE
        String jweString = (String) body.get("encryptedJwe");
        JWEObject jweObject = JWEObject.parse(jweString);
        JWEHeader header = jweObject.getHeader();

        // 2. Extract ephemeral public key (epk)
        JWK jwk = header.getEphemeralPublicKey();
        ECKey ecEphemeralKey = (ECKey) jwk;
        byte[] clientPublicKeyDer = ecEphemeralKey.toECPublicKey().getEncoded();

        // 3. Derive shared secret via AWS KMS
        DeriveSharedSecretResponse secretResponse = kmsClient.deriveSharedSecret(r -> r
                .keyId(KMS_KEY_ID)
                .publicKey(SdkBytes.fromByteArray(clientPublicKeyDer))
                .keyAgreementAlgorithm(KeyAgreementAlgorithmSpec.ECDH)
        );
        byte[] sharedSecretZ = secretResponse.sharedSecret().asByteArray();

        // Print Shared Secret Hash
        System.out.println("Backend Shared secret hash: " + DigestUtils.sha256Hex(sharedSecretZ));

        // 4. JWE DECRYPTION: Derive KWK and Unwrap JWE-CEK
        ConcatKDF kdf = new ConcatKDF("SHA-256");
        SecretKey sharedSecretKey = new SecretKeySpec(sharedSecretZ, "AES");

        // NIST OtherInfo for JWE-KWK
        byte[] algID = ConcatKDF.encodeStringData(header.getAlgorithm().getName());
        byte[] partyUInfo = ConcatKDF.encodeDataWithLength(header.getAgreementPartyUInfo());
        byte[] partyVInfo = ConcatKDF.encodeDataWithLength(header.getAgreementPartyVInfo());
        byte[] suppPubInfo = ConcatKDF.encodeIntData(256);

        SecretKey kwk = kdf.deriveKey(sharedSecretKey, 256, algID, partyUInfo, partyVInfo, suppPubInfo, ConcatKDF.encodeNoData());

        // Unwrap the CEK used for this JWE payload
        byte[] encryptedKey = jweObject.getEncryptedKey().decode();
        // AESKW.decrypt(SecretKey kwk, byte[] encryptedCEK, Provider provider)
        SecretKey jweCek = AESKW.unwrapCEK(kwk, encryptedKey, null);

// Reconstruct the CEK as a SecretKey for the decrypter
        //SecretKey jweCek = new SecretKeySpec(cekBytes, "AES");

        // Decrypt the JWE payload locally
        //jweObject.decrypt(new DirectDecrypter(jweCek));
        // 6. Decrypt JWE Payload
// Use AESDecrypter instead of DirectDecrypter
// AESDecrypter accepts the KWK (Key Wrapping Key) and handles the unwrap/decrypt internally
// OR, keep your manual unwrap and use a decrypter that doesn't check the 'alg' header:
//        jweObject.decrypt(new JWEDecrypter() {
//            @Override
//            public byte[] decrypt(JWEHeader jweHeader, Base64URL iv, Base64URL cipherText, Base64URL base64URL2, Base64URL base64URL3, byte[] bytes) throws JOSEException {
//                //return new byte[0];
//                return AESGCM.decrypt(jweCek, iv.decode(), cipherText.decode(), header.toBase64URL().toString().getBytes(), base64URL3.decode(), null);
//            }
//
//            @Override public Set<JWEAlgorithm> supportedJWEAlgorithms() { return null; }
//            @Override public Set<EncryptionMethod> supportedEncryptionMethods() { return null; }
//            @Override public JWEJCAContext getJCAContext() { return (JWEJCAContext) new JCAContext(); }
//        });

        jweObject.decrypt(new JWEDecrypter() {
            @Override
            public byte[] decrypt(JWEHeader header,
                                  Base64URL encryptedKey,
                                  Base64URL iv,
                                  Base64URL cipherText,
                                  Base64URL authTag, byte[] aadInput) throws JOSEException {

                // JWE AAD is the ASCII bytes of the Base64URL encoded header
                byte[] aad = header.toBase64URL().toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);

                return AESGCM.decrypt(
                        jweCek,
                        iv.decode(),
                        cipherText.decode(),
                        aad,
                        authTag.decode(),
                        null
                );
            }

            @Override public Set<JWEAlgorithm> supportedJWEAlgorithms() {
                return Collections.singleton(JWEAlgorithm.ECDH_ES_A256KW);
            }
            @Override public Set<EncryptionMethod> supportedEncryptionMethods() {
                return Collections.singleton(EncryptionMethod.A256GCM);
            }
            @Override public JWEJCAContext getJCAContext() {
                return new JWEJCAContext();
            }
        });

        // 5. APPLICATION LOGIC: Generate your SEPARATE CEK
        // Fetch your backend public key from KMS (if needed for your custom deriveCek)
        GetPublicKeyResponse pubKeyResponse = kmsClient.getPublicKey(r -> r.keyId(KMS_KEY_ID));
        byte[] backendPublicKeyDer = pubKeyResponse.publicKey().asByteArray();

//        SecretKey Zf = new SecretKeySpec(sharedSecretZ, "AES");
//        SecretKey kek = ECDH.deriveSharedKey(jweObject.getHeader(), Zf, new ConcatKDF("SHA-256"));
//
//        SecretKey cek = AESKW.unwrapCEK(kek,jweObject.getEncryptedKey().decode(),null);
//        System.out.println("Nimbus CEK derive: "+DigestUtils.sha256Hex(cek.getEncoded()));

        // Call your existing custom method
        byte[] separateAppCek = deriveCek(
                sharedSecretZ,
                clientPublicKeyDer,
                backendPublicKeyDer
        );

        System.out.println("Backend CEK hash: " + DigestUtils.sha256Hex(separateAppCek));

        // 6. Return decrypted payload
        return ResponseEntity.ok(jweObject.getPayload().toJSONObject());
    }


//    @PostMapping("/handshake/v2")
//    public ResponseEntity<?> decryptJwe(@RequestBody Map<String, Object> jweString) throws Exception {
//
//        // 1. Parse JWE
//        JWEObject jweObject = JWEObject.parse((String) jweString.get("encryptedJwe"));
//        JWEHeader header = jweObject.getHeader();
//
//        // 2. Extract ephemeral public key (epk)
//        JWK jwk = header.getEphemeralPublicKey();
//
//        if (!(jwk instanceof ECKey)) {
//            throw new IllegalArgumentException("epk is not EC key");
//        }
//
//        ECKey ecEphemeralKey = (ECKey) jwk;
//
//        ECPublicKey clientECPublicKey =
//                (ECPublicKey) ecEphemeralKey.toPublicKey();
//
//        byte[] clientPublicKeyDer =
//                clientECPublicKey.getEncoded();
//
//        // 3. Derive shared secret via AWS KMS
//        DeriveSharedSecretResponse secretResponse =
//                kmsClient.deriveSharedSecret(r -> r
//                        .keyId(KMS_KEY_ID)
//                        .publicKey(SdkBytes.fromByteArray(clientPublicKeyDer))
//                        .keyAgreementAlgorithm(
//                                KeyAgreementAlgorithmSpec.ECDH
//                        )
//                );
//
//        byte[] sharedSecret =
//                secretResponse.sharedSecret().asByteArray();
//
//        // 4. KDF: Derive Key Wrapping Key (KWK)
//        // For ECDH-ES+A256KW, the KWK must be 256 bits (32 bytes)
//        byte[] kwkBytes = ConcatKDF.encode(
//                sharedSecret,
//                256, // Key length in bits
//                header.getAlgorithm().getName().getBytes(), // Algorithm ID
//                null, null, null, null // PartyU, PartyV, SuppPub, SuppPriv
//        );
//        SecretKey kwk = new SecretKeySpec(kwkBytes, "AES");
//
//        // 5. Unwrap the Content Encryption Key (CEK)
//        byte[] encryptedKey = jweObject.getEncryptedKey().decode();
//        SecretKey cek = AESKW.unwrap(kwk, encryptedKey, null);
//
//        // 6. Decrypt the Payload using CEK (AES-GCM)
//        byte[] iv = jweObject.getIV().decode();
//        byte[] cipherText = jweObject.getCipherText().decode();
//        byte[] authTag = jweObject.getAuthTag().decode();
//
//
//
//        // AESGCM requires combining ciphertext and tag in some implementations
//        byte[] decryptedBytes = AESGCM.decrypt(cek, iv, cipherText, header.toBase64URL().toString().getBytes(), authTag, null);
//
//        System.out.println(
//                "Shared secret hash: " +
//                        DigestUtils.sha256Hex(sharedSecret)
//        );
//
//        // 4. Fetch backend public key from KMS
//        GetPublicKeyResponse pubKeyResponse =
//                kmsClient.getPublicKey(r -> r.keyId(KMS_KEY_ID));
//
//        byte[] backendPublicKeyDer =
//                pubKeyResponse.publicKey().asByteArray();
//
//        // 5. Derive CEK using YOUR EXISTING METHOD
//        byte[] cek = deriveCek(
//                sharedSecret,
//                clientPublicKeyDer,
//                backendPublicKeyDer
//        );
//
//        System.out.println(
//                "Derived CEK hash: " +
//                        DigestUtils.sha256Hex(cek)
//        );
//
//        // 6. Decrypt JWE using derived CEK
//        DirectDecrypter decrypter =
//                new DirectDecrypter(cek);
//
//        jweObject.decrypt(decrypter);
//
//        return ResponseEntity.ok(jweObject.getPayload());
//    }


//    @PostMapping("/handshake/v2")
//    public HandshakeResponse handshake(@RequestBody Map<String, String> body)
//            throws Exception {
//
//        String jweString = body.get("encryptedHandshake");
//
//        System.out.println("\n==== RECEIVED JWE ====");
//        System.out.println(jweString);
//
//        // 1️⃣ Parse JWE
//        JWEObject jwe = JWEObject.parse(jweString);
//        JWEHeader header = jwe.getHeader();
//
//        // 2️⃣ Extract client ephemeral public key (epk)
//        JWK epk = header.getEphemeralPublicKey();
//
//        if (!(epk instanceof ECKey)) {
//            throw new IllegalStateException("EPK is not an EC key");
//        }
//
//        ECKey ecEphemeralKey = (ECKey) epk;
//
//        if (ecEphemeralKey == null) {
//            throw new IllegalStateException("Missing epk in JWE header");
//        }
//
//        byte[] clientPublicKeyDer = ecEphemeralKey.toECPublicKey().getEncoded();
//
//        System.out.println("\n==== CLIENT EPK (DER) ====");
//        System.out.println(Base64.getEncoder().encodeToString(clientPublicKeyDer));
//
//        // 3️⃣ Derive shared secret via AWS KMS
//        DeriveSharedSecretResponse secretResponse =
//                kmsClient.deriveSharedSecret(r -> r
//                        .keyId(KMS_KEY_ID)
//                        .publicKey(SdkBytes.fromByteArray(clientPublicKeyDer))
//                        .keyAgreementAlgorithm(KeyAgreementAlgorithmSpec.ECDH)
//                );
//
//        byte[] sharedSecret =
//                secretResponse.sharedSecret().asByteArray();
//
//        // 🔐 Log hash ONLY
//        System.out.println(
//                "Backend shared secret hash: " +
//                        DigestUtils.sha256Hex(sharedSecret)
//        );
//
//        // 4️⃣ Fetch backend public key (optional, for CEK derivation context)
//        GetPublicKeyResponse pubKeyResponse =
//                kmsClient.getPublicKey(r -> r.keyId(KMS_KEY_ID));
//
//        byte[] backendPublicKeyDer =
//                pubKeyResponse.publicKey().asByteArray();
//
//        // 5️⃣ Derive CEK using Concat KDF (same as Nimbus / RFC 7518)
//        byte[] cek = deriveCek(
//                sharedSecret,
//                header,
//                32 // 256-bit CEK
//        );
//
//        // 🔐 Log hash ONLY
//        System.out.println(
//                "Backend CEK hash: " +
//                        DigestUtils.sha256Hex(cek)
//        );
//
//        // 6️⃣ Generate session ID
//        String sessionId = UUID.randomUUID().toString();
//
//        // (Later) store CEK securely mapped to sessionId
//
//        return new HandshakeResponse(
//                sessionId,
//                Base64.getEncoder().encodeToString(backendPublicKeyDer)
//        );
//    }

//    @PostMapping("/handshake/v2")
//    public ResponseEntity<?> init(@RequestBody Map<String, String> body)
//            throws Exception {
//
//        String jweString = body.get("encryptedHandshake");
//
//        System.out.println("\n==== RECEIVED JWE ====");
//        System.out.println(jweString);
//
//        // 1. Parse JWE
//        JWEObject jwe = JWEObject.parse(jweString);
//
//        // 2. Extract EPK from header
//        ECKey epkJwk = jwe.getHeader().getEphemeralPublicKey();
//        ECPublicKey clientEphemeralPublicKey = epkJwk.toECPublicKey();
//
//        System.out.println("\n==== CLIENT EPHEMERAL PUBLIC KEY ====");
//        System.out.println(Base64.getEncoder()
//                .encodeToString(clientEphemeralPublicKey.getEncoded()));
//
//        // 3. Load server private key (local for now, KMS later)
//        ECPrivateKey serverPrivateKey =
//                ServerPrivateKeyLoader.loadPrivateKey();
//
//        // 4. Derive shared secret Z
//        byte[] sharedSecretZ =
//                deriveSharedSecret(clientEphemeralPublicKey, serverPrivateKey);
//
//        System.out.println("\n==== SHARED SECRET Z ====");
//        System.out.println(Base64.getEncoder().encodeToString(sharedSecretZ));
//
//        // 5. Derive CEK from Z using Concat KDF
//        byte[] cek = deriveCek(
//                sharedSecretZ,
//                jwe.getHeader(),
//                32 // 256-bit CEK
//        );
//
//        System.out.println("\n==== DERIVED CEK ====");
//        System.out.println(Base64.getEncoder().encodeToString(cek));
//
//        return ResponseEntity.ok(
//                Map.of(
//                        "status", "HANDSHAKE_ESTABLISHED",
//                        "version", "v2"
//                )
//        );
//    }
}
