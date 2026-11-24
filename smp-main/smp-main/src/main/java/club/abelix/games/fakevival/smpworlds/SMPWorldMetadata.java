package club.abelix.games.fakevival.smpworlds;

import org.bukkit.World;

public final class SMPWorldMetadata {
    private final String worldId;
    private final String displayName;
    private final boolean template;
    private final String parentTemplate;
    private final SMPWorldState state;
    private final String worldType;
    private final World.Environment environment;
    private final Long seed;
    private final double spawnX;
    private final double spawnY;
    private final double spawnZ;
    private final float spawnYaw;
    private final float spawnPitch;
    private final long createdAt;
    private final long updatedAt;
    private final boolean persistent;
    private final boolean temporary;
    private final boolean saveOnHubsave;
    private final String sourceFormat;
    private final boolean requiresBridge;

    public SMPWorldMetadata(String worldId, String displayName, boolean template, String parentTemplate,
                            SMPWorldState state, String worldType, World.Environment environment, Long seed,
                            double spawnX, double spawnY, double spawnZ, float spawnYaw, float spawnPitch,
                            long createdAt, long updatedAt) {
        this(worldId, displayName, template, parentTemplate, state, worldType, environment, seed,
                spawnX, spawnY, spawnZ, spawnYaw, spawnPitch, createdAt, updatedAt, true, false, true, "SMP", false);
    }

    public SMPWorldMetadata(String worldId, String displayName, boolean template, String parentTemplate,
                            SMPWorldState state, String worldType, World.Environment environment, Long seed,
                            double spawnX, double spawnY, double spawnZ, float spawnYaw, float spawnPitch,
                            long createdAt, long updatedAt, boolean persistent, boolean temporary, boolean saveOnHubsave,
                            String sourceFormat, boolean requiresBridge) {
        this.worldId = worldId;
        this.displayName = displayName == null || displayName.isBlank() ? worldId : displayName;
        this.template = template;
        this.parentTemplate = parentTemplate;
        this.state = state == null ? SMPWorldState.UNLOADED : state;
        this.worldType = worldType == null || worldType.isBlank() ? "NORMAL" : worldType;
        this.environment = environment == null ? World.Environment.NORMAL : environment;
        this.seed = seed;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.spawnYaw = spawnYaw;
        this.spawnPitch = spawnPitch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.persistent = persistent;
        this.temporary = temporary;
        this.saveOnHubsave = saveOnHubsave;
        this.sourceFormat = sourceFormat == null || sourceFormat.isBlank() ? "SMP" : sourceFormat;
        this.requiresBridge = requiresBridge;
    }

    public static SMPWorldMetadata create(String worldId, boolean template, String parentTemplate) {
        long now = System.currentTimeMillis();
        return new SMPWorldMetadata(worldId, worldId, template, parentTemplate, SMPWorldState.UNLOADED,
                "NORMAL", World.Environment.NORMAL, null, 0.0D, 64.0D, 0.0D, 0.0F, 0.0F, now, now);
    }

    public SMPWorldMetadata withState(SMPWorldState state) {
        return new SMPWorldMetadata(worldId, displayName, template, parentTemplate, state, worldType, environment,
                seed, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch, createdAt, System.currentTimeMillis(),
                persistent, temporary, saveOnHubsave, sourceFormat, requiresBridge);
    }

    public SMPWorldMetadata asClone(String newWorldId, String templateId) {
        long now = System.currentTimeMillis();
        return new SMPWorldMetadata(newWorldId, newWorldId, false, templateId, SMPWorldState.UNLOADED,
                worldType, environment, seed, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch, now, now,
                persistent, temporary, saveOnHubsave, sourceFormat, requiresBridge);
    }

    public SMPWorldMetadata asTemporaryDungeonInstance() {
        return new SMPWorldMetadata(worldId, displayName, false, parentTemplate, state, "DUNGEON_INSTANCE", environment,
                seed, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch, createdAt, System.currentTimeMillis(),
                false, true, false, sourceFormat, requiresBridge);
    }

    public SMPWorldMetadata withImportDetails(String displayName, boolean template, String worldType,
                                               World.Environment environment, String sourceFormat,
                                               boolean requiresBridge) {
        return new SMPWorldMetadata(worldId, displayName, template, parentTemplate, SMPWorldState.UNLOADED,
                worldType, environment, seed, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch, createdAt,
                System.currentTimeMillis(), persistent, temporary, saveOnHubsave, sourceFormat, requiresBridge);
    }

    public static SMPWorldMetadata fromLoadedWorld(String worldId, World world, boolean dungeonTemplate) {
        long now = System.currentTimeMillis();
        org.bukkit.Location spawn = world.getSpawnLocation();
        return new SMPWorldMetadata(worldId, world.getName(), dungeonTemplate, null, SMPWorldState.LOADED,
                dungeonTemplate ? "DUNGEON_TEMPLATE" : "NORMAL", world.getEnvironment(), world.getSeed(),
                spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch(), now, now,
                true, false, true, "SMP", false);
    }

    public SMPWorldMetadata refreshedFrom(World world, boolean dungeonTemplate) {
        org.bukkit.Location spawn = world.getSpawnLocation();
        return new SMPWorldMetadata(worldId, world.getName(), dungeonTemplate || template, parentTemplate,
                SMPWorldState.LOADED, dungeonTemplate ? "DUNGEON_TEMPLATE" : worldType,
                world.getEnvironment(), world.getSeed(), spawn.getX(), spawn.getY(), spawn.getZ(),
                spawn.getYaw(), spawn.getPitch(), createdAt, System.currentTimeMillis(), true, false, true,
                "SMP", false);
    }

    public String worldId() { return worldId; }
    public String displayName() { return displayName; }
    public boolean template() { return template; }
    public String parentTemplate() { return parentTemplate; }
    public SMPWorldState state() { return state; }
    public String worldType() { return worldType; }
    public World.Environment environment() { return environment; }
    public Long seed() { return seed; }
    public double spawnX() { return spawnX; }
    public double spawnY() { return spawnY; }
    public double spawnZ() { return spawnZ; }
    public float spawnYaw() { return spawnYaw; }
    public float spawnPitch() { return spawnPitch; }
    public long createdAt() { return createdAt; }
    public long updatedAt() { return updatedAt; }
    public boolean persistent() { return persistent; }
    public boolean temporary() { return temporary; }
    public boolean saveOnHubsave() { return saveOnHubsave; }
    public String sourceFormat() { return sourceFormat; }
    public boolean requiresBridge() { return requiresBridge; }
}
