package club.abelix.network.online.data;

import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.Locale;

@UtilityClass
public final class Ids {

    public String physicalId(String group, int index) {
        return ("physical:" + group + ":" + index).toLowerCase(Locale.ROOT);
    }

    public String physicalVelocity(String podHash) {
        return ("physical:velocity:" + podHash).toLowerCase(Locale.ROOT);
    }

    public String arenaId(String group, String serverLetter, int arenaIndex) {
        return ("arena:" + group + ":" + serverLetter + ":" + arenaIndex).toLowerCase(Locale.ROOT);
    }

    public String parseId(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank");
        }

        val parts = name.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid name format: " + name);
        }

        val group = parts[0];
        val suffix = parts[1];

        if (suffix.matches("\\d+")) {
            val index = Integer.parseInt(suffix);
            return physicalId(group, index);
        }

        if (suffix.length() >= 2 && Character.isLetter(suffix.charAt(0))) {
            val letter = suffix.substring(0, 1);
            val indexPart = suffix.substring(1);

            if (!indexPart.matches("\\d+")) {
                throw new IllegalArgumentException("Invalid arena index in name: " + name);
            }

            val arenaIndex = Integer.parseInt(indexPart);
            return arenaId(group, letter, arenaIndex);
        }

        throw new IllegalArgumentException("Unrecognized server name pattern: " + name);
    }

    public String parsedArenaId(String arenaName) {
        val arenaNameParts = arenaName.split("-");
        if (arenaNameParts.length != 2) {
            throw new IllegalArgumentException("Invalid arena name: " + arenaName);
        }

        val group = arenaNameParts[0];
        val suffix = arenaNameParts[1];

        if (suffix.length() < 2 || !Character.isLetter(suffix.charAt(0))) {
            throw new IllegalArgumentException("Invalid arena suffix: " + arenaName);
        }

        val serverLetter = suffix.substring(0, 1);
        val indexPart = suffix.substring(1);

        val arenaIndex = Integer.parseInt(indexPart);

        return arenaId(group, serverLetter, arenaIndex);
    }

    public String parsedPhysicalId(String physicalName) {
        val nameParts = physicalName.split("-");
        if (nameParts.length != 2) {
            throw new IllegalArgumentException("Invalid physical server name: " + physicalName);
        }

        val group = nameParts[0];
        val index = Integer.parseInt(nameParts[1]);

        return physicalId(group, index);
    }

}