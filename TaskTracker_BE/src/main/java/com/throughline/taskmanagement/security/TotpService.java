package com.throughline.taskmanagement.security;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

import java.util.Base64;

/** RFC 6238 TOTP, wrapping dev.samstevens.totp — every operation here is pure local computation (HMAC
 *  over the system clock), no network call, so this works identically with or without internet access. */
@Component
public class TotpService {

    private static final String ISSUER = "Throughline";
    // Accepts the previous/current/next 30s code (±30s) to absorb ordinary clock drift
    // between the phone and the server without materially widening the guessable window.
    private static final int ALLOWED_TIME_PERIOD_DISCREPANCY = 1;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier;

    public TotpService() {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        verifier.setAllowedTimePeriodDiscrepancy(ALLOWED_TIME_PERIOD_DISCREPANCY);
        this.codeVerifier = verifier;
    }

    /** A fresh Base32 secret — generate once per person and keep reusing it across repeated
     *  enrollment attempts until confirmed (see Person.totpEnabledAt), rather than
     *  regenerating a new one on every login attempt during setup. */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /** A "data:image/png;base64,..." URI the frontend can drop straight into an <img> src —
     *  no server-side file, no separate download endpoint needed. */
    public String buildQrCodeDataUri(String email, String secret) {
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] png = qrGenerator.generate(qrData);
            return "data:" + qrGenerator.getImageMimeType() + ";base64," + Base64.getEncoder().encodeToString(png);
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Could not generate the TOTP QR code.", e);
        }
    }

    public boolean isCodeValid(String secret, String code) {
        return code != null && code.matches("\\d{6}") && codeVerifier.isValidCode(secret, code);
    }
}
