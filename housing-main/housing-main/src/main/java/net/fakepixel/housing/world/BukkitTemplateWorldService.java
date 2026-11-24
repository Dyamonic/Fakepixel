package net.fakepixel.housing.world;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;

public class BukkitTemplateWorldService implements WorldService {
    private final HousingPlugin plugin;

    public BukkitTemplateWorldService(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public World loadHouseWorld(House house) throws Exception {
        World loaded = Bukkit.getWorld(house.getWorldName());
        if (loaded != null) return loaded;

        File worldFolder = new File(Bukkit.getWorldContainer(), house.getWorldName());
        if (!worldFolder.exists()) {
            File template = new File(plugin.getDataFolder(), "templates/" + house.getTemplate());
            if (!template.exists()) template = new File(plugin.getDataFolder(), "templates/" + plugin.getStaticConfig().getString("settings.default-template", "basic"));
            if (!template.exists()) throw new IOException("Missing template folder " + template.getPath());
            copyDirectory(template, worldFolder);
            deleteSessionLock(worldFolder);
        }

        World world = Bukkit.createWorld(new WorldCreator(house.getWorldName()));
        if (world == null) throw new IOException("Bukkit could not create world " + house.getWorldName());
        net.fakepixel.housing.util.Compat.setGameRule(world, "doMobSpawning", "false");
        net.fakepixel.housing.util.Compat.setGameRule(world, "doDaylightCycle", "false");
        world.setTime(6000L);
        world.setStorm(false);
        return world;
    }

    @Override
    public String getName() {
        return "Bukkit template folders";
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) throw new IOException("Could not create " + target.getPath());
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) copyDirectory(child, new File(target, child.getName()));
            }
            return;
        }
        java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteSessionLock(File worldFolder) {
        File lock = new File(worldFolder, "session.lock");
        deleteFile(lock);
        deleteDirectory(new File(worldFolder, "playerdata"));
        deleteFile(new File(worldFolder, "uid.dat"));
    }

    private void deleteDirectory(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteDirectory(child);
        }
        deleteFile(file);
    }

    private void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete " + file.getPath());
        }
    }
}
