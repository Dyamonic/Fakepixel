package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.abilities.AbilityTrigger;
import club.abelix.games.fakevival.abilities.WeaponAbilityExecutor;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CustomItemListener implements Listener {
    private final JavaPlugin plugin;
    private final CustomItemManager customItemManager;
    private final Map<UUID, Long> baitMessageCooldowns = new HashMap<>();
    private final Map<UUID, PendingEdit> pendingEdits = new HashMap<>();
    private final Map<UUID, ItemStack> projectileAbilityItems = new HashMap<>();
    private final NamespacedKey projectileAbilityIdKey;
    private final NamespacedKey projectileAbilityOwnerKey;

    public CustomItemListener(JavaPlugin plugin, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.customItemManager = customItemManager;
        this.projectileAbilityIdKey = new NamespacedKey(plugin, "ability_id");
        this.projectileAbilityOwnerKey = new NamespacedKey(plugin, "ability_owner_uuid");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        // A protection plugin may cancel the block interaction. Safe item
        // abilities still get their own checks and may run without changing
        // the protected block.
        if ((action == Action.LEFT_CLICK_BLOCK) && event.isCancelled()) return;
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && customItemManager.isFishingBait(item)) {
            event.setCancelled(true);
            sendBaitMessage(player);
            return;
        }
        if (customItemManager.openFishingBag(player, item)) {
            event.setCancelled(true);
            return;
        }
        if (customItemManager.openBackpack(player, item)) {
            event.setCancelled(true);
            return;
        }
        AbilityTrigger clickTrigger = action == Action.RIGHT_CLICK_AIR
                ? (player.isSneaking() ? AbilityTrigger.SHIFT_RIGHT_CLICK : AbilityTrigger.RIGHT_CLICK_AIR)
                : action == Action.RIGHT_CLICK_BLOCK
                ? (player.isSneaking() ? AbilityTrigger.SHIFT_RIGHT_CLICK : AbilityTrigger.RIGHT_CLICK_BLOCK)
                : action == Action.LEFT_CLICK_AIR
                ? (player.isSneaking() ? AbilityTrigger.SHIFT_LEFT_CLICK : AbilityTrigger.LEFT_CLICK_AIR)
                : (player.isSneaking() ? AbilityTrigger.SHIFT_LEFT_CLICK : AbilityTrigger.LEFT_CLICK_BLOCK);
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && customItemManager.useAbility(player, item, clickTrigger)) {
            event.setCancelled(true);
            return;
        }
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && customItemManager.isInertCustomMaterial(item)) {
            event.setCancelled(true);
            player.sendMessage(CC.color("&cYou cannot place this item."));
            return;
        }
        if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
                && customItemManager.activateAbility(player, item, clickTrigger, null,
                event.getClickedBlock() == null ? null : event.getClickedBlock().getLocation(), 0.0D, null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        LivingEntity target = event.getRightClicked() instanceof LivingEntity living ? living : null;
        if (customItemManager.activateAbility(event.getPlayer(), item, AbilityTrigger.RIGHT_CLICK, target,
                event.getRightClicked().getLocation(), 0.0D, null)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (customItemManager.isInertCustomMaterial(event.getItem()) || customItemManager.isBlockedCustomConsumable(event.getItem())) {
            event.setCancelled(true);
            return;
        }
        if (!customItemManager.isFishingBait(event.getItem())) return;
        event.setCancelled(true);
        sendBaitMessage(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = attacker(event);
        if (attacker != null && event.getEntity() instanceof LivingEntity target && !WeaponAbilityExecutor.isApplyingAbilityDamage()) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            boolean allowed = !(target instanceof Player victim) || plugin instanceof club.abelix.games.fakevival.Game game
                    && game.getPveManager().canDamagePlayer(attacker, victim, event.getCause(), held);
            if (allowed) {
                if (plugin instanceof club.abelix.games.fakevival.Game game) WeaponAbilityExecutor.applyPendingOnHit(game, attacker, target);
                if (!customItemManager.activateAbility(attacker, held, AbilityTrigger.ENTITY_HIT, target, target.getLocation(), event.getFinalDamage(), event)
                        && !customItemManager.activateAbility(attacker, held, AbilityTrigger.ON_HIT, target, target.getLocation(), event.getFinalDamage(), event)) {
                    customItemManager.activateAbility(attacker, held, AbilityTrigger.HIT_ENTITY, target, target.getLocation(), event.getFinalDamage(), event);
                }
            }
        }
        if (event.getEntity() instanceof Player victim) {
            customItemManager.activateFromEquipment(victim, AbilityTrigger.TAKE_DAMAGE, attacker instanceof LivingEntity living ? living : null, victim.getLocation(), event.getFinalDamage(), event);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (event.getEntity() instanceof Player player) {
            customItemManager.activateFromEquipment(player, AbilityTrigger.TAKE_DAMAGE, null, player.getLocation(), event.getFinalDamage(), event);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            customItemManager.activateFromEquipment(killer, AbilityTrigger.KILL_ENTITY, event.getEntity(), event.getEntity().getLocation(), 0.0D, null);
            if (customItemManager.heldAbilityId(killer).equalsIgnoreCase("silent_killer")) WeaponAbilityExecutor.markSilentKiller(killer);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            customItemManager.activateFromEquipment(event.getPlayer(), AbilityTrigger.SNEAK_TOGGLE, null, event.getPlayer().getLocation(), 0.0D, null);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        ProjectileSource source = event.getEntity().getShooter();
        if (!(source instanceof Player player)) return;
        if (event.getHitEntity() instanceof Player victim && plugin instanceof club.abelix.games.fakevival.Game game
                && !game.getPveManager().canDamagePlayer(player, victim, org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE,
                player.getInventory().getItemInMainHand())) {
            projectileAbilityItems.remove(event.getEntity().getUniqueId());
            WeaponAbilityExecutor.discardProjectile(event.getEntity());
            return;
        }
        LivingEntity target = event.getHitEntity() instanceof LivingEntity living && !(living instanceof Player) ? living : null;
        if (plugin instanceof club.abelix.games.fakevival.Game game) {
            WeaponAbilityExecutor.handleProjectileImpact(game, player, event.getEntity(), target, event.getEntity().getLocation());
        }
        ItemStack sourceItem = projectileAbilityItems.remove(event.getEntity().getUniqueId());
        if (sourceItem != null) customItemManager.activateAbility(player, sourceItem, AbilityTrigger.PROJECTILE_HIT, target,
                event.getEntity().getLocation(), 0.0D, null, event.getEntity());
        else customItemManager.activateFromEquipment(player, AbilityTrigger.PROJECTILE_HIT, target, event.getEntity().getLocation(), 0.0D, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Projectile projectile)) return;
        ItemStack bow = event.getBow();
        if (bow == null || customItemManager.getItemId(bow) == null) return;
        ItemStack snapshot = bow.clone();
        snapshot.setAmount(1);
        projectileAbilityItems.put(projectile.getUniqueId(), snapshot);
        String abilityId = customItemManager.getAbilityId(snapshot);
        if (abilityId != null && !abilityId.isBlank() && !abilityId.equalsIgnoreCase("none")) {
            projectile.getPersistentDataContainer().set(projectileAbilityIdKey, PersistentDataType.STRING, abilityId);
            projectile.getPersistentDataContainer().set(projectileAbilityOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        }
        WeaponAbilityExecutor.recordBowForce(projectile, event.getForce());
        customItemManager.activateAbility(player, snapshot, AbilityTrigger.BOW_SHOOT, null, projectile.getLocation(),
                0.0D, null, projectile);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (type.equalsIgnoreCase("edititem") || type.startsWith("edititem:")) {
            handleEditItemClick(event, type);
            return;
        }
        if (type.startsWith("fishingbag-drops:")) {
            event.setCancelled(true);
            return;
        }
        if (!type.startsWith("fishingbag:")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String action = loreValue(clicked);
        if (action == null) return;
        String uuid = type.substring("fishingbag:".length());
        if (action.equalsIgnoreCase("bait")) {
            customItemManager.openFishingBagStorage(player, uuid, "bait");
        } else if (action.equalsIgnoreCase("drops")) {
            customItemManager.openFishingBagStorage(player, uuid, "drops");
        }
    }

    private void handleEditItemClick(InventoryClickEvent event, String type) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        String action = loreValue(clicked, "Edit Item Action: ");
        if ("close".equalsIgnoreCase(action)) {
            player.closeInventory();
            return;
        }
        boolean sandboxEnchantments = org.bukkit.ChatColor.stripColor(event.getView().getTitle()).startsWith("Sandbox - Enchantments");
        if ("back".equalsIgnoreCase(action)) {
            if (sandboxEnchantments) player.performCommand("sandbox"); else customItemManager.openEditor(player);
            return;
        }
        if ("save".equalsIgnoreCase(action)) {
            customItemManager.saveEditorItem(player);
            customItemManager.openEditor(player);
            return;
        }
        if ("recipes".equalsIgnoreCase(action)) {
            player.performCommand("recipes");
            return;
        }
        if (action != null) {
            customItemManager.openEditorSection(player, action);
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        String pageValue = loreValue(clicked, "Edit Item Page: ");
        if (pageValue != null) {
            if (sandboxEnchantments) customItemManager.openSandboxEnchantmentEditor(player, parsePage(pageValue));
            else customItemManager.openEnchantmentEditor(player, parsePage(pageValue));
            return;
        }

        String textField = loreValue(clicked, "Edit Item Text: ");
        if (textField != null) {
            pendingEdits.put(player.getUniqueId(), new PendingEdit(textField, "identity"));
            player.closeInventory();
            player.sendMessage(CC.color("&eEnter the new &f" + textField.replace('_', ' ') + " &ein chat, or type &ccancel&e."));
            return;
        }

        String itemType = loreValue(clicked, "Edit Item Type: ");
        if (itemType != null) {
            if (customItemManager.setItemType(held, itemType)) customItemManager.openEditorSection(player, "identity_types");
            else player.sendMessage(CC.color("&cHold a valid item first."));
            return;
        }


        String stat = loreValue(clicked, "Edit Item Stat: ");
        if (stat != null) {
            double amount = event.isShiftClick() ? 10.0D : 1.0D;
            boolean reset = event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE;
            double delta = event.isRightClick() ? -amount : amount;
            if (customItemManager.adjustStat(held, stat, delta, reset)) customItemManager.openEditorSection(player, "stats");
            else player.sendMessage(CC.color("&cCould not update that stat."));
            return;
        }

        String flag = loreValue(clicked, "Edit Item Flag: ");
        if (flag != null) {
            if (customItemManager.toggleFlag(held, flag)) customItemManager.openEditorSection(player, type.contains("economy") ? "economy" : type.contains("identity") ? "identity" : "lore_flags");
            else player.sendMessage(CC.color("&cCould not update that flag."));
            return;
        }

        String economy = loreValue(clicked, "Edit Item Economy: ");
        if (economy != null) {
            double amount = event.isShiftClick() ? 1000.0D : 100.0D;
            boolean reset = event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE;
            if (customItemManager.adjustEconomyValue(held, economy, event.isRightClick() ? -amount : amount, reset)) customItemManager.openEditorSection(player, "economy");
            else player.sendMessage(CC.color("&cCould not update that economy value."));
            return;
        }

        String description = loreValue(clicked, "Edit Item Description: ");
        if (description != null) {
            if (event.isShiftClick() && event.isRightClick()) {
                customItemManager.editDescription(held, null, true);
                customItemManager.openEditorSection(player, "lore_flags");
            } else {
                pendingEdits.put(player.getUniqueId(), new PendingEdit("description", "lore_flags"));
                player.closeInventory();
                player.sendMessage(CC.color("&eEnter a description line in chat, or type &ccancel&e."));
            }
            return;
        }

        String rarity = loreValue(clicked, "Edit Item Rarity: ");
        if (rarity != null) {
            if (customItemManager.setRarity(held, rarity)) {
                player.sendMessage(CC.color("&aSet held item rarity to &f" + rarity + "&a."));
                customItemManager.openEditorSection(player, "rarity");
            } else {
                player.sendMessage(CC.color("&cHold a valid item first."));
            }
            return;
        }

        String ability = loreValue(clicked, "Edit Item Ability: ");
        if (ability != null) {
            if (customItemManager.setAbility(held, ability)) {
                player.sendMessage(CC.color("&aSet held item ability to &f" + ability + "&a."));
                customItemManager.openEditorSection(player, "abilities");
            } else {
                player.sendMessage(CC.color("&cCould not apply that ability to the held item."));
            }
            return;
        }

        String enchant = loreValue(clicked, "Edit Item Enchant: ");
        if (enchant != null) {
            String[] parts = enchant.split(":", 3);
            CustomItemManager.EditorResult result = customItemManager.adjustEnchantment(held, parts.length > 0 ? parts[0] : "custom",
                    parts.length > 1 ? parts[1] : "", event.isLeftClick(), event.isShiftClick());
            player.sendMessage(CC.color(result.message()));
            if (!result.success()) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.6F);
            int page = type.startsWith("edititem:enchantments:") ? parsePage(type.substring("edititem:enchantments:".length())) : 0;
            if (sandboxEnchantments) customItemManager.openSandboxEnchantmentEditor(player, page);
            else customItemManager.openEnchantmentEditor(player, page);
            return;
        }

        String skill = loreValue(clicked, "Edit Item Skill Requirement: ");
        if (skill != null) {
            int required = event.isShiftClick() ? 25 : 5;
            if (event.isRightClick()) required = event.isShiftClick() ? 50 : 10;
            String key = "skill_" + skill.toLowerCase(java.util.Locale.ROOT) + "_level";
            if (customItemManager.setRequirement(held, key, String.valueOf(required))) {
                player.sendMessage(CC.color("&aSet &f" + skill + " " + required + " &arequirement on the held item."));
                customItemManager.openEditorSection(player, "requirements");
            } else {
                player.sendMessage(CC.color("&cHold a valid item first."));
            }
        }
    }

    @EventHandler
    public void onEditorChat(AsyncPlayerChatEvent event) {
        PendingEdit pending = pendingEdits.remove(event.getPlayer().getUniqueId());
        if (pending == null) return;
        event.setCancelled(true);
        String value = event.getMessage();
        pluginTask(event.getPlayer(), () -> {
            Player player = event.getPlayer();
            if (value.equalsIgnoreCase("cancel")) {
                player.sendMessage(CC.color("&cEdit cancelled."));
            } else {
                ItemStack held = player.getInventory().getItemInMainHand();
                boolean success = pending.field().equals("description")
                        ? customItemManager.editDescription(held, value, false)
                        : customItemManager.setEditorTextValue(held, pending.field(), value);
                player.sendMessage(CC.color(success ? "&aItem updated." : "&cThat value could not be applied."));
            }
            customItemManager.openEditorSection(player, pending.section());
        });
    }

    private void pluginTask(Player player, Runnable task) {
        player.getServer().getScheduler().runTask(plugin, task);
    }

    private record PendingEdit(String field, String section) {
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingEdits.remove(event.getPlayer().getUniqueId());
        baitMessageCooldowns.remove(event.getPlayer().getUniqueId());
        projectileAbilityItems.entrySet().removeIf(entry -> {
            Projectile projectile = org.bukkit.Bukkit.getEntity(entry.getKey()) instanceof Projectile value ? value : null;
            return projectile == null || event.getPlayer().equals(projectile.getShooter());
        });
        WeaponAbilityExecutor.clearPlayerState(event.getPlayer());
    }

    private int parseLevel(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private int parsePage(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (type.startsWith("backpack:")) {
            customItemManager.saveBackpack(type.substring("backpack:".length()), event.getInventory());
            return;
        }
        if (type.startsWith("fishingbag-bait:")) {
            customItemManager.saveFishingBag(type.substring("fishingbag-bait:".length()), "bait", event.getInventory());
            return;
        }
        if (type.startsWith("fishingbag-drops:")) {
            customItemManager.saveFishingBag(type.substring("fishingbag-drops:".length()), "drops", event.getInventory());
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) return;
        customItemManager.recordFishingDrop(event.getPlayer(), item.getItemStack());
    }

    private Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private void sendBaitMessage(Player player) {
        long now = System.currentTimeMillis();
        long next = baitMessageCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (next > now) return;
        baitMessageCooldowns.put(player.getUniqueId(), now + 1500L);
        player.sendMessage(CC.color("&cFishing bait can only be used with fishing rods."));
    }

    private String loreValue(ItemStack item) {
        return loreValue(item, "Fishing Bag Action: ");
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }
}
