package org.adcb.poc.jweecdhkms.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.nio.charset.StandardCharsets;

public final class HkdfUtil {

    private HkdfUtil() {}

    public static byte[] deriveCek(
            byte[] sharedSecret,
            byte[] clientPublicKey,
            byte[] backendPublicKey
    ) {

        // salt = SHA-256(clientPub || backendPub)
        byte[] salt = DigestUtils.sha256(
                concat(clientPublicKey, backendPublicKey)
        );

        HKDFBytesGenerator hkdf =
                new HKDFBytesGenerator(new SHA256Digest());

        hkdf.init(new HKDFParameters(
                sharedSecret,
                salt,
                "session-cek-v1".getBytes(StandardCharsets.UTF_8)
        ));

        byte[] cek = new byte[32]; // 256-bit AES
        hkdf.generateBytes(cek, 0, cek.length);

        return cek;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

}
