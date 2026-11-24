package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Locale;

public final class EditItemCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final CustomItemManager customItemManager;

    public EditItemCommand(JavaPlugin plugin, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.customItemManager = customItemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.requireDev(plugin, sender)) return true;
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);

        if (args.length == 0) {
            customItemManager.openEditor(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> save(player, args);
            case "load" -> load(player, args);
            case "list" -> list(player);
            case "abilities" -> abilities(player);
            case "delete" -> delete(player, args);
            case "id" -> setId(player, args);
            case "name" -> name(player, args);
            case "lore" -> lore(player, args);
            case "rarity" -> rarity(player, args);
            case "type" -> type(player, args);
            case "material" -> material(player, args);
            case "stat" -> stat(player, args);
            case "ability" -> ability(player, args);
            case "flag" -> flag(player, args);
            case "sellprice" -> sellPrice(player, args);
            case "requirement" -> requirement(player, args);
            default -> usage(player);
        }
        return true;
    }

    private void save(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem save <id> [--override-static]"));
            return;
        }
        boolean overrideStatic = Arrays.stream(args).anyMatch("--override-static"::equalsIgnoreCase);
        CustomItemManager.SaveResult result = customItemManager.saveDatabaseItem(args[1], held(player), player.getUniqueId().toString(), overrideStatic);
        switch (result) {
            case SAVED -> player.sendMessage(CC.color("&aSaved custom item &f" + normalize(args[1]) + "&a to MariaDB."));
            case STATIC_ID -> player.sendMessage(CC.color("&cThat id belongs to a static registry item. Add &f--override-static &cto intentionally shadow it."));
            case DATABASE_DISABLED -> player.sendMessage(CC.color("&cMariaDB is disabled; /edititem save requires database persistence."));
            case INVALID_ID -> player.sendMessage(CC.color("&cInvalid id. Use letters, numbers, and underscores, max 64 chars."));
            case INVALID_ITEM -> player.sendMessage(CC.color("&cHold a valid item first."));
            case ERROR -> player.sendMessage(CC.color("&cCould not save that custom item. Check console."));
        }
    }

    private void load(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem load <id>"));
            return;
        }
        if (!customItemManager.itemExists(args[1])) {
            player.sendMessage(CC.color("&cUnknown custom item id: &f" + normalize(args[1])));
            return;
        }
        player.getInventory().addItem(customItemManager.createItem(args[1], 1));
        player.sendMessage(CC.color("&aLoaded custom item &f" + normalize(args[1]) + "&a."));
    }

    private void list(Player player) {
        player.sendMessage(CC.color("&bDatabase custom items:"));
        if (customItemManager.getDatabaseItemIds().isEmpty()) {
            player.sendMessage(CC.color("&7- none"));
            return;
        }
        player.sendMessage(CC.color("&7- &f" + String.join("&7, &f", customItemManager.getDatabaseItemIds())));
    }

    private void abilities(Player player) {
        player.sendMessage(CC.color("&bCustom item abilities:"));
        String ids = customItemManager.abilityRegistry().all().stream()
                .map(ability -> ability.id() + " &8(" + ability.trigger().name() + ", " + ability.manaCost() + " mana&8)")
                .reduce((left, right) -> left + "&7, &f" + right)
                .orElse("none");
        player.sendMessage(CC.color("&7- &f" + ids));
    }

    private void delete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem delete <id>"));
            return;
        }
        if (customItemManager.isStaticItem(args[1]) && !customItemManager.isDatabaseItem(args[1])) {
            player.sendMessage(CC.color("&cThat is a static registry item and cannot be deleted with /edititem."));
            return;
        }
        if (customItemManager.deleteDatabaseItem(args[1], player.getName())) {
            player.sendMessage(CC.color("&aDeleted database custom item &f" + normalize(args[1]) + "&a."));
        } else {
            player.sendMessage(CC.color("&cCould not delete that database custom item."));
        }
    }

    private void setId(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem id <id>"));
            return;
        }
        if (customItemManager.tagItemId(held(player), args[1])) {
            logEdit(player, "id", normalize(args[1]));
            player.sendMessage(CC.color("&aSet held item id to &f" + normalize(args[1]) + "&a."));
        } else {
            player.sendMessage(CC.color("&cHold a valid item first."));
        }
    }

    private void name(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem name <name>"));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (customItemManager.setDisplayName(held(player), name)) {
            logEdit(player, "name", name);
            player.sendMessage(CC.color("&aUpdated display name."));
        } else player.sendMessage(CC.color("&cHold a valid item first."));
    }

    private void lore(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem lore add <line>|clear"));
            return;
        }
        if (args[1].equalsIgnoreCase("clear")) {
            if (customItemManager.clearLore(held(player))) {
                logEdit(player, "lore clear", "");
                player.sendMessage(CC.color("&aCleared lore."));
            } else player.sendMessage(CC.color("&cHold a valid item first."));
            return;
        }
        if (args[1].equalsIgnoreCase("add") && args.length >= 3) {
            String line = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            if (customItemManager.addLoreLine(held(player), line)) {
                logEdit(player, "lore add", line);
                player.sendMessage(CC.color("&aAdded lore line."));
            } else player.sendMessage(CC.color("&cHold a valid item first."));
            return;
        }
        player.sendMessage(CC.color("&cUsage: /edititem lore add <line>|clear"));
    }

    private void rarity(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem rarity <rarity>"));
            return;
        }
        if (customItemManager.setRarity(held(player), args[1])) {
            logEdit(player, "rarity", args[1]);
            player.sendMessage(CC.color("&aUpdated rarity."));
        } else player.sendMessage(CC.color("&cHold a valid item first."));
    }

    private void type(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem type <item_type>"));
            return;
        }
        if (customItemManager.setItemType(held(player), args[1])) {
            logEdit(player, "type", args[1]);
            player.sendMessage(CC.color("&aUpdated item type."));
        } else player.sendMessage(CC.color("&cHold a valid item first."));
    }

    private void material(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem material <material>"));
            return;
        }
        if (customItemManager.setMaterial(held(player), args[1])) {
            logEdit(player, "material", args[1]);
            player.sendMessage(CC.color("&aUpdated material."));
        } else player.sendMessage(CC.color("&cInvalid material or held item."));
    }

    private void stat(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(CC.color("&cUsage: /edititem stat <stat> <amount>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException exception) {
            player.sendMessage(CC.color("&cAmount must be a number."));
            return;
        }
        if (customItemManager.setStat(held(player), args[1], amount)) {
            logEdit(player, "stat " + normalize(args[1]), String.valueOf(amount));
            player.sendMessage(CC.color("&aUpdated stat &f" + normalize(args[1]) + "&a."));
        } else player.sendMessage(CC.color("&cHold a valid item first."));
    }

    private void ability(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem ability <ability_id>|trigger <trigger>|manacost <amount>"));
            return;
        }
        if (args[1].equalsIgnoreCase("trigger") && args.length >= 3) {
            if (customItemManager.setAbilityValue(held(player), "trigger", args[2])) {
                logEdit(player, "ability trigger", args[2]);
                player.sendMessage(CC.color("&aUpdated ability trigger."));
            } else player.sendMessage(CC.color("&cCould not update ability trigger."));
            return;
        }
        if ((args[1].equalsIgnoreCase("manacost") || args[1].equalsIgnoreCase("mana")) && args.length >= 3) {
            if (customItemManager.setAbilityValue(held(player), "mana_cost", args[2])) {
                logEdit(player, "ability mana_cost", args[2]);
                player.sendMessage(CC.color("&aUpdated ability mana cost."));
            } else player.sendMessage(CC.color("&cMana cost must be a whole number."));
            return;
        }
        if (customItemManager.setAbility(held(player), args[1])) {
            logEdit(player, "ability", args[1]);
            player.sendMessage(CC.color("&aUpdated ability."));
        } else player.sendMessage(CC.color("&cHold a valid item first."));
    }

    private void flag(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(CC.color("&cUsage: /edititem flag <unbreakable|glowing|soulbound|tradeable|auctionable> <true|false>"));
            return;
        }
        if (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false")) {
            player.sendMessage(CC.color("&cFlag value must be true or false."));
            return;
        }
        if (customItemManager.setFlag(held(player), args[1], Boolean.parseBoolean(args[2]))) {
            logEdit(player, "flag " + normalize(args[1]), args[2]);
            player.sendMessage(CC.color("&aUpdated flag."));
        } else player.sendMessage(CC.color("&cHold a valid item first."));
    }

    private void sellPrice(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /edititem sellprice <amount>"));
            return;
        }
        try {
            double price = Double.parseDouble(args[1]);
            if (customItemManager.setSellPrice(held(player), price)) {
                logEdit(player, "sellprice", String.valueOf(price));
                player.sendMessage(CC.color("&aUpdated sell price."));
            } else player.sendMessage(CC.color("&cHold a valid item first."));
        } catch (NumberFormatException exception) {
            player.sendMessage(CC.color("&cSell price must be a number."));
        }
    }

    private void requirement(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(CC.color("&cUsage: /edititem requirement <key> <value>"));
            return;
        }
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (customItemManager.setRequirement(held(player), args[1], value)) {
            logEdit(player, "requirement " + normalize(args[1]), value);
            player.sendMessage(CC.color("&aUpdated requirement."));
        } else player.sendMessage(CC.color("&cHold a valid item first."));
    }

    private void usage(Player player) {
        player.sendMessage(CC.color("&b/edititem &7- open item editor"));
        player.sendMessage(CC.color("&b/edititem save/load/list/abilities/delete/id/name/lore/rarity/type/material/stat/ability/flag/sellprice/requirement"));
    }

    private ItemStack held(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item == null || item.getType() == Material.AIR ? null : item;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private void logEdit(Player player, String action, String value) {
        plugin.getLogger().info("Custom item edited by " + player.getName() + ": " + action + " -> " + value);
    }
}
