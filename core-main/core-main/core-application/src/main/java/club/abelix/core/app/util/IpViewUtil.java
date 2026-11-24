package club.abelix.core.app.util;

import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.IpAddressUtil;
import club.abelix.core.app.CoreApplication;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-core</p>
 * <p>IpViewUtil creation on 26.04.2024 at 21:35</p>
 */

@UtilityClass
public class IpViewUtil {

    public String geo(InetAddress ip) {
        val application = CoreApplication.getInstance();
        val geoManager = application.getGeoLocationManager();

        if (geoManager == null) {
            return "";
        }

        val geoLocation = geoManager.lookup(ip);

        val country = geoLocation.getCountry();
        val city = geoLocation.getCity();


        return Language.defaultLanguage().getMessage("geo_ip_format",
                "%country%", country == null ? "N/A" : country.getName(),
                "%city%", city);
    }

    public String of(InetAddress ip, ExecutionSession execution) {
        val hideIp = IpAddressUtil.hideLastPart(ip.getHostAddress(), execution);

        return execution.getLanguage().getMessage("ip_format", "%ip%", hideIp,
                "%geo%", geo(ip));
    }

    public String of(InetAddress ip) {
        val hideIp = IpAddressUtil.hideLastPart(ip.getHostAddress());

        return Language.defaultLanguage().getMessage("ip_format", "%ip%", hideIp,
                "%geo%", geo(ip));
    }

    public String of(String ip, ExecutionSession execution) {
        try {
            val addr = InetAddress.getByName(ip);

            return of(addr, execution);
        } catch (UnknownHostException e) {
            return IpAddressUtil.hideLastPart(ip, execution);
        }
    }

    public String of(String ip) {
        try {
            val addr = InetAddress.getByName(ip);

            return of(addr);
        } catch (UnknownHostException e) {
            return IpAddressUtil.hideLastPart(ip);
        }
    }
}
