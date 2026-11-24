package club.abelix.games.buildffa.game.item;

import club.abelix.api.base.actionitem.AbstractActionItem;
import club.abelix.api.base.actionitem.ActionItem;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Language;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.vote.VoteGui;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MelonActionItem extends AbstractActionItem {
    private final BuildFFAGame game;

    public MelonActionItem(BuildFFAGame game, Language language) {
        super(ItemUtil.getBuilder(Material.MELON)
                .setName(language.getMessage("vote_map_display_name"))
                .build());

        this.game = game;
    }

    public static ItemStack getItemStack(final @NotNull BuildFFAGame game, final @NotNull Language language) {
        return new MelonActionItem(game, language).getActionItem().getItemStack();
    }

    @Override
    public void handle(@NonNull ActionItem actionItem) {
        actionItem.setInteractHandler(interactEvent -> {
            interactEvent.setCancelled(true);

            val player = interactEvent.getPlayer();

            new VoteGui(game, player).reopen();
        });
    }
}
