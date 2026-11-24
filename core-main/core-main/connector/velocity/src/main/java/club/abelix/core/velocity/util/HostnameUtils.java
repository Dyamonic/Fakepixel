package club.abelix.core.velocity.util;

import lombok.experimental.UtilityClass;

import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>HostnameUtils creation on 02.03.2023 at 21:02</p>
 */

@UtilityClass
public class HostnameUtils {

    public String cleanVhost(InetSocketAddress address) {
        if (address == null) {
            return "unknown";
        }
        String hostname = address.getHostString();

        String cleaned = hostname;
        int zeroIdx = hostname.indexOf(0);
        if (zeroIdx > -1) {
            cleaned = hostname.substring(0, zeroIdx);
        }

        if (!cleaned.isEmpty() && cleaned.charAt(cleaned.length() - 1) == '.') {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned.toLowerCase(Locale.ROOT);
    }
}
