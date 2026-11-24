/* Decompiler 6ms, total 224ms, lines 55 */
package club.abelix.commons.util.password.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ShaHash {
    private final PasswordHashType secureType;
    private final SecureRandom secureRandom;
    private final MessageDigest messageDigest;

    public ShaHash(PasswordHashType var1) {
        try {
            this.secureType = var1;
            this.secureRandom = new SecureRandom();
            this.messageDigest = MessageDigest.getInstance(PasswordHashType.a(var1));
        } catch (NoSuchAlgorithmException except) {
            throw new RuntimeException(except);
        }
    }

    // $FF: synthetic method
    public static boolean checkHash(ShaHash var0, String var1, String var2) {
        return var0.checkHash(var1, var2);
    }

    private String checkHash() {
        byte[] var1 = new byte[16];
        this.secureRandom.nextBytes(var1);
        return String.format("%016x", new BigInteger(1, var1));
    }

    public String checkHash(String var1) {
        byte[] var2 = var1.getBytes();
        byte[] var3 = this.messageDigest.digest(var2);
        return String.format("%0" + PasswordHashType.b(this.secureType) + "x", new BigInteger(1, var3));
    }

    public String getHash(String var1) {
        String var2 = this.checkHash();
        String var3 = this.checkHash(var1);
        String var4 = this.checkHash(var3 + var2);
        return this.secureType + "$" + var2 + "$" + var4;
    }

    private boolean checkHash(String var1, String var2) {
        String[] var3 = var2.split("\\$");
        String var4 = var3[1];
        String var5 = var3[2];
        String var6 = this.checkHash(this.checkHash(var1) + var4);
        return var6.equals(var5);
    }
}
