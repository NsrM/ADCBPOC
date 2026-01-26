package org.adcb.poc.jweecdhkms.model;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWECryptoParts;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.impl.AAD;
import com.nimbusds.jose.crypto.impl.ECDH;
import com.nimbusds.jose.jwk.ECKey;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class FixedKeyECDHEncrypter extends ECDHEncrypter {

    private final KeyPair fixedEphemeralKeyPair;

    public FixedKeyECDHEncrypter(ECPublicKey recipientPublicKey,
                                 KeyPair fixedEphemeralKeyPair)
            throws JOSEException {

        super(recipientPublicKey);
        this.fixedEphemeralKeyPair = fixedEphemeralKeyPair;
    }

    @Override
    public JWECryptoParts encrypt(JWEHeader header,
                                  byte[] clearText,
                                  byte[] aad)
            throws JOSEException {

        // 1. Use PROVIDED keypair
        ECPublicKey epk = (ECPublicKey) fixedEphemeralKeyPair.getPublic();
        ECPrivateKey esk = (ECPrivateKey) fixedEphemeralKeyPair.getPrivate();

        // 2. Override header with correct EPK
        JWEHeader updatedHeader = new JWEHeader.Builder(header)
                .ephemeralPublicKey(
                        new ECKey.Builder(getCurve(), epk).build()
                )
                .build();

        // 3. Derive shared secret Z
        SecretKey Z = ECDH.deriveSharedSecret(
                getPublicKey(),   // server public key
                esk,              // client private key
                getJCAContext().getKeyEncryptionProvider()
        );

        // 4. Handle AAD properly
        byte[] updatedAAD = AAD.compute(updatedHeader);

        // 5. Delegate to Nimbus internal encryption
        return encryptWithZ(updatedHeader, Z, clearText, updatedAAD);
    }

    // generate key pair
    // get the keypair generated
}

