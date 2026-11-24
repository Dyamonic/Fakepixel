package club.abelix.commons.util.version;

public record Git(String abbrev, String branch, String version) {

    public String displayVersion() {
        return String.format("%s (git-%s)", version, abbrev);
    }
}
