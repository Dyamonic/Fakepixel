package club.abelix.api.server.lobby.mysterybox.anim;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.song.model.FadeType;
import club.abelix.api.base.song.model.RepeatMode;
import club.abelix.api.base.song.model.Song;
import club.abelix.api.base.song.songplayer.Fade;
import club.abelix.api.base.song.songplayer.PositionSongPlayer;
import club.abelix.api.base.song.songplayer.SongPlayer;
import club.abelix.api.base.song.utils.NBSDecoder;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.server.lobby.mysterybox.MysteryBox;
import club.abelix.api.server.lobby.mysterybox.content.CaseContent;
import club.abelix.api.server.lobby.mysterybox.item.CaseManager;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractCaseOpener {

    /**
     * Количество предметов из одного кейса
     */
    private static final int ITEMS_PER_ONE_CASE = 8;
    private static Song caseOpenerSong;

    static {
        File songFile = new File(Bukkit.getWorldContainer(), "case_opener.nbs");

        if (songFile.exists()) {
            caseOpenerSong = NBSDecoder.parse(songFile);
        }
    }

    protected final AbstractPlugin plugin;
    protected final Gamer gamer;
    protected final Block block;
    protected final CaseType type;
    final List<CaseContent> rewardList = new ArrayList<>();
    private final MysteryBox mysteryBox;
    // выпавшая вещь определяется заранее. Нужно для некоторых типов анимаций
    protected CaseContent item;

    protected boolean alreadyHasThisItem;

    protected SongPlayer caseOpenSongPlayer;

    AbstractCaseOpener(@NonNull AbstractPlugin plugin,
                       @NonNull MysteryBox mysteryBox,
                       @NonNull Gamer gamer,
                       @NonNull CaseType type) {

        this.plugin = plugin;
        this.mysteryBox = mysteryBox;
        this.gamer = gamer;
        this.type = type;
        this.block = mysteryBox.getLocation().getBlock();
    }

    public final boolean open() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        String propertyName = type.name() + "_RANDOM";

        int pseudoRandom = gamer.getProperty(propertyName);

        for (int i = 0; i < ITEMS_PER_ONE_CASE; i++) {
            Rarity rarity = CaseManager.IMP.getRandomRarity(type, pseudoRandom);

            if (rarity == null) {
                gamer.sendLocalizedMessage("mysterybox_no_items");
                return false;
            }

            List<CaseContent> items = CaseManager.IMP.getItems(type, rarity);
            rewardList.add(items.get(random.nextInt(items.size())));
        }

        item = rewardList.get(rewardList.size() - 1);

        CaseManager.ContentIssueResult result = CaseManager.IMP.giveReward(gamer, item);

        // если FAIL - то игроку просто возвращается кейс, прокрутка отменяется
        if (result == CaseManager.ContentIssueResult.ALREADY_HAS) {
            alreadyHasThisItem = true;
        } else if (result == CaseManager.ContentIssueResult.FAIL) {
            return false;
        }

        int rarity = item.getRarity().ordinal();

        // повышаем если выбил COMMON, сбрасываем если выбил EPIC или выше
        if (rarity > Rarity.RARE.ordinal()) {
            gamer.setProperty(propertyName, 0);
        } else if (rarity < Rarity.RARE.ordinal()) {
            gamer.setProperty(propertyName, pseudoRandom + 1);
        }

        if (caseOpenerSong != null) {
            caseOpenSongPlayer = new PositionSongPlayer(plugin, caseOpenerSong);

            for (Player player : Bukkit.getOnlinePlayers()) {
                caseOpenSongPlayer.addPlayer(player);
            }

            ((PositionSongPlayer) caseOpenSongPlayer).setTargetLocation(block.getLocation());
            caseOpenSongPlayer.setRepeat(RepeatMode.NO);
            caseOpenSongPlayer.setAutoDestroy(true);
            caseOpenSongPlayer.setFadeIn(new Fade(FadeType.LINEAR, 40));
            caseOpenSongPlayer.setFadeOut(new Fade(FadeType.LINEAR, 40));
            caseOpenSongPlayer.setVolume((byte) 100);
            caseOpenSongPlayer.setPlaying(true);
        }

        new AbstractTask(plugin) {
            @Override
            protected void run() {
                if (!onTick()) {
                    cancel();
                    end();
                }
            }
        }.timer(1, 1);

        onStart();

        return true;
    }

    private void end() {
        if (caseOpenSongPlayer != null) {
            caseOpenSongPlayer.setPlaying(false);
            caseOpenSongPlayer = null;
        }

        onEnd();
    }

    public final void playerJoined(@NonNull Gamer gamer) {
        if (caseOpenSongPlayer != null) {
            caseOpenSongPlayer.addPlayer(gamer.getPlayer());
        }

        onPlayerJoin(gamer);
    }

    protected void onStart() {

    }

    public void onPlayerJoin(@NonNull Gamer gamer) {

    }

    protected void onEnd() {

    }

    protected abstract boolean onTick();

    protected void unregister() {
        mysteryBox.setCaseOpener(null);
    }

}
