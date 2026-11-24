package club.abelix.api.base.stream.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.chat.MessageSender;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.json.JSON;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.client.CGetActiveStreams;
import club.abelix.network.type.stream.Stream;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StreamsGui extends Gui {

    /**
     * Иконка о том, что друзей у игрока нет...
     */
    private static final ItemStack NO_STREAMS_ICON = ItemUtil.getBuilder(Material.BUCKET)
            .setName(Lang.of("stream_gui_empty"))
            .setLore(Lang.of("stream_gui_empty_lore"))
            .build();
    private final PagedZone<ActiveStream> zone;
    /**
     * Список стримов (уже загруженных асинхронно)
     */
    private final List<ActiveStream> streams = new ArrayList<>();

    public StreamsGui(Player player) {
        super(player, Lang.of("stream_gui_title"), 5);

        this.zone = new PagedZone<>(this) {

            @Override
            protected Collection<ActiveStream> getItems() {
                return streams;
            }

            @Override
            protected void drawItem(ActiveStream stream, int slot) {
                Gamer gamer = stream.gamer();

                Language language = getLanguage();

                ItemUtil.Builder itemBuilder = ItemUtil.getBuilder(gamer.getSkull());
                itemBuilder.setName(translated("stream_gui_entry", "%name%", gamer.getRealDisplayName()));
                itemBuilder.addLore(
                        translatedLore("stream_gui_entry_lore",
                                "%title%", stream.title(),
                                "%platform%", stream.platformName(),
                                "%duration%", stream.formatStreamTime(language),
                                "%viewers%", stream.viewers()
                        )
                );

                add(itemBuilder.build(), slot, clickEvent -> {
                    player.closeInventory();

                    MessageSender.sendJsonMessage(player, String.format(JSON.CLICK_URL_LAYOUT,
                            language.getMessage("stream_click_to_go"), stream.url()));
                });
            }
        };

        //слоты для списка друзей
        this.zone.addSlotRect(1, 1, 7, 2);

        draw();
    }

    @Override
    public void draw() {
        add(DATA_IS_LOADING_ICON, toSlot(4, 2));

        loadStreamList(() -> {
            if (streams.isEmpty()) {
                add(NO_STREAMS_ICON, toSlot(4, 2));
                return;
            }

            //рисуем страницы
            this.zone.draw();
            //кнопки назад/вперед
            this.zone.drawButtons(toSlot(2, 3), toSlot(6, 3));
        });
    }

    private void loadStreamList(Runnable complete) {
        Connection.awaitPacket(new CGetActiveStreams()).whenComplete((response, cause) -> {
            for (Stream stream : response.getStreams()) {
                Gamer gamer = Gamer.getOrCreate(stream.getPlayerId(), loadedGamer -> {
                    loadedGamer.loadPlayerId();
                    loadedGamer.loadRanks();
                    loadedGamer.loadSkull();
                    loadedGamer.loadPrefixAndUpdateName();
                    Logging.IMP.info("[Stream] Success load streamer {} [id {}]", loadedGamer.getName(), loadedGamer.getPlayerId());
                });

                streams.add(new ActiveStream(
                        gamer, stream.getTitle(), stream.getPlatformName(),
                        stream.getUrl(), stream.getViewers(), stream.getStartedTime()
                ));
            }

            complete.run();
        });
    }

    private record ActiveStream(Gamer gamer, String title, String platformName, String url, int viewers,
                                long startedTime) {

        private String formatStreamTime(@NonNull Language language) {
            return Duration.durationWordsMs(language, System.currentTimeMillis() - startedTime);
        }

    }
}
