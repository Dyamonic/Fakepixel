package club.abelix.core.app.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public final class BuildInfo {

    private static final String FILE_NAME = "build-info.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = BuildInfo.class
                .getClassLoader()
                .getResourceAsStream(FILE_NAME)) {

            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            log.error("Failed get build info:", e);
        }
    }

    public static String getVersion() {
        return PROPS.getProperty("version", "UNKNOWN");
    }

    public static String getBuildTime() {
        return PROPS.getProperty("buildTime", "UNKNOWN");
    }

    public static String getAuthor() {
        return PROPS.getProperty("author", "UNKNOWN");
    }

    public static String getBranch() {
        return PROPS.getProperty("branch", "UNKNOWN");
    }

    public static String getUrl() {
        return PROPS.getProperty("url", "UNKNOWN");
    }
}

