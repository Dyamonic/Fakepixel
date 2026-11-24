package club.abelix.games.fakevival.mmo;

public record MMODamageResult(double finalDamage, boolean critical, MMODamageSourceType sourceType, double lifestealAmount) {
}
