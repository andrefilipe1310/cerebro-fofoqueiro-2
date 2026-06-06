package com.fofoqueiro.auth.security;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TotpService {

    private final String issuer;
    private final DefaultSecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;

    public TotpService(@Value("${totp.issuer:Fofoqueiro}") String issuer) {
        this.issuer = issuer;
        this.secretGenerator = new DefaultSecretGenerator();
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String buildQrCodeUri(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return "otpauth://totp/" + data.getLabel() + "?secret=" + secret
               + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30";
    }

    public boolean verify(String secret, String code) {
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            log.warn("TOTP verification error: {}", e.getMessage());
            return false;
        }
    }
}
