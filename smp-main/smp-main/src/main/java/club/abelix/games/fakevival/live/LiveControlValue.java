package club.abelix.games.fakevival.live;

public record LiveControlValue(String key, String value, LiveControlType type, String category, String reason, String updatedBy, long updatedAt) {
}
