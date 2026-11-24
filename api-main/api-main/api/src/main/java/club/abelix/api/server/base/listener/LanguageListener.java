package club.abelix.api.server.base.listener;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerLanguageChangeEvent;
import club.abelix.api.base.util.nms.locale.LocaleUtil;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.commons.language.Language;
import club.abelix.commons.language.parser.I18nParserController;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.comphenix.protocol.PacketType.Play.Server.*;

public class LanguageListener extends PacketAdapter implements Listener {

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();
    private final I18nParserController parser = I18nParserController.getInstance();

    public LanguageListener(@NonNull FalconAPI plugin) {
        super(plugin, SET_SLOT, WINDOW_ITEMS, ENTITY_METADATA, CHAT, BOSS, OPEN_WINDOW, TITLE, SCOREBOARD_OBJECTIVE);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer container = event.getPacket();
        PacketType type = event.getPacketType();
        Gamer gamer = Gamer.getGamer(event.getPlayer());
        if (gamer == null) {
            return;
        }
        Language language = gamer.getLanguage();

        if (type == SET_SLOT) {
            container.getItemModifier().write(0, processItemStack(language, container.getItemModifier().read(0)));
        } else if (type == WINDOW_ITEMS) {
            var itemArray = container.getItemArrayModifier();

            if (itemArray.getFields().size() == 0) {
                var itemList = container.getItemListModifier();

                itemList.write(0, itemList.read(0).stream().map(itemStack -> processItemStack(language, itemStack)).toList());
            } else {
                itemArray.write(0, Arrays.stream(itemArray.read(0)).map(itemStack -> processItemStack(language, itemStack)).toArray(ItemStack[]::new));
            }
        } else if (type == ENTITY_METADATA) {
            var collection = container.getWatchableCollectionModifier();
            var objects = collection.read(0);

            objects.removeIf(object -> object.getRawValue() == null);

            var dataWatcher = new WrappedDataWatcher(objects);
            if (processCustomName(language, dataWatcher)) {
                collection.write(0, dataWatcher.getWatchableObjects());
            }
        } else if (type == SPAWN_ENTITY_LIVING) {
            var dataWatcherModifier = container.getDataWatcherModifier();
            var watcher = dataWatcherModifier.read(0);

            if (processCustomName(language, watcher)) {
                dataWatcherModifier.write(0, watcher);
            }
        } else if (type == CHAT || type == OPEN_WINDOW || type == TITLE || type == BOSS) {
            if (type == CHAT && (container.getChatTypes().read(0) == EnumWrappers.ChatType.CHAT || container.getChatTypes().read(0) == EnumWrappers.ChatType.SYSTEM)) {
                return;
            }
            var components = container.getChatComponents();
            var component = components.read(0);
            if (component != null) {
                component = component.deepClone();
                component.setJson(parser.parse(language, component.getJson()));
                components.write(0, component);
            }
        }
    }

    private boolean processCustomName(@NonNull Language language,
                                      @NonNull WrappedDataWatcher dataWatcher) {

        String customName = fakeEntityApi.getEntityCustomName(dataWatcher);

        if (customName != null) {
            String newCustomName = parser.parse(language, customName);

            fakeEntityApi.setEntityCustomName(dataWatcher, newCustomName);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onLanguageChange(GamerLanguageChangeEvent event) {
        Player player = event.getGamer().getPlayer();
        player.updateInventory();

        for (FakeEntity fakeEntity : fakeEntityApi.getTrackedEntities()) {
            if (!fakeEntity.hasViewer(player)) {
                continue;
            }

            String customName = fakeEntity.getCustomName();

            if (customName != null && customName.startsWith("lang:")) {
                fakeEntity.update(player);
            }
        }
    }

    private ItemStack processItemStack(@NonNull Language language, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return item;
        }

        try {
            ItemStack newItem = item.clone();

            parseLanguageItemStack(language, newItem);
            processPotionItemStack(language, newItem);

            return newItem;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Error occurred until processing item stack: " + t);
        }

        return item;
    }

    private void processPotionItemStack(@NonNull Language language, @NonNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (!(itemMeta instanceof PotionMeta)) {
            return;
        }

        PotionMeta potionMeta = (PotionMeta) itemMeta;
        potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

        List<PotionEffect> effects = potionMeta.getCustomEffects();

        if (!potionMeta.hasDisplayName()) {
            String name = "§f" + LocaleUtil.translate(language, itemStack);

            if (effects.size() == 1) {
                PotionEffect effect = effects.get(0);

                name += ": " + LocaleUtil.translateWithAmplifierAndDuration(
                        language,
                        effect.getType(),
                        effect.getAmplifier() + 1,
                        effect.getDuration()
                );
            }

            potionMeta.setDisplayName(name);
        }

        // FIXME
        // Кидает NullPointerException

        List<String> lore = new ArrayList<>();
        if (potionMeta.hasLore())
            lore.addAll(potionMeta.getLore());

        for (PotionEffect effect : effects) {
            String effectName = LocaleUtil.translateWithAmplifierAndDuration(
                    language,
                    effect.getType(),
                    effect.getAmplifier() + 1,
                    effect.getDuration()
            );

            lore.add(0, "§7" + effectName);
        }
        potionMeta.setLore(lore);

        itemStack.setItemMeta(potionMeta);
    }

    private void parseLanguageItemStack(@NonNull Language language, @NonNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) {
            return;
        }

        if (itemMeta.hasDisplayName()) {
            itemMeta.setDisplayName(parser.parse(language, itemMeta.getDisplayName()));
        }

        if (itemMeta.hasLore()) {
            List<String> newLore = new ArrayList<>();

            for (String loreLine : itemMeta.getLore()) {
                newLore.addAll(Arrays.asList(parser.parse(language, loreLine).split("\n")));
            }

            itemMeta.setLore(newLore);
        }

        itemStack.setItemMeta(itemMeta);
    }
}

