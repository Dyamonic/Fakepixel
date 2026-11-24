package club.abelix.api.base.joinmessage.custom;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.joinmessage.JoinMessageSqlHandler;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.both.BAddServerJoinMessage;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JoinMessageCreateGui extends Gui {

    private final String text;

    public JoinMessageCreateGui(@NonNull Player player, @NonNull String text) {
        super(player, Lang.of("joinmessage_create_title"), 3);

        this.text = text;

        draw();
    }

    @Override
    public void draw() {
        ItemStack messageItem = ItemUtil.getBuilder(Material.BOOK_AND_QUILL)
                .setName(language.getMessage("joinmessage_create_name"))
                .setLore(language.getMessageList("joinmessage_create_lore",
                        "%text%", text))
                .build();

        add(messageItem, toSlot(2, 1), clickEvent -> {
            int messageCreations = gamer.getProperty("JoinMessage/Creations");

            if (messageCreations <= 0) {
                gamer.sendLocalizedMessage("joinmessage_custom_purchase");
                return;
            }

            player.closeInventory();

            JoinMessageSqlHandler.INSTANCE.createJoinMessage("custom_join_message", text, 0, false, true)
                    .thenAccept(message -> {
                        gamer.sendLocalizedMessage("joinmessage_custom_created",
                                "%player%", gamer.getDisplayNameAndSuffix(), "%text%", text);

                        // добавляем сообщение игроку
                        JoinMessageSqlHandler.INSTANCE
                                .addPlayerJoinMessage(gamer.getPlayerId(), message);

                        // отправляем на кор новое сообщение (чтобы тот отправил на все сервера)
                        Connection.sendPacket(new BAddServerJoinMessage(
                                message.getId(), message.getName(), text, 0, false, true)
                        );

                        // устанавливаем созданное сообщение как активное у игрока
                        gamer.changeActiveJoinMessage(message);

                        // убираем 1 возможность создания собственного сообщения
                        gamer.setProperty("JoinMessage/Creations", messageCreations - 1);
                    });
        });
    }
}
