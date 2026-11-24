package club.abelix.commons.data.vote;

public record VotePlatform(
        int platformId,
        String name,
        String identifier,
        String url,
        String textureValue,
        int priority,
        boolean active
) {
}
