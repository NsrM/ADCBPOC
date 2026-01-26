package org.adcb.poc.jweecdhkms.util;

import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;

@Component
public class ClientKeyHolder {

    private KeyPair keyPair;

    public void generate() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp521r1"));
        this.keyPair = kpg.generateKeyPair();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public byte[] getPublicKeyBytes() {
        return keyPair.getPublic().getEncoded();
    }
}
