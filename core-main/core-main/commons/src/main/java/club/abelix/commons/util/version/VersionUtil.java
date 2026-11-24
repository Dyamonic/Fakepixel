package club.abelix.commons.util.version;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: core</p>
 * <p>VersionUtil creation on 06.06.2023 at 02:38</p>
 */

public class VersionUtil {
    @SuppressWarnings("resource")
    public static Version mavenVersionFor(ClassLoader cl, String groupId, String artifactId) {
        InputStream pomProperties = cl.getResourceAsStream("META-INF/maven/"
                + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/pom.properties");

        if (pomProperties != null) {
            try {
                Properties props = new Properties();
                props.load(pomProperties);
                String versionStr = props.getProperty("version");
                String pomPropertiesArtifactId = props.getProperty("artifactId");
                String pomPropertiesGroupId = props.getProperty("groupId");
                return new Version(pomPropertiesArtifactId, pomPropertiesGroupId, versionStr);
            } catch (IOException e) {
                // Ignore
            } finally {
                _close(pomProperties);
            }
        }
        return new Version("unknown", "unknown", "unknown");
    }

    @SuppressWarnings("resource")
    public static Git gitVersion(ClassLoader cl) {
        InputStream gitProperties = cl.getResourceAsStream("git.properties");

        if (gitProperties != null) {
            try {
                Properties props = new Properties();
                props.load(gitProperties);
                String versionStr = props.getProperty("git.build.version");
                String abbrevStr = props.getProperty("git.commit.id.abbrev");
                String branchStr = props.getProperty("git.branch");
                return new Git(abbrevStr, branchStr, versionStr);
            } catch (IOException e) {
                // Ignore
            } finally {
                _close(gitProperties);
            }
        }
        return new Git("unknown", "unknown", "unknown");
    }

    private static void _close(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
        }
    }

}