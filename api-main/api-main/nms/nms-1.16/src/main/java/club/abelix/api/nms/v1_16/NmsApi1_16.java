package club.abelix.api.nms.v1_16;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.bossbar.FakeBossbarApi;
import club.abelix.api.nms.command.BaseCommandApi;
import club.abelix.api.nms.command.CommandApi;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.nbt.NbtApi;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.player.PlayerApi;
import club.abelix.api.nms.sound.SoundApi;
import club.abelix.api.nms.sound.SoundApiImpl;
import club.abelix.api.nms.team.FakeTeamApi;
import club.abelix.api.nms.v1_12.bossbar.FakeBossbarApi1_12;
import club.abelix.api.nms.v1_12.team.FakeTeamApi1_12;
import club.abelix.api.nms.v1_16.entity.FakeEntityApi1_16;
import club.abelix.api.nms.v1_16.nbt.NbtApi1_16;
import club.abelix.api.nms.v1_16.packet.PacketLibraryApi1_16;
import club.abelix.api.nms.v1_16.player.PlayerApi1_16;
import club.abelix.api.nms.v1_16.world.WorldApi1_16;
import club.abelix.api.nms.world.WorldApi;
import club.abelix.commons.version.GameVersion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
@Getter
public class NmsApi1_16 implements NmsApi {

    private final Plugin plugin;

    private CommandApi commandApi;
    private PacketLibraryApi packetLibraryApi;
    private FakeEntityApi fakeEntityApi;
    private FakeTeamApi fakeTeamApi;
    private PlayerApi playerApi;
    private SoundApi soundApi;
    private FakeBossbarApi bossbarApi;
    private WorldApi worldApi;
    private NbtApi nbtApi;

    @Override
    public void load() {
        commandApi = new BaseCommandApi();
        packetLibraryApi = new PacketLibraryApi1_16();
        fakeEntityApi = new FakeEntityApi1_16(this);
        fakeTeamApi = new FakeTeamApi1_12(this);
        playerApi = new PlayerApi1_16(this);
        worldApi = new WorldApi1_16(this);
        nbtApi = new NbtApi1_16();
        soundApi = new SoundApiImpl(this);
        bossbarApi = new FakeBossbarApi1_12(this);
    }

    @Override
    public void enable() {
        getFakeEntityApi().registerFakeEntityListener(this);
        getFakeTeamApi().registerTeamListener(plugin);
        getBossbarApi().registerBossbarListener(plugin);
    }

    @Override
    public int getProtocolNumber() {
        return GameVersion.MINECRAFT_1_16_4.getVersion();
    }

    @Override
    public double[] getTPS() {
        return MinecraftServer.getServer().recentTps;
    }

}
