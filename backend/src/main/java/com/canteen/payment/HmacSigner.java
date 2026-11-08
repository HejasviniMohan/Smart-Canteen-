package com.canteen.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class HmacSigner {

    private HmacSigner() {}

    public static byte[] sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    public static String signHex(String data, String secret) {
        return HexFormat.of().formatHex(sign(data, secret));
    }

    /** Constant-time, so response timing reveals nothing about a guessed signature. */
    public static boolean matchesHex(String data, String secret, String signature) {
        if (signature == null) return false;
        return MessageDigest.isEqual(
                signHex(data, secret).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
