package club.abelix.api.base.util.item;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.nbt.NbtApi;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import club.abelix.api.nms.nbt.tag.StringTag;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.math.MathUtil;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.material.Colorable;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import w.config.Config;

import java.lang.reflect.Field;
import java.util.*;

@UtilityClass
public class ItemUtil {

    private Field profileField;

    static {
        try {
            profileField = Class.forName(MinecraftReflection.getCraftBukkitPackage().concat(".inventory.CraftMetaSkull"))
                    .getDeclaredField("profile");
            profileField.setAccessible(true);
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            profileField = null;
        }
    }

    public void setProfile(@NonNull SkullMeta skullMeta, @NonNull GameProfile gameProfile) {
        try {
            profileField.set(skullMeta, gameProfile);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private GameProfile getProfile(@NonNull SkullMeta skullMeta) {
        try {
            return (GameProfile) profileField.get(skullMeta);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public ItemStack getSkull(@NonNull Skin skin) {
        return getSkull(skin.getValue());
    }

    public ItemStack getSkull(@NonNull String value) {
        return ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability((short) 3)
                .setSkullValue(value)
                .setAmount(1)
                .build();
    }

    public boolean equals(ItemStack first, ItemStack second, EqualFlag... flagArray) {
        if (first == second) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        if (first.getType() != second.getType()) {
            return false;
        }

        NbtApi nbtApi = FalconAPI.getNmsApi().getNbtApi();

        CompoundTag firstTag = nbtApi.getItemFullTag(first);
        CompoundTag secondTag = nbtApi.getItemFullTag(second);

        for (EqualFlag flag : flagArray) {
            if (!flag.processTag(firstTag, secondTag)) {
                return false;
            }
        }

        return firstTag.equals(secondTag);
    }

    public boolean isInventoryFull(@NonNull Inventory inventory, @NonNull Material material) {
        return isInventoryFull(inventory, material, 1);
    }

    public boolean isInventoryFull(@NonNull Inventory inventory, @NonNull Material material, int amount) {
        return isInventoryFull(inventory, material, amount, (short) 0);
    }

    public boolean isInventoryFull(@NonNull Inventory inventory, @NonNull Material material,
                                   int amount, short damage) {
        int freeItems = 0;

        for (ItemStack slot : inventory.getContents()) {
            if (slot == null) {
                freeItems += material.getMaxStackSize();
            } else if (slot.getType() == material && slot.getDurability() == damage) {
                int i = material.getMaxStackSize() - slot.getAmount();

                // Вдруг каким-то образом в слоте оказалось больше, чем максимум в стаке
                // то мы не должны добавлять туда
                if (i > 0) {
                    freeItems += i;
                }
            } else continue;

            // Места больше или равно, чем нужно
            // значит инвентарь не заполнен
            if (freeItems >= amount) {
                return false;
            }
        }

        return true;
    }

    public boolean isInventoryFull(@NonNull Inventory inventory, @NonNull ItemStack item) {
        int i = inventory.firstEmpty();

        if (i == -1) {
            int available = 0;

            for (ItemStack inventoryItem : inventory.getContents()) {
                if (inventoryItem == null
                        || inventoryItem.getAmount() == inventoryItem.getMaxStackSize()
                        || !ItemUtil.equals(inventoryItem, item, EqualFlag.IGNORE_AMOUNT)) {
                    continue;
                }

                available += inventoryItem.getMaxStackSize() - inventoryItem.getAmount();

                if (available >= item.getAmount()) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    @Deprecated
    public boolean isInventoryFull(@NonNull Player player, @NonNull ItemStack item) {
        return isInventoryFull(player.getInventory(), item);
    }

    public void addItem(@NonNull Gamer gamer, @NonNull ItemStack... items) {
        addItem(false, gamer, items);
    }

    public void addItem(boolean silent, @NonNull Gamer gamer, @NonNull ItemStack... items) {
        Player player = gamer.getPlayer();

        if (player == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();

        Map<Integer, ItemStack> itemMap = inventory.addItem(items);

        if (itemMap.isEmpty()) {
            return;
        }

        if (!silent) {
            gamer.sendLocalizedMessage("some_items_dropped", "%items%", itemMap.size());
        }

        itemMap.values().forEach(value -> player.getWorld().dropItem(player.getLocation(), value));
    }

    public int search(@NonNull Inventory inventory, @NonNull Material material) {
        int items = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() != material) {
                continue;
            }

            items += item.getAmount();
        }

        return items;
    }

    public int fastSearch(@NonNull Player player, @NonNull Material material) {
        return search(player.getInventory(), material);
    }

    public void depositItem(@NonNull Inventory inventory, @NonNull Material material) {
        depositItem(inventory, material, 1);
    }

    public void depositItem(@NonNull Inventory inventory, @NonNull Material material, int amount) {
        depositItem(inventory, material, amount, (short) 0);
    }

    public void depositItem(@NonNull Inventory inventory, @NonNull Material material, int amount, short damage) {
        if (amount <= 0) {
            return;
        }

        int i = 0;
        ItemStack[] items = new ItemStack[MathUtil.ceil((double) amount / material.getMaxStackSize())];

        while (amount > 0) {
            items[i++] = new ItemStack(material, amount, damage);
            amount -= material.getMaxStackSize();
        }

        inventory.addItem(items);
    }

    /**
     * Проверить, есть ли в инвентаре определённый предмет. Работает гораздо
     * быстрее, чем {@link #hasInventoryItem(Inventory, ItemStack, int)}, потому что здесь не сравнивается нбт тэг
     *
     * @param inventory Инвентарь
     * @param material  Материал, который ищется в инвентаре
     * @param amount    Количество, которое должно быть в инвентаре, может быть в разных слотах и быть больше 64
     * @param damage    Дурабилити, чтобы допустим андезит не считался камнем
     * @return Если нужное количество найдено, то вернёт <code>true</code>, в противоположном случае <code>false</code>
     * @see #hasInventoryItem(Inventory, ItemStack, int)
     * @see #hasInventoryItem(Inventory, ItemStack)
     * @see #hasInventoryItem(Inventory, Material, int)
     * @see #hasInventoryItem(Inventory, Material)
     */
    public boolean hasInventoryItem(@NonNull Inventory inventory, @NonNull Material material,
                                    int amount, short damage) {
        if (amount <= 0) {
            return true;
        }

        int found = 0;

        for (ItemStack slot : inventory.getContents()) {
            if (slot == null || slot.getType() != material || slot.getDurability() != damage) {
                continue;
            }

            found += slot.getAmount();

            if (found >= amount) {
                return true;
            }
        }

        return false;
    }

    public boolean hasInventoryItem(@NonNull Inventory inventory, @NonNull Material material, int amount) {
        return hasInventoryItem(inventory, material, amount, (short) 0);
    }

    public boolean hasInventoryItem(@NonNull Inventory inventory, @NonNull Material material) {
        return hasInventoryItem(inventory, material, 1);
    }

    public boolean hasInventoryItem(@NonNull Inventory inventory, @NonNull ItemStack item) {
        return hasInventoryItem(inventory, item, item.getAmount());
    }

    public boolean hasInventoryItem(@NonNull Inventory inventory, @NonNull ItemStack item, int amount) {
        int found = 0;

        for (ItemStack slot : inventory.getContents()) {
            if (slot == null || !ItemUtil.equals(item, slot, EqualFlag.IGNORE_AMOUNT)) {
                continue;
            }

            found += slot.getAmount();

            if (found >= amount) {
                return true;
            }
        }

        return false;
    }

    // поиск "паков" предметов
    // т.е нужно найти STONE x8, каждые 8 камня - пак
    // если у игрока 9 камня, то у него в инвентаре 1 пак
    // или же если у него 87 камня, то у него 10 паков в инвентаре
    public int packSearch(@NonNull Player player, @NonNull ItemStack targetItem) {
        return (int) Math.floor((double) search(player, targetItem) / targetItem.getAmount());
    }

    public int search(@NonNull Player player, @NonNull ItemStack targetItem) {
        int items = 0;

        Inventory inv = player.getInventory();

        for (ItemStack item : inv.getContents()) {
            if (item == null || !ItemUtil.equals(item, targetItem, EqualFlag.IGNORE_AMOUNT)) {
                continue;
            }

            items += item.getAmount();
        }

        return items;
    }

    public void withdraw(@NonNull Inventory inventory, @NonNull ItemStack targetItem, int amount) {
        ItemStack[] items = inventory.getContents();

        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];

            if (item == null || !ItemUtil.equals(targetItem, item, EqualFlag.IGNORE_AMOUNT)) continue;

            if (item.getAmount() <= amount) {
                items[i] = null;

                amount -= item.getAmount();
            } else {
                item.setAmount(item.getAmount() - amount);

                amount = 0;
            }

            if (amount <= 0) {
                break;
            }
        }

        inventory.setContents(items);
    }

    public void fastWithdraw(@NonNull Inventory inventory, @NonNull Material type) {
        fastWithdraw(inventory, type, 1);
    }

    public void fastWithdraw(@NonNull Inventory inventory, @NonNull Material type, int amount) {
        withdrawItem(inventory, type, amount, (short) 0);
    }

    public void withdrawItem(@NonNull Inventory inventory, @NonNull Material type, int amount, short damage) {
        if (amount <= 0) {
            return;
        }

        ItemStack[] items = inventory.getContents();

        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];

            if (item == null || type != item.getType() || damage != item.getDurability()) continue;

            if (item.getAmount() <= amount) {
                items[i] = null;

                amount -= item.getAmount();
            } else {
                item.setAmount(item.getAmount() - amount);

                amount = 0;
            }

            if (amount <= 0) {
                break;
            }
        }

        inventory.setContents(items);
    }

    @Deprecated
    public void fastWithdraw(@NonNull Inventory inventory, @NonNull Material type, int amount, short damage) {
        withdrawItem(inventory, type, amount, damage);
    }

    public ItemStack parseItem(ConfigurationSection section) {
        String id = section.getString("id", "");
        Material type = Material.matchMaterial(Objects.requireNonNull(id));

        if (type == null) {
            System.out.println("Material \"" + id + "\" not found at " + section.getCurrentPath());

            type = Material.STONE;
        }

        ItemStack itemStack = new ItemStack(type, section.getInt("amount", 1), (short) section.getInt("durability", 0));

        ItemMeta itemMeta = itemStack.getItemMeta();

        String displayName = section.getString("name");
        List<String> lore = section.getStringList("lore");

        if (displayName != null) {
            itemMeta.setDisplayName(displayName);
        }

        if (lore != null) {
            itemMeta.setLore(lore);
        }

        ConfigurationSection enchants = section.getConfigurationSection("enchants");

        if (enchants != null) {
            for (String enchantment : enchants.getKeys(false)) {
                Enchantment bukkitEnchantment = Enchantment.getByName(enchantment.toUpperCase());

                if (bukkitEnchantment == null) {
                    continue;
                }

                int level = enchants.getInt(enchantment);

                if (itemMeta instanceof EnchantmentStorageMeta) {
                    EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) itemMeta;
                    enchantmentStorageMeta.addStoredEnchant(bukkitEnchantment, level, true);
                } else {
                    itemMeta.addEnchant(bukkitEnchantment, level, true);
                }
            }
        }

        for (String flagName : section.getStringList("flags")) {
            ItemFlag itemFlag = ItemFlag.valueOf(flagName.toUpperCase());
            itemMeta.addItemFlags(itemFlag);
        }

        //itemMeta.spigot().setUnbreakable(section.getBoolean("unbreakable", false));

        if (itemMeta instanceof PotionMeta) {
            ConfigurationSection potionSection = section.getConfigurationSection("effects");

            if (potionSection != null) {
                for (String effect : potionSection.getKeys(false)) {
                    PotionEffectType effectType = PotionEffectType.getByName(effect);

                    if (effectType == null) {
                        continue;
                    }

                    int duration = potionSection.getInt(effect.concat(".duration"), 0) * 20;
                    int amplifier = potionSection.getInt(effect.concat(".amplifier"), 0);

                    boolean ambient = potionSection.getBoolean(effect.concat(".ambient"));
                    boolean particles = potionSection.getBoolean(effect.concat(".particles"), true);

                    PotionEffect potionEffect
                            = new PotionEffect(effectType, duration, amplifier, ambient, particles);

                    ((PotionMeta) itemMeta).addCustomEffect(potionEffect, true);
                }
            }

            ConfigurationSection baseEffectSection = section.getConfigurationSection("base_effect");

            if (baseEffectSection != null) {
                String effectType = baseEffectSection.getString("type");

                boolean upgraded = baseEffectSection.getBoolean("upgraded", false);
                boolean extended = baseEffectSection.getBoolean("extended", false);
                boolean splash = baseEffectSection.getBoolean("splash", false);

                Potion potion = new Potion(PotionType.valueOf(effectType));
                if (extended) potion.setHasExtendedDuration(true);
                potion.setLevel(upgraded ? 2 : 1);
                potion.setSplash(splash);

                ItemStack potionStack = potion.toItemStack(1);

                PotionMeta meta = (PotionMeta) potionStack.getItemMeta();
                itemStack.setDurability(potionStack.getDurability());
                ((PotionMeta) itemMeta).setMainEffect(potion.getType().getEffectType());

                for (PotionEffect effect : meta.getCustomEffects()) {
                    ((PotionMeta) itemMeta).addCustomEffect(effect, true);
                }
            }
        }

        if (section.contains("color")) {
            Color color = getColor(section);

            if (itemMeta instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) itemMeta).setColor(color);
            }
        }

        itemStack.setItemMeta(itemMeta);

        if (section.contains("monsterType")) {
            EntityType monsterType = EntityType.valueOf(section.getString("monsterType"));

            FalconAPI.getNmsApi().getWorldApi().modifySpawnerType(itemStack, monsterType);
        }

        return itemStack;
    }

    public ItemStack parseItem(Config config) {
        String id = config.getString("id", "");
        Material type = Material.matchMaterial(Objects.requireNonNull(id));

        if (type == null) {
            System.out.println("Material \"" + id + "\" was not found");

            type = Material.STONE;
        }

        ItemStack itemStack = new ItemStack(type, config.getInt("amount", 1), (short) config.getInt("durability", 0));

        ItemMeta itemMeta = itemStack.getItemMeta();

        String displayName = config.findString("name").orElse(null);
        List<String> lore = config.getStringList("lore");

        if (displayName != null) {
            itemMeta.setDisplayName(displayName);
        }

        if (!lore.isEmpty()) {
            itemMeta.setLore(lore);
        }

        config.findObject("enchants").ifPresent(enchants -> {
            val enchantments = enchants.keySet();

            for (val enchantment : enchantments) {
                Enchantment bukkitEnchantment = Enchantment.getByName(enchantment.toUpperCase());

                if (bukkitEnchantment == null) {
                    continue;
                }

                int level = enchants.getInt(enchantment);

                if (itemMeta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
                    enchantmentStorageMeta.addStoredEnchant(bukkitEnchantment, level, true);
                } else {
                    itemMeta.addEnchant(bukkitEnchantment, level, true);
                }
            }
        });

        for (val flagName : config.getStringList("flags")) {
            val itemFlag = ItemFlag.valueOf(flagName.toUpperCase());
            itemMeta.addItemFlags(itemFlag);
        }

        itemMeta.spigot().setUnbreakable(config.getBoolean("unbreakable", false));

        if (itemMeta instanceof PotionMeta potionMeta) {
            config.findObject("effects").ifPresent(effectsSection -> {
                val effects = effectsSection.keySet();

                for (val effect : effects) {
                    val effectSection = effectsSection.getObject(effect);
                    val effectType = PotionEffectType.getByName(effect);

                    if (effectType == null) {
                        continue;
                    }

                    val duration = effectSection.getInt("duration", 0) * 20;
                    val amplifier = effectSection.getInt("amplifier", 0);

                    val ambient = effectSection.getBoolean("ambient", false);
                    val particles = effectSection.getBoolean("particles", true);

                    val potionEffect = new PotionEffect(effectType, duration, amplifier, ambient, particles);
                    potionMeta.addCustomEffect(potionEffect, true);
                }
            });

            config.findObject("base_effect").ifPresent(baseEffectSection -> {
                val effectType = baseEffectSection.getString("type");

                val upgraded = baseEffectSection.getBoolean("upgraded", false);
                val extended = baseEffectSection.getBoolean("extended", false);
                val splash = baseEffectSection.getBoolean("splash", false);

                val potion = new Potion(PotionType.valueOf(effectType));

                if (extended) {
                    potion.setHasExtendedDuration(true);
                }

                potion.setLevel(upgraded ? 2 : 1);
                potion.setSplash(splash);

                val potionStack = potion.toItemStack(1);

                val meta = (PotionMeta) potionStack.getItemMeta();
                itemStack.setDurability(potionStack.getDurability());

                ((PotionMeta) itemMeta).setMainEffect(potion.getType().getEffectType());

                for (PotionEffect effect : meta.getCustomEffects()) {
                    ((PotionMeta) itemMeta).addCustomEffect(effect, true);
                }
            });
        }

        config.findObject("color").ifPresent(colorSection -> {
            val color = getColor(colorSection);

            if (itemMeta instanceof LeatherArmorMeta meta) {
                meta.setColor(color);
            }
        });

        itemStack.setItemMeta(itemMeta);

        config.findString("monsterType").ifPresent(monsterTypeString -> {
            val monsterType = EntityType.valueOf(monsterTypeString);
            FalconAPI.getNmsApi().getWorldApi().modifySpawnerType(itemStack, monsterType);
        });

        return itemStack;
    }


    public Color getColor(ConfigurationSection section) {
        return Color.fromRGB((section.getInt("potionColor") & 16777215));
    }

    public Color getColor(Config section) {
        return Color.fromRGB((section.getInt("potionColor") & 16777215));
    }

    public Builder getBuilder(ItemStack item) {
        return new Builder(item.clone());
    }

    public Builder getBuilder(Material material) {
        return new Builder(material);
    }

    public Builder getSkullBuilder(@NonNull String value) {
        return ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setSkullValue(value);
    }

    public enum EqualFlag {
        IGNORE_DURABILITY {
            @Override
            public boolean processTag(CompoundTag tag, CompoundTag another) {
                tag.removeTag("Damage");
                another.removeTag("Damage");

                return true;
            }
        },
        IGNORE_AMOUNT {
            @Override
            public boolean processTag(CompoundTag tag, CompoundTag another) {
                tag.removeTag("Count");
                another.removeTag("Count");

                return true;
            }
        },
        IGNORE_CASE {
            @Override
            public boolean processTag(CompoundTag tag, CompoundTag another) {
                CompoundTag displayTag = tag.getCompound("display");
                CompoundTag anotherDisplayTag = another.getCompound("display");

                int i = 0;

                if (displayTag != null) {
                    StringTag nameTag = displayTag.getTag("Name", StringTag.class);
                    nameTag.setValue(nameTag.getValue().toLowerCase());

                    i++;
                }

                if (anotherDisplayTag != null) {
                    StringTag nameTag = anotherDisplayTag.getTag("Name", StringTag.class);
                    nameTag.setValue(nameTag.getValue().toLowerCase());

                    i++;
                }

                return i == 0 || i == 2; // оба null или оба не null
            }
        };

        public abstract boolean processTag(@NonNull CompoundTag tag, @NonNull CompoundTag another);
    }

    @RequiredArgsConstructor
    public static class Builder {

        private final ItemStack itemStack;

        private Builder(Material material) {
            this(new ItemStack(material, 1));
        }

        public Builder setEntityType(EntityType type) {
            FalconAPI.getNmsApi().getPlayerApi().setSpawnEggType(itemStack, type);
            return this;
        }

        public Builder setPotionData(PotionEffectType type, int amplifier, int duration) {
            ItemMeta meta = itemStack.getItemMeta();

            if (meta instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) meta;
                pm.addCustomEffect(new PotionEffect(type, duration, amplifier, false, false), true);

                PotionType potionType = PotionType.getByEffect(type);

                if (potionType == null) {
                    potionType = PotionType.WEAKNESS;
                }

                itemStack.setItemMeta(meta);
                itemStack.setDurability((short) potionType.getDamageValue());
            }
            return this;
        }

        public Builder setPotionData(PotionType type) {
            return setPotionData(type, false, false);
        }

        public Builder setPotionData(PotionType type, int amplifier, int duration) {
            ItemMeta meta = itemStack.getItemMeta();

            if (meta instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) meta;
                pm.addCustomEffect(new PotionEffect(type.getEffectType(), duration, amplifier, false, false), true);
                itemStack.setItemMeta(meta);
                itemStack.setDurability((short) type.getDamageValue());
            }

            return this;
        }

        public Builder setPotionData(PotionType type, boolean extended, boolean upgraded) {
            ItemMeta meta = itemStack.getItemMeta();

            if (meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta) meta;

                Potion potion = new Potion(type);

                if (extended) {
                    potion.setHasExtendedDuration(true);
                }

                potion.setLevel(upgraded ? 2 : 1);

                ItemMeta itemMeta = potion.toItemStack(1).getItemMeta();

                for (PotionEffect effect : ((PotionMeta) itemMeta).getCustomEffects()) {
                    potionMeta.addCustomEffect(effect, true);
                }

                itemStack.setItemMeta(meta);
            }

            return this;
        }

        public Builder setPotionSplash() {
            ItemMeta meta = itemStack.getItemMeta();

            if (meta instanceof PotionMeta) {
                itemStack.setDurability((short) (itemStack.getDurability() | 16384));
            }

            return this;
        }

        public Builder setType(Material material) {
            itemStack.setType(material);

            return this;
        }

        public Builder setDurability(int durability) {
            itemStack.setDurability((short) durability);

            return this;
        }

        public Builder setAmount(int amount) {
            itemStack.setAmount(amount);
            return this;
        }

        public Builder addItemFlags() {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.addItemFlags(ItemFlag.values());

                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder removeEnchantment() {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.getEnchants().keySet().forEach(itemMeta::removeEnchant);
                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder setUnbreakable() {
            return setUnbreakable(true);
        }

        public Builder setUnbreakable(boolean unbreakable) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.spigot().setUnbreakable(unbreakable);
                itemStack.setItemMeta(itemMeta);
            }

            addFlag(ItemFlag.HIDE_UNBREAKABLE);
            return this;
        }

        public Builder glowing() {
            addFlag(ItemFlag.HIDE_ENCHANTS);
            this.itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 1);
            return this;
        }

        public Builder glowing(boolean glow) {
            if (!glow) {
                itemStack.getEnchantments().forEach((enchant, level) -> itemStack.removeEnchantment(enchant));
                return this;
            }

            addFlag(ItemFlag.HIDE_ENCHANTS);
            itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 1);

            return this;
        }

        public Builder addEnchantment(Enchantment type, int level) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta instanceof EnchantmentStorageMeta) {
                ((EnchantmentStorageMeta) itemMeta).addStoredEnchant(type, level, true);
            } else {
                itemMeta.addEnchant(type, level, true);
            }

            itemStack.setItemMeta(itemMeta);

            return this;
        }

        public Builder setName(Lang lang) {
            return setName(lang.compile());
        }

        public Builder setName(String name) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.setDisplayName(name);
                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public String getDisplayName() {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null && itemMeta.getDisplayName() != null) {
                return itemMeta.getDisplayName();
            }

            return "";
        }

        public Builder setLore(Lang lang) {
            return setLore(lang.compile());
        }

        public Builder setLore(String... lore) {
            return setLore(Arrays.asList(lore));
        }

        public Builder setLore(List<String> lore) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.setLore(lore);
                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder addLore(Lang lang) {
            return addLore(lang.compile());
        }

        public Builder addLore(String... lore) {
            return addLore(Arrays.asList(lore));
        }

        public Builder addLore(List<String> lore) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                List<String> oldLore = itemMeta.getLore();

                if (oldLore != null) {
                    oldLore.addAll(lore);
                } else {
                    oldLore = lore;
                }

                itemMeta.setLore(oldLore);
                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder addLore(int index, String... lore) {
            return addLore(index, Arrays.asList(lore));
        }

        public Builder addLore(int index, List<String> lore) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                List<String> oldLore = itemMeta.getLore();

                if (oldLore == null) {
                    oldLore = new ArrayList<>();
                }

                oldLore.addAll(index, lore);

                itemMeta.setLore(oldLore);

                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder addFlag(ItemFlag... flag) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.addItemFlags(flag);
                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder addItemFlagsExcept(@NonNull ItemFlag... except) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.addItemFlags(ItemFlag.values());
                itemMeta.removeItemFlags(except);

                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder addFlag(ItemFlag flag) {
            return addFlag(new ItemFlag[]{flag});
        }

        public Builder setSkullOwner(@NonNull String ownerName) {
            if (itemStack.getItemMeta() instanceof SkullMeta) {
                SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

                skullMeta.setOwner(ownerName);
            }

            return this;
        }

        public Builder setSkullValue(@NonNull String value) {
            if (itemStack.getItemMeta() instanceof SkullMeta) {
                UUID uuid = UUID.randomUUID();

                GameProfile profile = new GameProfile(uuid, "AbelixProfile");
                profile.getProperties().put("textures", new Property("textures", value));
                profile.getProperties().put("skinDigest", new Property("skinDigest", uuid.toString()));

                SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
                setProfile(skullMeta, profile);

                itemStack.setItemMeta(skullMeta);
            }

            return this;
        }

        public Builder setSkullSignature(@NonNull String signature) {
            return this.setSkullUrl("https://textures.minecraft.net/texture/" + signature);
        }

        public Builder setSkullUrl(@NonNull String url) {
            if (itemStack.getItemMeta() instanceof SkullMeta) {
                UUID uuid = UUID.randomUUID();
                GameProfile profile = new GameProfile(uuid, "FpSkin");

                byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());

                profile.getProperties().put("skinURL", new Property("skinURL", url));
                profile.getProperties().put("skinDigest", new Property("skinDigest", uuid.toString()));
                profile.getProperties().put("textures", new Property("textures", new String(encodedData)));

                SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
                setProfile(skullMeta, profile);


                itemStack.setItemMeta(skullMeta);
            }

            return this;
        }


        public Builder setColor(DyeColor color) {
            if (itemStack.getItemMeta() instanceof Colorable) {
                ItemMeta itemMeta = itemStack.getItemMeta();
                ((Colorable) itemMeta).setColor(color);

                itemStack.setItemMeta(itemMeta);
            }

            return this;
        }

        public Builder setColor(Color color) {
            if (itemStack.getItemMeta() instanceof LeatherArmorMeta) {
                LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemStack.getItemMeta();
                leatherArmorMeta.setColor(color);

                itemStack.setItemMeta(leatherArmorMeta);
            }

            return this;
        }

        public ItemStack build() {
            return itemStack;
        }
    }

}
