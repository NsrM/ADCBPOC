package org.adcb.poc.jweecdhkms.controller;

import org.adcb.poc.jweecdhkms.model.HandshakeRequest;
import org.adcb.poc.jweecdhkms.model.HandshakeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DeriveSharedSecretResponse;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyAgreementAlgorithmSpec;

import java.util.Base64;
import java.util.UUID;

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

        // 4. Generate session id
        String sessionId = UUID.randomUUID().toString();

        // (Later we will store sharedSecret mapped to sessionId)

        return new HandshakeResponse(sessionId, backendPublicKey);
    }
}
