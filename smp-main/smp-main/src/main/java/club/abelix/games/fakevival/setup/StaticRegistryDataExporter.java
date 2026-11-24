package club.abelix.games.fakevival.setup;

public final class StaticRegistryDataExporter {
    private StaticRegistryDataExporter() {
    }

    public static String javaSource(SMPSetupSnapshot snapshot) {
        return snapshot.data() + "\n";
    }
}
