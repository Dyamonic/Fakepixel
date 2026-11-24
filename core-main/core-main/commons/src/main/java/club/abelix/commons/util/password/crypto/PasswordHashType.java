/* Decompiler 4ms, total 221ms, lines 26 */
package club.abelix.commons.util.password.crypto;

public enum PasswordHashType {
    SHA256("SHA-256", 64);

    private final String name;
    private final int bits;

    PasswordHashType(String var3, int var4) {
        this.name = var3;
        this.bits = var4;
    }

    // $FF: synthetic method
    static String a(PasswordHashType var0) {
        return var0.name;
    }

    // $FF: synthetic method
    static int b(PasswordHashType var0) {
        return var0.bits;
    }
}
