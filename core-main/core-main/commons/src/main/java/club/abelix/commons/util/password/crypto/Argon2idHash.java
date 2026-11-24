package club.abelix.commons.util.password.crypto;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


@UtilityClass
public final class Argon2idHash {

    private static final String ALG = "ARGON2ID";

    // Параметры Argon2id (подбери под своё железо)
    private static final int SALT_LENGTH = 16;      // 128 бит
    private static final int HASH_LENGTH = 32;      // 256 бит
    private static final int ITERATIONS = 4;
    private static final int MEMORY_KB = 64 * 1024; // 64 MB
    private static final int PARALLELISM = 1;

    private static final SecureRandom RANDOM = new SecureRandom();


    /**
     * Генерация нового хеша пароля.
     * Возвращает строку формата: ALG$SALT$PASSWORD
     */
    public String generatePassword(@NonNull String rawPassword) {

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        byte[] hash = hash(rawPassword.toCharArray(), salt);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        return ALG + "$" + saltB64 + "$" + hashB64;
    }

    /**
     * Проверка пароля против хеша в формате ALG$SALT$PASSWORD.
     */
    public boolean checkPassword(@NonNull String rawPassword, @NonNull String hashPassword) {
        if (!checkHash(hashPassword)) { // формат не наш / не Argon2id
            throw new IllegalArgumentException("Invalid hash provided");
        }

        String[] parts = hashPassword.split("\\$");
        // parts[0] = ALG
        String saltB64 = parts[1];
        String hashB64 = parts[2];

        byte[] salt;
        byte[] expectedHash;
        try {
            salt = Base64.getDecoder().decode(saltB64);
            expectedHash = Base64.getDecoder().decode(hashB64);
        } catch (IllegalArgumentException e) {
            // битая base64
            return false;
        }

        byte[] actualHash = hash(rawPassword.toCharArray(), salt);

        // сравнение в "constant time"
        if (actualHash.length != expectedHash.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < actualHash.length; i++) {
            diff |= actualHash[i] ^ expectedHash[i];
        }
        return diff == 0;
    }

    public boolean checkHash(@NonNull String hashPassword) {
        String[] parts = hashPassword.split("\\$");
        if (parts.length != 3) {
            return false;
        }
        return ALG.equalsIgnoreCase(parts[0]);
    }

    private byte[] hash(char[] password, byte[] salt) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withParallelism(PARALLELISM)
                .withMemoryAsKB(MEMORY_KB)
                .withIterations(ITERATIONS);

        Argon2Parameters params = builder.build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] hash = new byte[HASH_LENGTH];
        generator.generateBytes(
                new String(password).getBytes(StandardCharsets.UTF_8),
                hash
        );

        return hash;
    }
}
