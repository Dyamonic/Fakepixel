package club.abelix.games.fakevival.smpworlds;

public record SMPWorldImportResult(boolean success, String worldId, String message, long sizeBytes,
                                   String sourceFormat) {
    public static SMPWorldImportResult failure(String worldId, String message) {
        return new SMPWorldImportResult(false, worldId, message, 0L, null);
    }
}
