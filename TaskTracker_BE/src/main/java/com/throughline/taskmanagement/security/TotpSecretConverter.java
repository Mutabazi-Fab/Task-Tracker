package com.throughline.taskmanagement.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Transparent encrypt-on-write / decrypt-on-read for Person.totpSecret — Spring Boot's
 *  auto-configured EntityManagerFactory uses Spring's bean container for JPA converters, so
 *  this being a normal @Component (with TotpSecretCipher injected) works without any extra
 *  wiring beyond @Convert(converter = TotpSecretConverter.class) on the field. */
@Converter
@Component
@RequiredArgsConstructor
public class TotpSecretConverter implements AttributeConverter<String, String> {

    private final TotpSecretCipher cipher;

    @Override
    public String convertToDatabaseColumn(String plainSecret) {
        return plainSecret == null ? null : cipher.encrypt(plainSecret);
    }

    @Override
    public String convertToEntityAttribute(String encryptedSecret) {
        return encryptedSecret == null ? null : cipher.decrypt(encryptedSecret);
    }
}
