package club.abelix.games.fakevival.recipes;

import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.Map;

public record CustomRecipe(
        String id,
        String displayName,
        RecipeCategory category,
        String resultId,
        List<String> shape,
        Map<Character, String> ingredients,
        Map<Character, Integer> ingredientAmounts,
        RecipeUnlock unlock,
        NamespacedKey key
) {
    public int ingredientAmount(char symbol) {
        return Math.max(1, ingredientAmounts.getOrDefault(symbol, 1));
    }
}
