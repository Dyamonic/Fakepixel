package club.abelix.api.base.song.command;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.song.NoteBlockManager;
import club.abelix.api.base.song.model.RepeatMode;
import club.abelix.api.base.song.model.Song;
import club.abelix.api.base.song.model.SoundCategory;
import club.abelix.api.base.song.songplayer.DefaultSongPlayer;
import club.abelix.api.base.song.utils.NBSDecoder;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandMessageException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author artem
 */
@WithHelp
@NamedCommand("song")
@IgnoreLogs
public class SongCommand {

    private final AbstractPlugin plugin;

    public SongCommand(AbstractPlugin plugin) {
        this.plugin = plugin;
    }

    @WithPermission("command.song")
    @WithCondition("disableConsole")
    @SubcommandExecutor("stop")
    public static void stop(BukkitExecutionSession session) {
        NoteBlockManager.IMP.stopPlaying(session.getPlayer());

        session.sendPrefixedMessage("Воспроизведение остановлено");
    }

    @WithAlias("vol")
    @WithCondition("minArgs: 1")
    @WithCondition("disableConsole")
    @WithPermission("command.song")
    @SubcommandExecutor("volume")
    public static void volume(BukkitExecutionSession session) throws CommandException {
        byte sound = (byte) session.getIntArgument(0, "Volume");

        if (sound < 0 || sound > 100) {
            throw new CommandMessageException("Вы указали неверное значение звука");
        }

        NoteBlockManager.IMP.setPlayerVolume(session.getPlayer(), sound);

        session.sendPrefixedMessage("Звук установлен на §c" + sound);
    }

    @WithCondition("minArgs: 1")
    @WithCondition("disableConsole")
    @WithPermission("command.song")
    @SubcommandExecutor("play")
    public void play(BukkitExecutionSession session) throws CommandMessageException, IOException {
        String url = session.getArgumentString();

        URLConnection conn = new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "PlazmixNetwork");
        conn.setRequestProperty("Accept", "*");

        try (InputStream is = conn.getInputStream()) {
            Song song = NBSDecoder.parse(is);

            DefaultSongPlayer player = new DefaultSongPlayer(plugin, song, SoundCategory.VOICE);
            player.setAutoDestroy(true);
            player.setPlaying(true);
            player.setRepeat(RepeatMode.ALL);
            player.addPlayer(session.getPlayer());
        } catch (IOException ex) {
            throw new CommandMessageException("Вы указали неверную ссылку на файл");
        }

        session.sendPrefixedMessage("Песня воспроизведена");
    }
}
