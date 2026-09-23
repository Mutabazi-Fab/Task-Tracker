package com.throughline.taskmanagement.security;

import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.PersonRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Person.totpSecret goes through TotpSecretConverter (backed by TotpSecretCipher) — this
 * confirms that conversion actually happens against the real, Spring-managed
 * EntityManagerFactory, not just that the cipher class works in isolation. Specifically
 * guards against the converter silently no-op'ing if Spring Boot's JPA autoconfiguration
 * ever stopped wiring @Component-annotated AttributeConverters through Spring's bean
 * container — that would leave TotpSecretCipher's injected field null and NPE at runtime,
 * or worse, silently store the secret in plaintext.
 */
@SpringBootTest
@Transactional
class TotpSecretEncryptionTest {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void totpSecretIsEncryptedAtRestAndDecryptsBackCorrectlyOnReload() {
        Person person = new Person();
        person.setFullName("TOTP Encryption Test");
        person.setEmail("totp-encryption-test@example.com");
        person.setJobTitle("Tester");
        person.setTotpSecret("JBSWY3DPEHPK3PXP");
        Person saved = personRepository.saveAndFlush(person);

        String rawColumnValue = jdbcTemplate.queryForObject(
                "SELECT totp_secret FROM persons WHERE id = ?", String.class, saved.getId());
        assertNotNull(rawColumnValue);
        assertNotEquals("JBSWY3DPEHPK3PXP", rawColumnValue, "the raw DB column must not hold the plaintext secret");

        // Evicts the persistence context so the next findById is a genuine fresh read from
        // the database (and thus genuinely exercises convertToEntityAttribute), rather than
        // just handing back the same in-memory entity from Hibernate's first-level cache.
        entityManager.clear();
        Person reloaded = personRepository.findById(saved.getId()).orElseThrow();

        assertEquals("JBSWY3DPEHPK3PXP", reloaded.getTotpSecret());
    }
}
