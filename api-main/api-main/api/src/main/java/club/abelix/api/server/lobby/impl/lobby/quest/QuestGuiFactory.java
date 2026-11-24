package club.abelix.api.server.lobby.impl.lobby.quest;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.commons.data.quest.Quest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface QuestGuiFactory {

    void createQuestsGui(
            @NotNull Gamer gamer,
            @NotNull List<@NotNull Quest> quests,
            @Nullable Gui prev
    );

}
