package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.GuildManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.listeners.GuildDisplayListener;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.guilds.GuildTagColor;
import club.abelix.games.fakevival.guilds.GuildPermission;
import club.abelix.games.fakevival.guilds.GuildRoleDefinition;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Comparator;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class GuildsCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final GuildManager guildManager;
    private final GuildDisplayListener guildDisplayListener;

    public GuildsCommand(JavaPlugin plugin, GuildManager guildManager, GuildDisplayListener guildDisplayListener) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.guildDisplayListener = guildDisplayListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.guilds")) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.no-permission", "&cYou do not have permission to use guilds.")));
            return true;
        }

        if (args.length == 0) {
            openMenu(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player, label);
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            String name = args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : player.getName() + " Guild";
            create(player, name);
            return true;
        }
        if (args[0].equalsIgnoreCase("claim")) {
            if (plugin instanceof Game game && !game.getNetworkService().isOverworld()) {
                player.sendMessage(CC.color(game.getNetworkService().isHub() ? "&cYou can only use this on overworld servers. Use &e/rtp &cor go to survival first." : "&cYou cannot use this inside a dungeon."));
                return true;
            }
            claim(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("unclaim")) {
            unclaim(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            list(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            info(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("invite")) {
            invite(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("leave")) {
            leave(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("disband")) {
            disband(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("kick")) {
            kick(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("join")) {
            join(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("accept")) {
            accept(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote")) {
            setRank(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("settings")) {
            openSettings(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("members") || args[0].equalsIgnoreCase("member")) { openMembers(player, 0); return true; }
        if (args[0].equalsIgnoreCase("roles") || args[0].equalsIgnoreCase("permissions") || args[0].equalsIgnoreCase("perms")) { openRoles(player); return true; }
        if (args[0].equalsIgnoreCase("setrole")) { setRole(player, args); return true; }
        if (args[0].equalsIgnoreCase("war") || args[0].equalsIgnoreCase("wars")) { handleRelationCommand(player, args, true); return true; }
        if (args[0].equalsIgnoreCase("ally") || args[0].equalsIgnoreCase("allies")) { handleRelationCommand(player, args, false); return true; }
        if (args[0].equalsIgnoreCase("motd")) {
            motd(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("tag")) {
            tag(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("tagcolor") || args[0].equalsIgnoreCase("tagcolour")) {
            tagColor(player, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("browse")) {
            openBrowse(player);
            return true;
        }

        sendHelp(player, label);
        return true;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(CC.color("&6Guild Commands"));
        player.sendMessage(CC.color("&e/" + label + " &7- Open the guild menu."));
        player.sendMessage(CC.color("&e/" + label + " help &7- Show this command list."));
        player.sendMessage(CC.color("&e/" + label + " create [name] &7- Create a guild."));
        player.sendMessage(CC.color("&e/" + label + " list &7- List guild members."));
        player.sendMessage(CC.color("&e/" + label + " info &7- View your guild info."));
        player.sendMessage(CC.color("&e/" + label + " invite <player> &7- Invite a player."));
        player.sendMessage(CC.color("&e/" + label + " join <guild> &7- Request to join an open guild."));
        player.sendMessage(CC.color("&e/" + label + " accept <player> &7- Accept a join request."));
        player.sendMessage(CC.color("&e/" + label + " leave &7- Leave your guild."));
        player.sendMessage(CC.color("&e/" + label + " disband confirm &7- Permanently delete your guild."));
        player.sendMessage(CC.color("&e/" + label + " kick <player> &7- Remove a member."));
        player.sendMessage(CC.color("&e/" + label + " promote <rank> <player> &7- Promote a member."));
        player.sendMessage(CC.color("&e/" + label + " demote <rank> <player> &7- Demote a member."));
        player.sendMessage(CC.color("&e/" + label + " settings &7- Open guild settings."));
        player.sendMessage(CC.color("&e/" + label + " motd <message> &7- Change the guild MOTD."));
        player.sendMessage(CC.color("&e/" + label + " tag <tag> &7- Change guild tag, max 10 characters."));
        player.sendMessage(CC.color("&e/" + label + " tagcolor <color> &7- Change guild tag color."));
        player.sendMessage(CC.color("&e/" + label + " members &7- Browse guild members."));
        player.sendMessage(CC.color("&e/" + label + " roles &7- Manage guild role permissions."));
        player.sendMessage(CC.color("&e/" + label + " setrole <player> <role> &7- Change a member role."));
        player.sendMessage(CC.color("&e/" + label + " war|ally <guild> &7- Manage relations."));
        player.sendMessage(CC.color("&e/" + label + " browse &7- Browse public guilds."));
        player.sendMessage(CC.color("&e/" + label + " claim &7- Claim the current chunk."));
        player.sendMessage(CC.color("&e/" + label + " unclaim &7- Unclaim the current chunk."));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.toLowerCase(Locale.ROOT).startsWith("guild")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("back")) {
            if (type.equalsIgnoreCase("guild-tag-color")) openSettings(player);
            else if (type.startsWith("guild-role:")) openRoles(player);
            else if (type.startsWith("guild-members:")) openMenu(player);
            else if ((type.startsWith("guild-war:") || type.startsWith("guild-ally:"))) openRelations(player, type.startsWith("guild-war:"));
            else if (type.startsWith("guild-war") || type.startsWith("guild-all")) openMenu(player);
            else openMenu(player);
            return;
        }
        if (type.startsWith("guild-members:")) { int page = parsePage(type); if (name.equals("previous page")) openMembers(player, page - 1); else if (name.equals("next page")) openMembers(player, page + 1); return; }
        if (type.equals("guild-roles")) { String role = metadata(clicked, "Guild Role ID: "); if (role != null) openRolePermissions(player, role); return; }
        if (type.startsWith("guild-role:")) { String permission = metadata(clicked, "Guild Permission: "); if (permission != null) { try { if (!guildManager.toggleRolePermission(player, type.substring("guild-role:".length()), GuildPermission.valueOf(permission))) player.sendMessage(CC.color("&cYou cannot edit that role permission.")); } catch (IllegalArgumentException ignored) { } openRolePermissions(player, type.substring("guild-role:".length())); } return; }
        if (type.startsWith("guild-war") || type.startsWith("guild-ally")) { handleRelationClick(player, type, clicked, event.getClick()); return; }
        if (type.equalsIgnoreCase("guild-tag-color")) {
            String color = tagColorValue(clicked);
            if (color != null) selectTagColor(player, color);
            return;
        }
        if (type.equalsIgnoreCase("guild-browse")) {
            String guild = loreValue(clicked);
            if (guild != null) {
                handleJoinResult(player, guildManager.join(player, guild), guild);
                player.closeInventory();
            }
            return;
        }
        if (type.equalsIgnoreCase("guild-settings")) {
            if (name.equals("tag color")) {
                openTagColorMenu(player);
                return;
            }
            if (name.equals("toggle privacy")) {
                String guildId = guildManager.getGuildId(player);
                if (guildId != null && guildManager.canManage(player)) {
                    guildManager.setOpen(player, !guildManager.isOpen(guildId));
                    player.sendMessage(CC.color("&aGuild privacy updated."));
                    openSettings(player);
                }
                return;
            }
            String setting = metadata(clicked, "Guild Setting: ");
            if (setting != null) { String value = guildManager.cycleSetting(player, setting); if (value == null) player.sendMessage(CC.color("&cYou do not have permission to edit guild settings.")); else player.sendMessage(CC.color("&aGuild setting updated: &e" + setting + " &7-> &a" + value)); openSettings(player); return; }
            if (name.equals("disband guild")) {
                disband(player, new String[]{"disband"});
            }
            return;
        }
        if (name.equals("create guild")) {
            create(player, player.getName() + " Guild");
            openMenu(player);
            return;
        }
        if (name.equals("claim current chunk")) {
            claim(player);
            openMenu(player);
            return;
        }
        if (name.equals("unclaim current chunk")) {
            unclaim(player);
            openMenu(player);
            return;
        }
        if (name.equals("buy more claims")) {
            player.performCommand("store");
            return;
        }
        if (name.equals("browse guilds")) {
            openBrowse(player);
            return;
        }
        if (name.equals("guild settings")) {
            openSettings(player);
            return;
        }
        if (name.equals("guild members")) {
            openMembers(player, 0);
            return;
        }
        if (name.equals("guild roles")) { openRoles(player); return; }
        if (name.equals("guild wars")) { openRelations(player, true); return; }
        if (name.equals("guild allies")) { openRelations(player, false); return; }
        if (name.equals("guild help")) {
            sendHelp(player, "g");
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder(String type) && type.toLowerCase(Locale.ROOT).startsWith("guild")) event.setCancelled(true);
    }

    private void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("guilds"), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.menu.title", "&6Guilds")));
        boolean hasGuild = guildManager.hasGuild(player);
        inventory.setItem(4, item(Material.BELL, "&6Guilds", List.of(
                hasGuild ? "&7Guild: &f" + guildManager.getGuildName(player) : "&7You are not in a guild yet.",
                "&7Claims: &e" + guildManager.getClaimCount(player) + "&7/&e" + guildManager.getClaimLimit(player),
                "&7Current Chunk: &f" + guildManager.chunkKey(player.getLocation()),
                "&7Claim Owner: &f" + currentClaimOwner(player),
                "",
                "&7Use guild land claims to protect",
                "&7chunks for your guild."
        )));

        inventory.setItem(20, item(hasGuild ? Material.GRAY_DYE : Material.LIME_DYE, "&aCreate Guild", List.of(
                hasGuild ? "&cYou already have a guild." : "&7Creates a guild named &f" + player.getName() + " Guild&7.",
                hasGuild ? "" : "&eClick to create.",
                "&8Guild Action: create"
        )));
        inventory.setItem(22, item(Material.GRASS_BLOCK, "&aClaim Current Chunk", List.of(
                hasGuild ? "&7Claims the chunk you are standing in." : "&cCreate a guild first.",
                "&7Claims: &e" + guildManager.getClaimCount(player) + "&7/&e" + guildManager.getClaimLimit(player),
                "&7Owner: &f" + currentClaimOwner(player),
                "",
                "&eClick to claim.",
                "&8Guild Action: claim"
        )));
        inventory.setItem(24, item(Material.IRON_SHOVEL, "&cUnclaim Current Chunk", List.of(
                "&7Only the guild owner can unclaim.",
                "&7Owner: &f" + currentClaimOwner(player),
                "",
                "&eClick to unclaim.",
                "&8Guild Action: unclaim"
        )));
        inventory.setItem(40, item(Material.NETHER_STAR, "&bBuy More Claims", List.of(
                "&7Open the store account upgrades menu.",
                "&7Buy &aExtra Guild Claim &7upgrades",
                "&7to increase your guild land limit.",
                "",
                "&eClick to open /store."
        )));
        inventory.setItem(38, item(Material.SPYGLASS, "&bBrowse Guilds", List.of(
                "&7View open guilds that accept joins.",
                "",
                "&eClick to browse."
        )));
        inventory.setItem(39, item(Material.PLAYER_HEAD, "&eGuild Members", List.of(
                hasGuild ? "&7View your current guild roster." : "&cJoin or create a guild first.",
                "",
                "&eClick to list members."
        )));
        inventory.setItem(42, item(Material.COMPARATOR, "&eGuild Settings", List.of(
                hasGuild ? "&7Open guild settings." : "&cJoin or create a guild first.",
                "&7Privacy, MOTD, and guild info.",
                "",
                "&eClick to open."
        )));
        inventory.setItem(10, item(Material.ARMOR_STAND, "&bGuild Roles", List.of("&7View roles and guild permissions.", "", "&eClick to open.")));
        inventory.setItem(12, item(Material.IRON_SWORD, "&cGuild Wars", List.of("&7Active: &c" + (hasGuild ? guildManager.getActiveWarCount(guildManager.getGuildId(player)) : 0), "", "&eClick to open.")));
        inventory.setItem(14, item(Material.TOTEM_OF_UNDYING, "&aGuild Allies", List.of("&7Active: &a" + (hasGuild ? guildManager.getActiveAllyCount(guildManager.getGuildId(player)) : 0), "", "&eClick to open.")));
        inventory.setItem(41, item(Material.WRITABLE_BOOK, "&eGuild Help", List.of(
                "&7Show all guild commands in chat.",
                "",
                "&eClick to view help."
        )));
        inventory.setItem(49, item(Material.BOOK, "&eGuild Info", guildInfo(player)));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close the guild menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void create(Player player, String name) {
        if (guildManager.createGuild(player, name)) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.created", "&aCreated guild &f%guild%&a.").replace("%guild%", Objects.requireNonNull(guildManager.getGuildName(player)))));
            refreshGuildDisplays();
        } else {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.already-in-guild", "&cYou are already in a guild.")));
        }
    }

    private void list(Player player) {
        String guildId = guildManager.getGuildId(player);
        if (guildId == null) {
            player.sendMessage(CC.color("&cYou are not in a guild."));
            return;
        }
        player.sendMessage(CC.color("&6Guild Members &8- &f" + guildManager.getGuildName(guildId)));
        for (java.util.UUID uuid : guildManager.getMembers(guildId)) {
            player.sendMessage(CC.color("&8- &f" + guildManager.getMemberName(uuid) + " &7(" + guildManager.getRank(guildId, uuid) + ")"));
        }
    }

    private void info(Player player) {
        String guildId = guildManager.getGuildId(player);
        if (guildId == null) {
            player.sendMessage(CC.color("&cYou are not in a guild."));
            return;
        }
        player.sendMessage(CC.color("&6Guild Info"));
        player.sendMessage(CC.color("&7Name: &f" + guildManager.getGuildName(guildId)));
        player.sendMessage(CC.color("&7Members: &e" + guildManager.getMemberCount(guildId)));
        player.sendMessage(CC.color("&7Privacy: " + (guildManager.isOpen(guildId) ? "&aOpen" : "&cPrivate")));
        player.sendMessage(CC.color("&7Tag: " + guildManager.getGuildTag(player)));
        player.sendMessage(CC.color("&7MOTD: &f" + guildManager.getMotd(guildId)));
        player.sendMessage(CC.color("&7Claims: &e" + guildManager.getClaimCount(player) + "&7/&e" + guildManager.getClaimLimit(player)));
    }

    private void invite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /g invite <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(CC.color("&cThat player is not online."));
            return;
        }
        if (guildManager.invite(player, target)) {
            player.sendMessage(CC.color("&aInvited &f" + target.getName() + " &ato your guild."));
            target.sendMessage(CC.color("&e" + player.getName() + " &finvited you to join &e" + guildManager.getGuildName(player) + "&f. Use &a/guild join " + guildManager.getGuildName(player) + " &eto accept."));
        } else {
            player.sendMessage(CC.color("&cCould not invite that player."));
        }
    }

    private void leave(Player player) {
        if (guildManager.leave(player)) {
            player.sendMessage(CC.color("&aYou left your guild."));
            refreshGuildDisplays();
        } else {
            player.sendMessage(CC.color("&cYou cannot leave. Owners must use &e/g disband confirm &cor transfer ownership later."));
        }
    }

    private void disband(Player player, String[] args) {
        if (!guildManager.hasGuild(player)) {
            player.sendMessage(CC.color("&cYou are not in a guild."));
            return;
        }
        if (!guildManager.isOwner(player)) {
            player.sendMessage(CC.color("&cOnly the guild owner can disband the guild."));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage(CC.color("&cThis permanently deletes your guild, removes all members, and unclaims all guild land."));
            player.sendMessage(CC.color("&eUse /g disband confirm &cto confirm."));
            return;
        }
        String name = guildManager.getGuildName(player);
        if (guildManager.disband(player)) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.disbanded", "&cDisbanded guild &f%guild%&c.").replace("%guild%", name == null ? "Guild" : name)));
            refreshGuildDisplays();
        } else {
            player.sendMessage(CC.color("&cCould not disband your guild."));
        }
    }

    private void kick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /g kick <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (guildManager.kick(player, target)) {
            player.sendMessage(CC.color("&aKicked &f" + args[1] + " &afrom the guild."));
            if (target.isOnline() && target.getPlayer() != null) target.getPlayer().sendMessage(CC.color("&cYou were kicked from your guild."));
            refreshGuildDisplays();
        } else {
            player.sendMessage(CC.color("&cCould not kick that player."));
        }
    }

    private void join(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /guild join <guild>"));
            return;
        }
        String guild = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        handleJoinResult(player, guildManager.join(player, guild), guild);
    }

    private void handleJoinResult(Player player, GuildManager.JoinResult result, String guild) {
        switch (result) {
            case JOINED -> {
                player.sendMessage(CC.color("&aJoined guild &f" + guild + "&a."));
                refreshGuildDisplays();
            }
            case REQUESTED -> player.sendMessage(CC.color("&aRequested to join &f" + guild + "&a."));
            case ALREADY_IN_GUILD -> player.sendMessage(CC.color("&cYou are already in a guild."));
            case NOT_FOUND -> player.sendMessage(CC.color("&cThat guild was not found."));
        }
    }

    private void accept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /g accept <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (guildManager.accept(player, target)) {
            player.sendMessage(CC.color("&aAccepted &f" + args[1] + "&a into your guild."));
            if (target.isOnline() && target.getPlayer() != null) target.getPlayer().sendMessage(CC.color("&aYour guild join request was accepted."));
            refreshGuildDisplays();
        } else {
            player.sendMessage(CC.color("&cCould not accept that request."));
        }
    }

    private void setRank(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(CC.color("&cUsage: /g " + args[0].toLowerCase(Locale.ROOT) + " <rank> <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (guildManager.setRank(player, target, args[1])) {
            player.sendMessage(CC.color("&aSet &f" + args[2] + " &ato rank &e" + args[1] + "&a."));
        } else {
            player.sendMessage(CC.color("&cCould not change that player's rank. Valid ranks: officer, member."));
        }
    }

    private void motd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /g motd <message>"));
            return;
        }
        String motd = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (guildManager.setMotd(player, motd)) player.sendMessage(CC.color("&aGuild MOTD updated."));
        else player.sendMessage(CC.color("&cYou cannot update the guild MOTD."));
    }

    private void tag(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /g tag <tagname> &7(Max 10 letters/numbers)"));
            return;
        }
        GuildManager.TagResult result = guildManager.setTag(player, args[1]);
        switch (result) {
            case UPDATED -> {
                player.sendMessage(CC.color("&aGuild tag updated to " + guildManager.getGuildTag(player)));
                refreshGuildDisplays();
            }
            case NO_GUILD -> player.sendMessage(CC.color("&cYou are not in a guild."));
            case NOT_OWNER -> player.sendMessage(CC.color("&cOnly the guild owner can change the tag."));
            case BLOCKED -> player.sendMessage(CC.color("&cThat guild tag is blocked by the word filter."));
            case INVALID -> player.sendMessage(CC.color("&cInvalid tag. Use 1-10 letters/numbers only."));
        }
    }

    private void tagColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /g tagcolor <color>"));
            return;
        }
        selectTagColor(player, args[1]);
    }

    private void selectTagColor(Player player, String requestedColor) {
        GuildManager.TagResult result = guildManager.setTagColor(player, requestedColor);
        switch (result) {
            case UPDATED -> {
                GuildTagColor color = GuildTagColor.safe(requestedColor);
                player.sendMessage(CC.color("&aGuild tag color changed to " + color.code() + color.name() + "&a."));
                refreshGuildDisplays();
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder(String type)
                        && type.equalsIgnoreCase("guild-tag-color")) openTagColorMenu(player);
            }
            case NO_GUILD -> player.sendMessage(CC.color("&cYou are not in a guild."));
            case NOT_OWNER -> player.sendMessage(CC.color("&cYou do not have permission to change the guild tag color."));
            default -> player.sendMessage(CC.color("&cInvalid tag color. Available colors: &7GRAY, &fWHITE, &aGREEN, &bAQUA, &cRED, &dLIGHT_PURPLE, &eYELLOW, &6GOLD&c."));
        }
    }

    private void openBrowse(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("guild-browse"), 54, CC.color("&8Browse Guilds"));
        inventory.setItem(4, item(Material.SPYGLASS, "&bOpen Guilds", List.of("&7Private guilds are hidden.", "&eClick a guild to join.")));
        int[] slots = contentSlots();
        List<String> guilds = guildManager.getOpenGuildIds();
        for (int i = 0; i < slots.length && i < guilds.size(); i++) {
            String id = guilds.get(i);
            inventory.setItem(slots[i], item(Material.WHITE_BANNER, "&6" + guildManager.getGuildName(id), List.of(
                    "&7Tag: " + (guildManager.formatGuildTag(id).isBlank() ? "&fNone" : guildManager.formatGuildTag(id)),
                    "&7Members: &e" + guildManager.getMemberCount(id),
                    "&7MOTD: &f" + guildManager.getMotd(id),
                    "",
                    "&eClick to join.",
                    "&8Guild ID: " + id
            )));
        }
        inventory.setItem(45, item(Material.ARROW, "&cBack", List.of("&7Return to guild menu.")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openSettings(Player player) {
        if (!guildManager.hasGuild(player)) {
            player.sendMessage(CC.color("&cJoin or create a guild first."));
            return;
        }
        String guildId = guildManager.getGuildId(player);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("guild-settings"), 54, CC.color("&8Guild Settings"));
        inventory.setItem(4, item(Material.COMPARATOR, "&eGuild Settings", List.of(
                "&7Guild: &f" + guildManager.getGuildName(guildId),
                "&7Privacy: " + (guildManager.isOpen(guildId) ? "&aOpen" : "&cPrivate"),
                "&7Tag: " + guildManager.getGuildTag(player),
                "&7MOTD: &f" + guildManager.getMotd(guildId)
        )));
        inventory.setItem(22, item(guildManager.isOpen(guildId) ? Material.LIME_DYE : Material.GRAY_DYE, "&eToggle Privacy", List.of(
                guildManager.canManage(player) ? "&7Switch between open and private." : "&cOfficer or owner only.",
                "&7Current: " + (guildManager.isOpen(guildId) ? "&aOpen" : "&cPrivate"),
                "",
                "&eClick to toggle."
        )));
        inventory.setItem(10, settingItem(Material.OAK_DOOR, "&eJoin Type", "join-type", guildManager.getJoinType(guildId)));
        inventory.setItem(11, settingItem(Material.BOOK, "&eGuild Chat Toggle", "guild-chat", guildManager.getGuildSetting(guildId, "guild-chat")));
        inventory.setItem(12, settingItem(Material.IRON_SWORD, "&cGuild War Requests", "war-requests", guildManager.getGuildSetting(guildId, "war-requests")));
        inventory.setItem(13, settingItem(Material.TOTEM_OF_UNDYING, "&aGuild Ally Requests", "ally-requests", guildManager.getGuildSetting(guildId, "ally-requests")));
        inventory.setItem(14, settingItem(Material.PLAYER_HEAD, "&ePublic Member List", "public-member-list", guildManager.getGuildSetting(guildId, "public-member-list")));
        inventory.setItem(15, settingItem(Material.SHIELD, "&eGuild Friendly Fire", "friendly-fire", guildManager.getGuildSetting(guildId, "friendly-fire")));
        inventory.setItem(31, item(Material.TNT, "&cDisband Guild", List.of(
                guildManager.isOwner(player) ? "&7Permanently delete this guild." : "&cOwner only.",
                "&7This removes all members and claims.",
                "",
                guildManager.isOwner(player) ? "&eClick for confirmation command." : "&cYou cannot do this."
        )));
        inventory.setItem(24, item(Material.NAME_TAG, "&bGuild Tag", List.of(
                "&7Current: " + guildManager.getGuildTag(player),
                "&7Use &e/g tag <tagname>",
                "&7Max 10 letters/numbers."
        )));
        inventory.setItem(25, item(Material.PAINTING, "&bTag Color", List.of(
                "&7Current: " + guildManager.getGuildTag(player),
                guildManager.canManage(player) ? "&7Choose a Hypixel-style tag color." : "&cOfficer or owner only.",
                "",
                guildManager.canManage(player) ? "&eClick to choose!" : "&cYou cannot change this."
        )));
        inventory.setItem(45, item(Material.ARROW, "&cBack", List.of("&7Return to guild menu.")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openTagColorMenu(Player player) {
        if (!guildManager.hasGuild(player)) {
            player.sendMessage(CC.color("&cYou are not in a guild."));
            return;
        }
        if (!guildManager.canManage(player)) {
            player.sendMessage(CC.color("&cYou do not have permission to change the guild tag color."));
            return;
        }
        Inventory inventory = Bukkit.createInventory(new MenuHolder("guild-tag-color"), 54, CC.color("&8Guild Tag Color"));
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28};
        String current = guildManager.getTagColorId(player);
        String rawTag = guildManager.getRawGuildTag(player);
        if (rawTag.isBlank()) rawTag = "TAG";
        GuildTagColor[] colors = GuildTagColor.values();
        for (int index = 0; index < colors.length; index++) {
            GuildTagColor color = colors[index];
            List<String> lore = new ArrayList<>();
            lore.add("&7Preview: &7[" + color.code() + rawTag + "&7]");
            lore.add("");
            if (color.name().equals(current)) lore.add("&aCurrently selected");
            else lore.add("&eClick to select!");
            lore.add("&8Tag Color: " + color.name());
            inventory.setItem(slots[index], item(color.icon(), color.code() + prettyColor(color.name()), lore));
        }
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to guild settings.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openMembers(Player player, int requestedPage) {
        String guildId = guildManager.getGuildId(player);
        if (guildId == null) { player.sendMessage(CC.color("&cYou are not in a guild.")); return; }
        if (!guildManager.getGuildSettingBoolean(guildId, "public-member-list", true) && !guildManager.canManage(player)) { player.sendMessage(CC.color("&cThis guild's member list is private.")); return; }
        List<UUID> members = new ArrayList<>(guildManager.getMembers(guildId));
        members.sort(Comparator.comparingInt((UUID id) -> guildManager.getRolePriority(guildId, guildManager.getMemberRole(guildId, id))).reversed().thenComparing(id -> Bukkit.getPlayer(id) == null).thenComparingLong(id -> guildManager.getJoinedAt(guildId, id)).thenComparing(guildManager::getMemberName, String.CASE_INSENSITIVE_ORDER));
        int maxPage = Math.max(0, (members.size() - 1) / 21), page = Math.max(0, Math.min(requestedPage, maxPage));
        Inventory inventory = Bukkit.createInventory(new MenuHolder("guild-members:" + page), 54, CC.color("&8Guild Members")); int[] slots = contentSlots();
        for (int i = 0; i < slots.length; i++) { int index = page * 21 + i; if (index >= members.size()) break; UUID uuid = members.get(index); Player online = Bukkit.getPlayer(uuid); ItemStack head = new ItemStack(Material.PLAYER_HEAD); if (online != null && head.getItemMeta() instanceof SkullMeta skull) { skull.setOwningPlayer(online); head.setItemMeta(skull); } long joined = guildManager.getJoinedAt(guildId, uuid), playtime = guildManager.getPlaytimeSeconds(uuid); setMeta(head, "&b" + guildManager.getMemberName(uuid), List.of("&7Guild Role: &e" + guildManager.getMemberRole(guildId, uuid), "&7Server Rank: " + rankPrefix(online), "&7Status: " + (online == null ? "&cOffline" : "&aOnline"), "&7Playtime: &a" + (playtime < 0 ? "&7Unknown" : duration(playtime * 1000L)), "&7In Guild For: &e" + duration(Math.max(0, System.currentTimeMillis() - joined)), "", "&7Joined: &f" + date(joined), "&8Member UUID: " + uuid)); inventory.setItem(slots[i], head); }
        inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + (page + 1)))); inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to guild menu."))); inventory.setItem(53, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (page + 1)))); fill(inventory); player.openInventory(inventory);
    }

    private void openRoles(Player player) {
        String guildId = guildManager.getGuildId(player); if (guildId == null) { player.sendMessage(CC.color("&cYou are not in a guild.")); return; }
        Inventory inventory = Bukkit.createInventory(new MenuHolder("guild-roles"), 54, CC.color("&8Guild Roles")); int[] slots = contentSlots(); List<GuildRoleDefinition> roles = guildManager.getRoles(guildId);
        for (int i=0;i<roles.size()&&i<slots.length;i++){GuildRoleDefinition role=roles.get(i);inventory.setItem(slots[i],item(role.id().equals("GUILD_MASTER")?Material.NETHER_STAR:Material.NAME_TAG,"&e"+role.name(),List.of("&7Priority: &e"+role.priority(),"&7Members: &a"+guildManager.getRoleMemberCount(guildId,role.id()),"&7Permissions: &b"+guildManager.getRolePermissions(guildId,role.id()).size(),"","&eClick to edit permissions!","&8Guild Role ID: "+role.id())));}
        inventory.setItem(49,item(Material.BARRIER,"&cBack",List.of("&7Return to guild menu.")));fill(inventory);player.openInventory(inventory);
    }

    private void openRolePermissions(Player player,String roleId){String guildId=guildManager.getGuildId(player);GuildRoleDefinition role=guildId==null?null:guildManager.getRole(guildId,roleId);if(role==null)return;Inventory inventory=Bukkit.createInventory(new MenuHolder("guild-role:"+role.id()),54,CC.color("&8Role Permissions - "+role.name()));GuildPermission[] permissions=GuildPermission.values();int[] slots=contentSlots();for(int i=0;i<permissions.length&&i<slots.length;i++){GuildPermission permission=permissions[i];boolean enabled=guildManager.getRolePermissions(guildId,role.id()).contains(permission);inventory.setItem(slots[i],item(enabled?Material.LIME_DYE:Material.RED_DYE,(enabled?"&a":"&c")+prettyColor(permission.name()),List.of("&7Permission: &e"+permission.name(),"&7Status: "+(enabled?"&aEnabled":"&cDisabled"),"","&eClick to toggle!","&8Guild Permission: "+permission.name())));}inventory.setItem(49,item(Material.BARRIER,"&cBack",List.of("&7Return to roles.")));fill(inventory);player.openInventory(inventory);}

    private void setRole(Player player,String[] args){if(args.length<3){player.sendMessage(CC.color("&cUsage: /g setrole <player> <role>"));return;}String guildId=guildManager.getGuildId(player);if(guildId==null){player.sendMessage(CC.color("&cThat player is not in your guild."));return;}UUID target=null;for(UUID uuid:guildManager.getMembers(guildId))if(guildManager.getMemberName(uuid).equalsIgnoreCase(args[1])){target=uuid;break;}if(target==null){player.sendMessage(CC.color("&cThat player is not in your guild."));return;}GuildManager.RoleResult result=guildManager.setMemberRole(player,target,args[2]);switch(result){case UPDATED->{String role=args[2].toUpperCase(Locale.ROOT);player.sendMessage(CC.color("&aSet &e"+guildManager.getMemberName(target)+"&a's guild role to &b"+role+"&a."));Player online=Bukkit.getPlayer(target);if(online!=null)online.sendMessage(CC.color("&eYour guild role was changed to &b"+role+"&e."));}case INVALID_ROLE->player.sendMessage(CC.color("&cThat guild role does not exist."));case NOT_MEMBER->player.sendMessage(CC.color("&cThat player is not in your guild."));default->player.sendMessage(CC.color("&cYou do not have permission to set guild roles."));}}

    private void handleRelationCommand(Player player,String[] args,boolean war){if(args.length==1||args[0].equalsIgnoreCase(war?"wars":"allies")){openRelations(player,war);return;}String action=args[1].toLowerCase(Locale.ROOT);if((action.equals("accept")||action.equals("deny")||action.equals("cancel")||action.equals("remove"))&&args.length<3){player.sendMessage(CC.color("&cUsage: /g "+(war?"war":"ally")+" "+action+" <guild>"));return;}boolean subcommand=action.equals("accept")||action.equals("deny")||action.equals("cancel")||action.equals("remove");String other=String.join(" ",java.util.Arrays.copyOfRange(args,subcommand?2:1,args.length));GuildManager.RelationResult result;if(action.equals("accept"))result=war?guildManager.acceptWar(player,other):guildManager.acceptAlly(player,other);else if(action.equals("deny"))result=war?guildManager.denyWar(player,other):guildManager.denyAlly(player,other);else if(action.equals("cancel"))result=war?guildManager.cancelWar(player,other):guildManager.cancelAlly(player,other);else if(action.equals("remove")&&!war)result=guildManager.removeAlly(player,other);else result=war?guildManager.requestWar(player,other):guildManager.requestAlly(player,other);sendRelationResult(player,result,war,other);}

    private void sendRelationResult(Player player,GuildManager.RelationResult result,boolean war,String other){switch(result){case REQUESTED->{player.sendMessage(CC.color("&a"+(war?"War":"Ally")+" request sent to &e"+other+"&a."));notifyRelationRequest(player,other,war);}case ACCEPTED->player.sendMessage(CC.color(war?"&cGuild war started!":"&aGuild alliance formed!"));case DENIED->player.sendMessage(CC.color("&cGuild "+(war?"war":"ally")+" request denied."));case CANCELLED->player.sendMessage(CC.color("&cRequest cancelled."));case REMOVED->player.sendMessage(CC.color("&cGuild alliance with &e"+other+" &chas ended."));case NO_PERMISSION->player.sendMessage(CC.color("&cYou do not have permission to manage guild "+(war?"wars.":"allies.")));case CONFLICT->player.sendMessage(CC.color("&cGuilds cannot be allied and at war at the same time."));case DUPLICATE,COOLDOWN->player.sendMessage(CC.color("&cPlease wait before sending another request."));case DISABLED->player.sendMessage(CC.color("&cThat guild is not accepting requests."));case SAME_GUILD->player.sendMessage(CC.color("&cYou cannot target your own guild."));default->player.sendMessage(CC.color("&cThat guild request could not be completed."));}}

    private void notifyRelationRequest(Player sender,String other,boolean war){String targetId=guildManager.findGuildId(other);if(targetId==null)return;String senderGuild=guildManager.getGuildName(sender);for(UUID uuid:guildManager.getMembers(targetId)){Player target=Bukkit.getPlayer(uuid);if(target==null||!guildManager.hasPermission(target,war?GuildPermission.ACCEPT_WARS:GuildPermission.ACCEPT_ALLIES))continue;target.sendMessage(CC.color((war?"&cGuild War Request!":"&aGuild Ally Request!")+"\n&e"+senderGuild+" &7wants to "+(war?"start a guild war.":"become allies.")));String root="/guild "+(war?"war":"ally");TextComponent yes=new TextComponent(CC.color("&a[YES]"));yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,root+" accept "+senderGuild));TextComponent gap=new TextComponent("  ");TextComponent no=new TextComponent(CC.color("&c[NO]"));no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,root+" deny "+senderGuild));target.spigot().sendMessage(new ComponentBuilder().append(yes).append(gap).append(no).create());}}

    private void openRelations(Player player,boolean war){String guildId=guildManager.getGuildId(player);if(guildId==null){player.sendMessage(CC.color("&cYou are not in a guild."));return;}Inventory inventory=Bukkit.createInventory(new MenuHolder(war?"guild-wars":"guild-allies"),54,CC.color(war?"&8Guild Wars":"&8Guild Allies"));inventory.setItem(11,item(war?Material.IRON_SWORD:Material.TOTEM_OF_UNDYING,war?"&cActive Wars":"&aActive Allies",List.of("&7Active: &e"+(war?guildManager.getActiveWarCount(guildId):guildManager.getActiveAllyCount(guildId)),"","&eClick to view.")));inventory.setItem(13,item(Material.CHEST,"&eIncoming Requests",List.of("&7Pending incoming guild requests.","","&eClick to view.")));inventory.setItem(15,item(Material.ENDER_CHEST,"&eOutgoing Requests",List.of("&7Pending outgoing guild requests.","","&eClick to view.")));inventory.setItem(31,item(Material.BOOK,"&eHistory",List.of("&7View completed relations.","","&eClick to view.")));inventory.setItem(49,item(Material.BARRIER,"&cBack",List.of()));fill(inventory);player.openInventory(inventory);}

    private void handleRelationClick(Player player,String type,ItemStack clicked,ClickType click){boolean war=type.startsWith("guild-war");String name=ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);if(type.equals(war?"guild-wars":"guild-allies")){if(name.contains("active"))openRelationList(player,war,"active");else if(name.contains("incoming"))openRelationList(player,war,"incoming");else if(name.contains("outgoing"))openRelationList(player,war,"outgoing");else if(name.contains("history"))openRelationList(player,war,"history");return;}String other=metadata(clicked,"Other Guild: ");if(other==null)return;if(type.endsWith(":incoming"))sendRelationResult(player,click.isRightClick()?(war?guildManager.denyWar(player,other):guildManager.denyAlly(player,other)):(war?guildManager.acceptWar(player,other):guildManager.acceptAlly(player,other)),war,other);else if(type.endsWith(":outgoing"))sendRelationResult(player,war?guildManager.cancelWar(player,other):guildManager.cancelAlly(player,other),war,other);else if(!war&&type.endsWith(":active"))sendRelationResult(player,guildManager.removeAlly(player,other),false,other);openRelationList(player,war,type.substring(type.lastIndexOf(':')+1));}

    private void openRelationList(Player player,boolean war,String view){String guildId=guildManager.getGuildId(player);if(guildId==null)return;Inventory inventory=Bukkit.createInventory(new MenuHolder((war?"guild-war:":"guild-ally:")+view),54,CC.color(war?"&8Guild Wars":"&8Guild Allies"));int[] slots=contentSlots();int i=0;if(view.equals("incoming")||view.equals("outgoing")){for(GuildManager.GuildRequest request:guildManager.getRequests(guildId,war,view.equals("incoming"))){String otherId=view.equals("incoming")?request.fromGuildId():request.toGuildId(),other=guildManager.getGuildName(otherId);inventory.setItem(slots[i++],item(Material.PAPER,"&e"+other,List.of("&7"+(view.equals("incoming")?"From: ":"To: ")+"&e"+other,"&7Expires In: &c"+duration(Math.max(0,request.expiresAt()-System.currentTimeMillis())),"",view.equals("incoming")?"&aLeft Click to accept\n&cRight Click to deny":"&eClick to cancel","&8Other Guild: "+other)));if(i>=slots.length)break;}}else{for(GuildManager.GuildRelation relation:guildManager.getRelations(guildId,war,view.equals("active"))){String other=guildManager.getGuildName(relation.other(guildId));inventory.setItem(slots[i++],item(war?Material.IRON_SWORD:Material.TOTEM_OF_UNDYING,(war?"&c":"&a")+other,List.of("&7"+(war?"Enemy Guild: &c":"Allied Guild: &a")+other,"&7Started: &e"+date(relation.startedAt()),"&7Duration: &a"+duration(System.currentTimeMillis()-relation.startedAt()),"&7Status: "+relation.status(),"",(!war&&view.equals("active"))?"&cClick to remove alliance":"","&8Other Guild: "+other)));if(i>=slots.length)break;}}inventory.setItem(49,item(Material.BARRIER,"&cBack",List.of()));fill(inventory);player.openInventory(inventory);}

    private ItemStack settingItem(Material material,String name,String key,String value){return item(material,name,List.of("&7Current: &e"+(value==null||value.isBlank()?"DEFAULT":value),"","&eClick to change!","&8Guild Setting: "+key));}
    private void setMeta(ItemStack stack,String name,List<String> lore){ItemMeta meta=stack.getItemMeta();if(meta==null)return;meta.setDisplayName(CC.color(name));List<String> colored=new ArrayList<>();for(String line:lore)for(String split:line.split("\\n"))colored.add(CC.color(split));meta.setLore(colored);stack.setItemMeta(meta);}
    private String metadata(ItemStack item,String prefix){if(item==null||!item.hasItemMeta()||!item.getItemMeta().hasLore())return null;for(String line:Objects.requireNonNull(item.getItemMeta().getLore())){String clean=ChatColor.stripColor(line);if(clean!=null&&clean.startsWith(prefix))return clean.substring(prefix.length()).trim();}return null;}
    private int parsePage(String holder){try{return Integer.parseInt(holder.substring(holder.lastIndexOf(':')+1));}catch(Exception ignored){return 0;}}
    private String date(long millis){return millis<=0?"Unknown":DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis));}
    private String duration(long millis){long seconds=Math.max(0,millis/1000),days=seconds/86400,hours=(seconds%86400)/3600,minutes=(seconds%3600)/60;return days>0?days+"d "+hours+"h":hours>0?hours+"h "+minutes+"m":minutes+"m";}
    private String rankPrefix(Player player){if(player==null)return "&7Unknown";if(plugin instanceof Game game&&game.getAbelixHook()!=null)return game.getAbelixHook().getPrefix(player);return "&7";}

    private void claim(Player player) {
        GuildManager.ClaimResult result = guildManager.claim(player, player.getLocation());
        switch (result) {
            case CLAIMED -> player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.claimed", "&aClaimed this chunk for your guild.")));
            case NO_GUILD -> player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.no-guild", "&cCreate a guild first.")));
            case NOT_OWNER -> player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.not-owner", "&cOnly the guild owner can manage claims.")));
            case ALREADY_YOURS -> player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.already-yours", "&eYour guild already owns this chunk.")));
            case ALREADY_CLAIMED -> player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.already-claimed", "&cThat chunk is already claimed by another guild.")));
            case LIMIT_REACHED -> player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.limit-reached", "&cYour guild claim limit is full. Buy more claim upgrades in /store.")));
            case INVALID -> player.sendMessage(CC.color("&cCould not claim this chunk."));
        }
    }

    private void unclaim(Player player) {
        if (guildManager.unclaim(player, player.getLocation())) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.unclaimed", "&aUnclaimed this chunk.")));
        } else {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.messages.cannot-unclaim", "&cYou cannot unclaim this chunk.")));
        }
    }

    private String currentClaimOwner(Player player) {
        String owner = guildManager.getClaimOwnerName(player.getLocation());
        return owner == null ? "Wilderness" : owner;
    }

    private List<String> guildInfo(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Guild: &f" + (guildManager.hasGuild(player) ? guildManager.getGuildName(player) : "None"));
        lore.add("&7Tag: " + (guildManager.hasGuild(player) ? guildManager.getGuildTag(player) : "&fNone"));
        lore.add("&7Claims: &e" + guildManager.getClaimCount(player) + "&7/&e" + guildManager.getClaimLimit(player));
        lore.add("&7Owner Tools: " + (guildManager.isOwner(player) ? "&aUnlocked" : "&cOwner only"));
        lore.add("");
        lore.add("&7Commands:");
        lore.add("&e/guilds &7- Open this menu");
        lore.add("&e/guilds create <name> &7- Create guild");
        lore.add("&e/guilds claim &7- Claim current chunk");
        lore.add("&e/guilds unclaim &7- Unclaim current chunk");
        lore.add("&e/g list &7- List members");
        lore.add("&e/g browse &7- Browse open guilds");
        lore.add("&e/g settings &7- Open settings");
        lore.add("&e/g tag <tag> &7- Set guild tag");
        lore.add("&e/g tagcolor <color> &7- Set tag color");
        lore.add("&e/g disband confirm &7- Delete guild");
        return lore;
    }

    private String loreValue(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith("Guild ID: ")) return clean.substring("Guild ID: ".length()).trim();
        }
        return null;
    }

    private String tagColorValue(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith("Tag Color: ")) return clean.substring("Tag Color: ".length()).trim();
        }
        return null;
    }

    private String prettyColor(String color) {
        StringBuilder result = new StringBuilder();
        for (String word : color.toLowerCase(Locale.ROOT).split("_")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private int[] contentSlots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    }

    private void refreshGuildDisplays() {
        if (guildDisplayListener != null) guildDisplayListener.refreshAll();
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(CC.color(line));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
    }
}
