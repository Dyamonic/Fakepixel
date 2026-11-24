package club.abelix.games.fakevival.smpworlds;

public record SMPWorldData(String worldId, int formatVersion, String compression, byte[] data,
                           String checksum, long sizeBytes, long updatedAt) {
}
