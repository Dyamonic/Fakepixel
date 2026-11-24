package club.abelix.games.fakevival.smpworlds;

public enum SMPWorldFormat {
    ZIP_V1("zip-v1");

    private final String id;

    SMPWorldFormat(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
