package org.adcb.poc.jweecdhkms.util;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;

import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;

@Component
public class ServerKeyLoader {

    private final KmsClient kmsClient;
    private final String keyId = "arn:aws:kms:eu-north-1:220236885184:key/cc83c38c-6fc7-4491-b340-768f2ec52526";


    public ServerKeyLoader(KmsClient kmsClient) {
        this.kmsClient = kmsClient;
    }

    public ECPublicKey getServerPublicKey() {

        GetPublicKeyRequest request = GetPublicKeyRequest.builder()
                .keyId(keyId)
                .build();

        GetPublicKeyResponse response = kmsClient.getPublicKey(request);

        byte[] publicKeyDer = response.publicKey().asByteArray();

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyDer);

            return (ECPublicKey) keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse EC public key from KMS", e);
        }
    }
}
