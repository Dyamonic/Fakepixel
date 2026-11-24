package club.abelix.commons.util;

import club.abelix.commons.command.execution.ExecutionSession;
import lombok.experimental.UtilityClass;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-core</p>
 * <p>IpAddressutil creation on 26.04.2024 at 21:27</p>
 */

@UtilityClass
public class IpAddressUtil {

    public String hideLastPart(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        String[] parts = ip.split("\\.");

        if (parts.length != 4) {
            return ip;
        }

        return parts[0] + "." + parts[1] + "." + parts[2] + ".XXX";
    }

    public String hideLastPart(String ip, ExecutionSession session) {
        if (session.getSender().hasPermission("ip.reveal")) {
            return ip;
        }

        return hideLastPart(ip);
    }
}
