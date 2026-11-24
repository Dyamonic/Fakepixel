package club.abelix.commons.util.password;

import club.abelix.commons.util.password.crypto.Argon2idHash;
import club.abelix.commons.util.password.crypto.PasswordHashType;
import club.abelix.commons.util.password.crypto.ShaHash;

import java.util.concurrent.ThreadLocalRandom;

public class PasswordHashing {
    private static final ShaHash SHA_HASH = new ShaHash(PasswordHashType.SHA256);
    private static final char[] PASSWORD_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();

    public static String getHash(String text) {
        return Argon2idHash.generatePassword(text);
    }

    public static boolean checkPassword(String inputPassword, String hashPassword) {
        if (hashPassword.startsWith("SHA256")) {
            return ShaHash.checkHash(SHA_HASH, inputPassword, hashPassword);   
        } else if (hashPassword.startsWith("ARGON2ID")) {
            return Argon2idHash.checkPassword(inputPassword, hashPassword);
        } else {
            throw new IllegalArgumentException("Invalid hash provided");
        }
    }

    public static String generateRandomPassword(int length) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        StringBuilder password = new StringBuilder();

        while (password.length() < length) {
            password.append(PASSWORD_CHARACTERS[random.nextInt(PASSWORD_CHARACTERS.length)]);
        }

        return password.toString();
    }

}
