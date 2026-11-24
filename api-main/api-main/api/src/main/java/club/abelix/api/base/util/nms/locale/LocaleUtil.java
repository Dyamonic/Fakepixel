package club.abelix.api.base.util.nms.locale;

import club.abelix.api.FalconAPI;
import club.abelix.api.nms.NmsApi;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.math.MathUtil;
import club.abelix.commons.version.GameVersion;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public final class LocaleUtil {

    public static String translate(@NonNull Language language, @NonNull EntityType entity) {
        return language.getMessage("entity_" + FalconAPI.getNmsApi().getPlayerApi().getNmsEntityName(entity)
                .replace('.', '_'));
    }

    public static String translate(@NonNull Language language, @NonNull ItemStack item) {
        NmsApi nmsApi = FalconAPI.getNmsApi();
        String result = language.getMessage(nmsApi.getPlayerApi().getCraftItemStackName(item)
                .replace('.', '_'));

        // на 1.13+ предметы уже называются как CREEPER_SPAWN_EGG итд
        // дополнительно переводить это не нужно
        if (nmsApi.getProtocolNumber() < GameVersion.MINECRAFT_1_13.getVersion()
                && item.getType() == Material.MONSTER_EGG) {

            result += " " + translate(language, nmsApi.getPlayerApi().getSpawnEggType(item));
        }

        return result;
    }

    public static String translate(@NonNull Language language, @NonNull PotionEffectType effectType) {
        return language.getMessage(FalconAPI.getNmsApi().getPlayerApi().getEffectLocaleName(effectType)
                .replace('.', '_'));
    }

    public static String translate(@NonNull Language language, @NonNull Enchantment enchantment) {
        return language.getMessage(FalconAPI.getNmsApi().getPlayerApi().getNmsEnchantmentName(enchantment)
                .replace('.', '_'));
    }

    public static String translate(@NonNull Language language, @NonNull ItemStack item, @NonNull String format) {
        return String.format(format, translate(language, item));
    }

    public static String translateMultiple(@NonNull Language language, @NonNull ItemStack item, @NonNull String amountFormat) {
        return item.getAmount() == 1 ? translate(language, item) : translateWithAmount(language, item, "%s " + amountFormat);
    }

    public static String translateWithAmount(@NonNull Language language, @NonNull ItemStack item) {
        return translateWithAmount(language, item, "%s x%d");
    }

    public static String translateWithAmount(@NonNull Language language, @NonNull ItemStack item, @NonNull String format) {
        return String.format(format, translate(language, item), item.getAmount());
    }

    public static String translate(@NonNull Language language, @NonNull Enchantment enchantment,
                                   @NonNull String format) {
        return String.format(format, translate(language, enchantment));
    }

    public static String translateWithLevel(@NonNull Language language, @NonNull Enchantment enchantment,
                                            int level) {
        return translateWithLevel(language, enchantment, level, "%s %s");
    }

    public static String translateWithLevel(@NonNull Language language, @NonNull Enchantment enchantment,
                                            int level, @NonNull String format) {
        return String.format(format, translate(language, enchantment), MathUtil.getRoman(level));
    }

    public static String translateWithAmplifierAndDuration(@NonNull Language language, @NonNull PotionEffectType effectType,
                                                           int amplifier, int duration) {
        return translateWithAmplifierAndDuration(language, effectType, amplifier, duration, "%s %s %s");
    }

    public static String translateWithAmplifierAndDuration(@NonNull Language language, @NonNull PotionEffectType effectType,
                                                           int amplifier, int duration, @NonNull String format) {
        String durationString = duration > 1_000_000
                ? language.getMessage("potion_infinity")
                : (int) (duration / 20D) + "s";

        durationString = "(" + durationString + ")";

        return String.format(format, translate(language, effectType), MathUtil.getRoman(amplifier),
                duration > 0 ? durationString : "");
    }

    public static String translateWithAmplifier(@NonNull Language language, @NonNull PotionEffectType effectType,
                                                int amplifier) {
        return translateWithAmplifier(language, effectType, amplifier, "%s %s");
    }

    public static String translateWithAmplifier(@NonNull Language language, @NonNull PotionEffectType effectType,
                                                int amplifier, @NonNull String format) {
        return String.format(format, translate(language, effectType), MathUtil.getRoman(amplifier));
    }

    public static String translate(@NonNull Language language, @NonNull PotionEffectType effectType, @NonNull String format) {
        return String.format(format, translate(language, effectType));
    }

}
