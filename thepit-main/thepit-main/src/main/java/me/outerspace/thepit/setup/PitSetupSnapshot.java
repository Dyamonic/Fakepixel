package me.outerspace.thepit.setup;

import java.util.ArrayList;
import java.util.List;

public final class PitSetupSnapshot {
    public int schemaVersion = 1;
    public String exportedAt = "";
    public List<Arena> arenas = new ArrayList<>();
    public List<Leaderboard> leaderboards = new ArrayList<>();
    public List<Npc> npcs = new ArrayList<>();

    public static final class Arena {
        public String id;
        public boolean enabled = true;
        public String displayName;
        public String worldName;
        public String arenaType;
        public Location spawn;
        public Location death;
        public Location spectator;
        public Location voidReturn;
        public Region spawnProtection;
        public Region pvpArena;
    }

    public static final class Location {
        public String worldName;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;

        public Location() {}

        public Location(String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static final class Region {
        public Location pos1;
        public Location pos2;
    }

    public static final class Leaderboard {
        public String stableId;
        public String arenaId;
        public Location location;
        public String statType;
        public String period;
    }

    public static final class Npc {
        public String stableId;
        public String arenaId;
        public String worldName;
        public String typeId = "GENERIC";
        public String name;
        public Location location;
        public String skinId = "default";
        public String skinName = "";
        public String skinValue = "";
        public String skinSignature = "";
        public String actionId = "command";
        public String command = "";
        public String executionMode = "PLAYER";
        public String hologramId = "default";
        public boolean lookAtPlayers;
        public boolean enabled = true;
    }

}
