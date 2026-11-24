package club.abelix.commons.util;

import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.rank.permission.GroupPermission;

public class PermissionUtil {

    private static final int UNLIMITED_INT = 1000;
    private static final double UNLIMITED_DOUBLE = 100.0;

    public static boolean has(UserRanks ranks, String node) {
        return ranks.findPriorityRank().rank().getPermissions().asSet().stream()
                .anyMatch(p -> p.getName().equals(node) || p.getName().equals("*"));
    }

    public static int extractIntValue(UserRanks ranks, String baseKey, String typeKey) {
        return extractIntValue(ranks.findPriorityRank().rank(), baseKey, typeKey);
    }

    public static int extractIntValue(Rank rank, String baseKey, String typeKey) {
        int max = 0;
        String unlimitedKey = baseKey + "." + typeKey + ".unlimited";
        String prefix = baseKey + "." + typeKey + ".";
        for (GroupPermission gp : rank.getPermissions().asSet()) {
            String perm = gp.getName();

            if (perm.equals("*") || perm.equals(unlimitedKey)) {
                return UNLIMITED_INT;
            }

            if (perm.startsWith(prefix)) {
                String num = perm.substring(prefix.length());
                try {
                    max = Math.max(max, Integer.parseInt(num));
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("Invalid permission format for int extraction: " + perm);
                }
            }
        }
        return max;
    }

    public static double extractDoubleValue(UserRanks ranks, String baseKey, String typeKey) {
        return extractDoubleValue(ranks.findPriorityRank().rank(), baseKey, typeKey);
    }

    public static double extractDoubleValue(Rank rank, String baseKey, String typeKey) {
        double max = 0.0;
        String unlimitedKey = baseKey + "." + typeKey + ".unlimited";
        String prefix = baseKey + "." + typeKey + ".";
        for (GroupPermission gp : rank.getPermissions().asSet()) {
            String perm = gp.getName();

            if (perm.equals("*") || perm.equals(unlimitedKey)) {
                return UNLIMITED_DOUBLE;
            }

            if (perm.startsWith(prefix)) {
                String num = perm.substring(prefix.length());
                try {
                    max = Math.max(max, Double.parseDouble(num));
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("Invalid permission format for double extraction: " + perm);
                }
            }
        }
        return max;
    }

    public static int extractLimit(UserRanks ranks, String baseKey) {
        return extractIntValue(ranks, baseKey, "limit");
    }

    public static int extractLimit(Rank rank, String baseKey) {
        return extractIntValue(rank, baseKey, "limit");
    }

    public static double extractDoubleLimit(UserRanks ranks, String baseKey) {
        return extractDoubleValue(ranks, baseKey, "limit");
    }

    public static double extractDoubleLimit(Rank rank, String baseKey) {
        return extractDoubleValue(rank, baseKey, "limit");
    }
}
