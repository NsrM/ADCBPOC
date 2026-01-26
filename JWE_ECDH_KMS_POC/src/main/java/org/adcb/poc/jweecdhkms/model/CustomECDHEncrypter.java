package org.adcb.poc.jweecdhkms.model;



import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.impl.AAD;
import com.nimbusds.jose.crypto.impl.ECDH;
import com.nimbusds.jose.jwk.ECKey;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;

public class CustomECDHEncrypter extends ECDHEncrypter {

    private final KeyPair manualKeyPair;

    /**
     * Custom constructor to supply your own ephemeral KeyPair.
     * @param recipientPublicKey The public key of the receiver.
     * @param manualKeyPair Your pre-generated ephemeral KeyPair.
     */
    public CustomECDHEncrypter(ECPublicKey recipientPublicKey, KeyPair manualKeyPair) throws JOSEException {
        super(recipientPublicKey);
        this.manualKeyPair = manualKeyPair;
    }

    @Override
    public JWECryptoParts encrypt(JWEHeader header, byte[] clearText, byte[] aad) throws JOSEException {

        // 1. Use the manual KeyPair instead of generating a new one
        ECPublicKey ephemeralPublicKey = (ECPublicKey) manualKeyPair.getPublic();
        ECPrivateKey ephemeralPrivateKey = (ECPrivateKey) manualKeyPair.getPrivate();

        // 2. Build the updated header with your manual EPK (Satisfies JWE requirements)
        JWEHeader updatedHeader = new JWEHeader.Builder(header)
                .ephemeralPublicKey(new ECKey.Builder(this.getCurve(), ephemeralPublicKey).build())
                .build();

        // 3. Derive the shared secret 'Z' using the recipient's public key and YOUR private key
        SecretKey Z = ECDH.deriveSharedSecret(
                this.getPublicKey(),
                ephemeralPrivateKey,
                this.getJCAContext().getKeyEncryptionProvider()
        );

        // 4. Handle AAD (Additional Authenticated Data) logic from the original source
        byte[] updatedAAD = Arrays.equals(AAD.compute(header), aad) ? AAD.compute(updatedHeader) : aad;

        // 5. Call the protected method from ECDHCryptoProvider to perform actual encryption
        return this.encryptWithZ(updatedHeader, Z, clearText, updatedAAD);
    }
}
