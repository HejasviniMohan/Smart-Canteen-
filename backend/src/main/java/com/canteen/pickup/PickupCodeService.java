package com.canteen.pickup;

import com.canteen.entity.Order;
import com.canteen.payment.HmacSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.OptionalLong;

/**
 * The QR payload: SC1.orderId.nonce.signature. Only this server holds the signing key, so a
 * code cannot be forged or changed to point at another order, unlike a plain "A101" token.
 */
@Service
public class PickupCodeService {

    private static final String VERSION = "SC1";
    /**
     * 128 bits of HMAC-SHA256, a standard truncation that still can't be forged. The full
     * 256 bits would add 21 characters and make a denser QR code that scans less reliably.
     */
    private static final int SIGNATURE_BYTES = 16;

    private final String secret;

    public PickupCodeService(@Value("${app.pickup.secret}") String secret) {
        this.secret = secret;
    }

    public String codeFor(Order order) {
        String body = VERSION + "." + order.getId() + "." + order.getPickupNonce();
        return body + "." + signature(body);
    }

    /** Returns the order id if the signature is genuine; empty for anything else. */
    public OptionalLong parse(String code) {
        if (code == null) return OptionalLong.empty();
        String[] parts = code.trim().split("\\.");
        if (parts.length != 4 || !VERSION.equals(parts[0])) return OptionalLong.empty();

        String body = parts[0] + "." + parts[1] + "." + parts[2];
        boolean genuine = MessageDigest.isEqual(
                signature(body).getBytes(StandardCharsets.UTF_8),
                parts[3].getBytes(StandardCharsets.UTF_8));
        if (!genuine) return OptionalLong.empty();
        try {
            return OptionalLong.of(Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    private String signature(String body) {
        byte[] mac = Arrays.copyOf(HmacSigner.sign(body, secret), SIGNATURE_BYTES);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac);
    }
}
