package net.fakepixel.housing.world;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.io.File;
import java.io.IOException;


public class SlimeWorldManagerService implements WorldService {
    private final HousingPlugin plugin;

    public SlimeWorldManagerService(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public World loadHouseWorld(House house) {
        World alreadyLoaded = Bukkit.getWorld(house.getWorldName());
        if (alreadyLoaded != null) return alreadyLoaded;

        try {
            ensureSlimeWorldFolder();
            World aspWorld = tryLoadAdvancedSlimePaper(house);
            if (aspWorld != null) return aspWorld;

            Plugin swm = findSlimePlugin();
            if (swm == null) throw new IllegalStateException("No slime world plugin/API is installed");
            Object loader = call(swm, new Class[]{String.class}, plugin.getStaticConfig().getString("slimeworldmanager.loader", "file"));
            Object properties = createPropertyMap();

            Object houseWorld = tryLoadSlimeWorld(swm, loader, house.getWorldName(), properties);
            if (houseWorld == null) {
                importTemplate(swm, loader, house);
                houseWorld = tryLoadSlimeWorld(swm, loader, house.getWorldName(), properties);
                if (houseWorld == null) {
                    throw new IllegalStateException("Imported slime world could not be loaded: " + house.getWorldName());
                }
            }

            callAny(swm, "generateWorld", houseWorld);
            World world = Bukkit.getWorld(house.getWorldName());
            if (world == null) throw new IllegalStateException("SWM generated no Bukkit world for " + house.getWorldName());
            net.fakepixel.housing.util.Compat.setGameRule(world, "doMobSpawning", "false");
            net.fakepixel.housing.util.Compat.setGameRule(world, "doDaylightCycle", "false");
            world.setTime(6000L);
            return world;
        } catch (Throwable throwable) {
            throw new IllegalStateException("SlimeWorldManager load failed: " + throwable.getMessage(), throwable);
        }
    }

    @Override
    public String getName() {
        return "SlimeWorld";
    }

    public World loadThemeWorld(String themeName) {
        String clean = cleanWorldName(themeName);
        if (clean.isEmpty()) throw new IllegalStateException("Theme SlimeWorld name is empty");
        String tempName = "__housing_theme_" + clean + "_" + System.currentTimeMillis();
        try {
            ensureSlimeWorldFolder();
            copyThemeSlimeFile(clean, tempName);
            World aspWorld = tryLoadAdvancedSlimePaperWorld(tempName);
            if (aspWorld != null) return aspWorld;

            Plugin swm = findSlimePlugin();
            if (swm == null) throw new IllegalStateException("No slime world plugin/API is installed");
            Object loader = call(swm, new Class[]{String.class}, plugin.getStaticConfig().getString("slimeworldmanager.loader", "file"));
            Object properties = createPropertyMap();
            Object slimeWorld = tryLoadSlimeWorld(swm, loader, tempName, properties);
            if (slimeWorld == null) throw new IllegalStateException("Could not load theme SlimeWorld " + clean);
            callAny(swm, "generateWorld", slimeWorld);
            World world = Bukkit.getWorld(tempName);
            if (world == null) throw new IllegalStateException("SWM generated no Bukkit world for theme " + clean);
            configureWorld(world);
            return world;
        } catch (Throwable throwable) {
            cleanupTemporarySlimeWorld(tempName);
            throw new IllegalStateException("Theme SlimeWorld load failed: " + throwable.getMessage(), throwable);
        }
    }

    public void cleanupThemeWorld(World world) {
        if (world == null) return;
        String worldName = world.getName();
        if (!worldName.startsWith("__housing_theme_")) return;
        movePlayersOutOfTemporaryThemeWorld(world);
        if (Bukkit.unloadWorld(world, false) || Bukkit.getWorld(worldName) == null) {
            cleanupTemporarySlimeWorld(worldName);
        }
    }

    private void movePlayersOutOfTemporaryThemeWorld(World world) {
        if (world == null || world.getPlayers().isEmpty()) return;
        org.bukkit.Location fallback = safeFallbackLocation();
        for (org.bukkit.entity.Player player : new java.util.ArrayList<>(world.getPlayers())) {
            if (fallback != null) player.teleport(fallback);
        }
    }

    private org.bukkit.Location safeFallbackLocation() {
        HouseManager houseManager = plugin.getHouseManager();
        if (houseManager != null) {
            for (House house : houseManager.getHouses()) {
                World houseWorld = Bukkit.getWorld(house.getWorldName());
                if (houseWorld == null || houseWorld.getPlayers().isEmpty()) continue;
                return houseManager.spawnLocation(house, houseWorld, true);
            }
        }
        World lobby = Bukkit.getWorld(plugin.getStaticConfig().getString("settings.lobby-world", "housinglobby"));
        if (lobby == null) return null;
        return lobby.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
    }

    private void ensureSlimeWorldFolder() {
        try {
            java.io.File folder = new java.io.File(plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds"));
            if (!folder.exists() && !folder.mkdirs()) plugin.getLogger().warning("Could not create " + folder.getPath());
        } catch (Throwable ignored) {}
    }

    private World tryLoadAdvancedSlimePaper(House house) throws Exception {
        Object asp = advancedSlimePaperApi();
        if (asp == null) return null;

        String loaderName = plugin.getStaticConfig().getString("slimeworldmanager.loader", "file");
        Object loader = getAdvancedLoader(loaderName);
        if (loader == null) {
            throw new IllegalStateException("Could not create ASP slime loader " + loaderName);
        }
        Object properties = createPropertyMap();

        Object slimeWorld = tryCall(asp, "readWorld", loader, house.getWorldName(), false, properties);
        if (slimeWorld == null) {

            slimeWorld = createEmptyWorldAdvanced(asp, loader, house, properties);
            Object instance = callAny(asp, "loadWorld", slimeWorld, true);
            World world = bukkitWorld(instance, house.getWorldName());
            if (world == null) throw new IllegalStateException("ASP loaded no Bukkit world for " + house.getWorldName());
            configureWorld(world);
            pasteFullTemplateRegion(house, world, true);
            saveAdvancedWorld(asp, loader, slimeWorld, instance, world);
            removeNormalWorldFolderIfStoredAsSlime(world.getName());
            return world;
        }

        Object instance = callAny(asp, "loadWorld", slimeWorld, true);
        World world = bukkitWorld(instance, house.getWorldName());
        if (world == null) throw new IllegalStateException("ASP loaded no Bukkit world for " + house.getWorldName());
        configureWorld(world);
        if (repairMissingTemplateOutsideBorder(house, world)) {
            saveAdvancedWorld(asp, loader, slimeWorld, instance, world);
            removeNormalWorldFolderIfStoredAsSlime(world.getName());
        }
        return world;
    }

    private Object createEmptyWorldAdvanced(Object asp, Object loader, House house, Object properties) throws Exception {

        return callAny(asp, "createEmptyWorld", house.getWorldName(), false, properties, loader);
    }

    private void saveAdvancedWorld(Object asp, Object loader, Object slimeWorld, Object instance, World world) {

        if (invokeAny(asp, "saveWorld", slimeWorld)) return;
        if (invokeAny(asp, "saveWorld", instance)) return;
        if (invokeAny(asp, "saveWorld", world)) return;
        if (invokeAny(asp, "saveWorld", world.getName())) return;
        if (invokeAny(asp, "saveWorld", loader, slimeWorld)) return;
        if (invokeAny(asp, "saveWorld", slimeWorld, loader)) return;
        if (invokeAny(asp, "saveWorld", loader, instance)) return;
        if (invokeAny(asp, "saveWorld", instance, loader)) return;
        if (invokeAny(asp, "saveWorld", loader, world.getName())) return;
        if (invokeAny(asp, "saveWorld", world.getName(), loader)) return;
        if (invokeAny(slimeWorld, "save")) return;
        if (invokeAny(instance, "save")) return;
        if (invokeAny(loader, "saveWorld", slimeWorld)) return;
        if (invokeAny(loader, "saveWorld", instance)) return;
        if (invokeAny(loader, "writeWorld", slimeWorld)) return;
        if (invokeAny(loader, "writeWorld", instance)) return;
        if (invokeAny(loader, "save", slimeWorld)) return;
        if (invokeAny(loader, "save", instance)) return;
        plugin.getLogger().warning("Could not explicitly save pasted ASP house world " + world.getName() + " into slime_worlds. Check your AdvancedSlimePaper save API/version.");
    }

    private World loadFullTemplateFolderWorld(House house) throws Exception {
        File worldFolder = new File(Bukkit.getWorldContainer(), house.getWorldName());
        if (!worldFolder.exists()) {
            File template = templateFolder(house);
            copyDirectory(template, worldFolder);
            cleanCopiedWorldFolder(worldFolder);
        }
        World world = Bukkit.createWorld(new WorldCreator(house.getWorldName()));
        if (world == null) throw new IllegalStateException("Bukkit could not load copied template world " + house.getWorldName());
        configureWorld(world);
        return world;
    }

    private void cleanCopiedWorldFolder(File worldFolder) {
        deleteDirectory(new File(worldFolder, "uid.dat"));
        deleteDirectory(new File(worldFolder, "session.lock"));
        deleteDirectory(new File(worldFolder, "playerdata"));
    }

    private void removeNormalWorldFolderIfStoredAsSlime(String worldName) {
        if (!plugin.getStaticConfig().getBoolean("slimeworldmanager.store-house-worlds-in-slime-worlds", true)) return;
        File normalWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!normalWorldFolder.exists()) return;

        File storage = new File(plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds"));
        boolean hasSlimeSave = new File(storage, worldName).exists()
                || new File(storage, worldName + ".slime").exists()
                || new File(storage, worldName + ".slime.gz").exists()
                || new File(storage, worldName + ".swm").exists()
                || new File(storage, worldName + ".dat").exists();

        if (hasSlimeSave) {
            deleteDirectory(normalWorldFolder);
        }
    }

    private void pasteTemplateInsideBorder(House house, World targetWorld) throws Exception {

        pasteFullTemplateRegion(house, targetWorld, true);
    }

    private boolean repairMissingTemplateOutsideBorder(House house, World targetWorld) throws Exception {
        if (!plugin.getStaticConfig().getBoolean("slimeworldmanager.repair-missing-template-outside-border", true)) return false;

        File template = templateFolder(house);
        File tempWorldFolder = new File(Bukkit.getWorldContainer(), "__housing_repair_check_" + house.getOwner().toString().replace("-", ""));
        World templateWorld = null;
        try {
            if (tempWorldFolder.exists()) deleteDirectory(tempWorldFolder);
            copyDirectory(template, tempWorldFolder);
            cleanCopiedWorldFolder(tempWorldFolder);
            templateWorld = Bukkit.createWorld(new WorldCreator(tempWorldFolder.getName()));
            if (templateWorld == null) return false;

            TemplateCounts counts = countOutsideBorder(templateWorld, targetWorld);
            int templateNeeded = plugin.getStaticConfig().getInt("slimeworldmanager.repair-template-min-outside-blocks", 100);
            int targetAllowed = plugin.getStaticConfig().getInt("slimeworldmanager.repair-template-max-existing-outside-blocks", 25);
            if (counts.templateOutsideNonAir >= templateNeeded && counts.targetOutsideNonAir <= targetAllowed) {
                plugin.getLogger().warning("House " + targetWorld.getName() + " is missing most of the default template outside the red border. Repairing it from the full template folder.");
                copyTemplateRegion(templateWorld, targetWorld, true);
                return true;
            }
            return false;
        } finally {
            if (templateWorld != null) Bukkit.unloadWorld(templateWorld, false);
            deleteDirectory(tempWorldFolder);
        }
    }

    private void pasteFullTemplateRegion(House house, World targetWorld, boolean includeAir) throws Exception {
        File template = templateFolder(house);
        File tempWorldFolder = new File(Bukkit.getWorldContainer(), "__housing_template_" + house.getOwner().toString().replace("-", ""));
        World templateWorld = null;
        try {
            if (tempWorldFolder.exists()) deleteDirectory(tempWorldFolder);
            copyDirectory(template, tempWorldFolder);
            cleanCopiedWorldFolder(tempWorldFolder);
            templateWorld = Bukkit.createWorld(new WorldCreator(tempWorldFolder.getName()));
            if (templateWorld == null) throw new IllegalStateException("Could not load template world " + template.getName());
            copyTemplateRegion(templateWorld, targetWorld, includeAir);
        } finally {
            if (templateWorld != null) Bukkit.unloadWorld(templateWorld, false);
            deleteDirectory(tempWorldFolder);
        }
    }

    @SuppressWarnings("deprecation")
    private void copyTemplateRegion(World source, World target, boolean includeAir) {
        int minX = plugin.getStaticConfig().getInt("template-copy.min-x", -128);
        int maxX = plugin.getStaticConfig().getInt("template-copy.max-x", 128);
        int minZ = plugin.getStaticConfig().getInt("template-copy.min-z", -128);
        int maxZ = plugin.getStaticConfig().getInt("template-copy.max-z", 128);
        int minY = Math.max(0, plugin.getStaticConfig().getInt("template-copy.min-y", 0));
        int maxY = Math.min(256 - 1, plugin.getStaticConfig().getInt("template-copy.max-y", 256 - 1));

        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                source.getChunkAt(x >> 4, z >> 4).load();
                target.getChunkAt(x >> 4, z >> 4).load();
                for (int y = minY; y <= maxY; y++) {
                    org.bukkit.block.Block sourceBlock = source.getBlockAt(x, y, z);
                    if (!includeAir && sourceBlock.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) continue;
                    org.bukkit.block.Block targetBlock = target.getBlockAt(x, y, z);
                    targetBlock.setTypeIdAndData(sourceBlock.getTypeId(), sourceBlock.getData(), false);
                    copyBlockState(sourceBlock, targetBlock);
                }
            }
        }
    }

    private TemplateCounts countOutsideBorder(World source, World target) {
        int minX = plugin.getStaticConfig().getInt("template-copy.min-x", -128);
        int maxX = plugin.getStaticConfig().getInt("template-copy.max-x", 128);
        int minZ = plugin.getStaticConfig().getInt("template-copy.min-z", -128);
        int maxZ = plugin.getStaticConfig().getInt("template-copy.max-z", 128);
        int minY = Math.max(0, plugin.getStaticConfig().getInt("template-copy.min-y", 0));
        int maxY = Math.min(256 - 1, plugin.getStaticConfig().getInt("template-copy.max-y", 256 - 1));
        HouseManager manager = plugin.getHouseManager();
        House house = manager == null ? null : manager.getHouseByWorld(target.getName());
        HouseManager.BorderBounds bounds = manager == null ? null : manager.borderBounds(house);
        int borderMinX = bounds == null ? Math.min(plugin.getStaticConfig().getInt("border.min-x", -21), plugin.getStaticConfig().getInt("border.max-x", 17)) : bounds.minX;
        int borderMaxX = bounds == null ? Math.max(plugin.getStaticConfig().getInt("border.min-x", -21), plugin.getStaticConfig().getInt("border.max-x", 17)) : bounds.maxX;
        int borderMinZ = bounds == null ? Math.min(plugin.getStaticConfig().getInt("border.min-z", -36), plugin.getStaticConfig().getInt("border.max-z", 7)) : bounds.minZ;
        int borderMaxZ = bounds == null ? Math.max(plugin.getStaticConfig().getInt("border.min-z", -36), plugin.getStaticConfig().getInt("border.max-z", 7)) : bounds.maxZ;

        int templateNonAir = 0;
        int targetNonAir = 0;
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x += 2) {
            for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z += 2) {
                boolean insideOrOnBorder = x >= borderMinX && x <= borderMaxX && z >= borderMinZ && z <= borderMaxZ;
                if (insideOrOnBorder) continue;
                source.getChunkAt(x >> 4, z >> 4).load();
                target.getChunkAt(x >> 4, z >> 4).load();
                for (int y = minY; y <= maxY; y += 2) {
                    if (source.getBlockAt(x, y, z).getType() != net.fakepixel.housing.util.Compat.mat("AIR")) templateNonAir++;
                    if (target.getBlockAt(x, y, z).getType() != net.fakepixel.housing.util.Compat.mat("AIR")) targetNonAir++;
                    if (templateNonAir > 1000 && targetNonAir > 1000) return new TemplateCounts(templateNonAir, targetNonAir);
                }
            }
        }
        return new TemplateCounts(templateNonAir, targetNonAir);
    }

    private static final class TemplateCounts {
        private final int templateOutsideNonAir;
        private final int targetOutsideNonAir;

        private TemplateCounts(int templateOutsideNonAir, int targetOutsideNonAir) {
            this.templateOutsideNonAir = templateOutsideNonAir;
            this.targetOutsideNonAir = targetOutsideNonAir;
        }
    }

    private void copyBlockState(org.bukkit.block.Block sourceBlock, org.bukkit.block.Block targetBlock) {
        org.bukkit.block.BlockState sourceState = sourceBlock.getState();
        org.bukkit.block.BlockState targetState = targetBlock.getState();
        if (sourceState instanceof org.bukkit.inventory.InventoryHolder && targetState instanceof org.bukkit.inventory.InventoryHolder) {
            ((org.bukkit.inventory.InventoryHolder) targetState).getInventory().setContents(((org.bukkit.inventory.InventoryHolder) sourceState).getInventory().getContents());
        }
        if (sourceState instanceof org.bukkit.block.Sign && targetState instanceof org.bukkit.block.Sign) {
            org.bukkit.block.Sign sourceSign = (org.bukkit.block.Sign) sourceState;
            org.bukkit.block.Sign targetSign = (org.bukkit.block.Sign) targetState;
            for (int i = 0; i < 4; i++) targetSign.setLine(i, sourceSign.getLine(i));
            targetSign.update(true, false);
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) throw new IOException("Could not create " + target.getPath());
            File[] children = source.listFiles();
            if (children != null) for (File child : children) copyDirectory(child, new File(target, child.getName()));
            return;
        }
        java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteDirectory(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteDirectory(child);
        }
        if (!file.delete()) plugin.getLogger().warning("Could not delete " + file.getPath());
    }

    private Object tryLoadSlimeWorld(Object swm, Object loader, String worldName, Object properties) {
        Object result = tryCall(swm, "loadWorld", loader, worldName, properties);
        if (result != null) return result;
        result = tryCall(swm, "loadWorld", loader, worldName, false, properties);
        if (result != null) return result;
        result = tryCall(swm, "loadWorld", loader, worldName, true, properties);
        if (result != null) return result;
        result = tryCall(swm, "loadWorld", worldName);
        return result;
    }

    private void importTemplate(Object swm, Object loader, House house) {
        File template = templateFolder(house);
        if (invokeAny(swm, "importWorld", template, house.getWorldName(), loader)) return;
        if (invokeAny(swm, "importWorld", template, house.getWorldName(), loader, false)) return;
        if (invokeAny(swm, "importWorld", template.getPath(), house.getWorldName(), loader)) return;
        if (invokeAny(swm, "importWorld", loader, template, house.getWorldName())) return;
        if (invokeAny(swm, "importWorld", loader, template.getPath(), house.getWorldName())) return;
        if (invokeAny(loader, "importWorld", template, house.getWorldName())) return;
        if (invokeAny(loader, "importWorld", template.getPath(), house.getWorldName())) return;
        throw new IllegalStateException("Could not import template " + template.getName() + " into slime_worlds as " + house.getWorldName());
    }

    private File templateFolder(House house) {
        File template = new File(plugin.getDataFolder(), "templates/" + effectiveTemplateName(house));
        if (!template.exists()) {
            String fallbackTemplate = plugin.getStaticConfig().getString("settings.default-template", "basic");
            template = new File(plugin.getDataFolder(), "templates/" + fallbackTemplate);
        }
        if (!template.exists()) throw new IllegalStateException("Missing template folder " + template.getPath());
        return template;
    }

    private String effectiveTemplateName(House house) {
        String configuredDefault = plugin.getStaticConfig().getString("settings.default-template", "basic");
        String template = house.getTemplate();
        if (template == null || template.trim().isEmpty()) return configuredDefault;
        if (template.equalsIgnoreCase("air") && !configuredDefault.equalsIgnoreCase("air")) return configuredDefault;
        return template;
    }

    private World tryLoadAdvancedSlimePaperWorld(String worldName) throws Exception {
        Object asp = advancedSlimePaperApi();
        if (asp == null) return null;
        Object loader = getAdvancedLoader(plugin.getStaticConfig().getString("slimeworldmanager.loader", "file"));
        if (loader == null) return null;
        Object properties = createPropertyMap();
        Object slimeWorld = tryCall(asp, "readWorld", loader, worldName, false, properties);
        if (slimeWorld == null) return null;
        Object instance = callAny(asp, "loadWorld", slimeWorld, true);
        World world = bukkitWorld(instance, worldName);
        if (world != null) configureWorld(world);
        return world;
    }

    private void copyThemeSlimeFile(String themeName, String tempName) throws IOException {
        File source = themeSlimeFile(themeName);
        if (source == null) throw new IOException("Missing theme SlimeWorld slime_worlds/" + themeName + ".slime");
        String suffix = slimeSuffix(source.getName());
        File storage = new File(plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds"));
        if (!storage.exists() && !storage.mkdirs()) plugin.getLogger().warning("Could not create " + storage.getPath());
        java.nio.file.Files.copy(source.toPath(), new File(storage, tempName + suffix).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private File themeSlimeFile(String themeName) {
        File folder = new File(plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds"));
        String[] suffixes = new String[]{".slime", ".slime.gz", ".swm", ".dat"};
        for (String suffix : suffixes) {
            File file = new File(folder, themeName + suffix);
            if (file.exists() && file.isFile()) return file;
        }
        return null;
    }

    private String slimeSuffix(String name) {
        if (name.endsWith(".slime.gz")) return ".slime.gz";
        if (name.endsWith(".swm")) return ".swm";
        if (name.endsWith(".dat")) return ".dat";
        return ".slime";
    }

    private void cleanupTemporarySlimeWorld(String worldName) {
        if (worldName == null || !worldName.startsWith("__housing_theme_")) return;
        File storage = new File(plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds"));
        deleteDirectory(new File(storage, worldName));
        deleteDirectory(new File(storage, worldName + ".slime"));
        deleteDirectory(new File(storage, worldName + ".slime.gz"));
        deleteDirectory(new File(storage, worldName + ".swm"));
        deleteDirectory(new File(storage, worldName + ".dat"));
        deleteDirectory(new File(Bukkit.getWorldContainer(), worldName));
    }

    private String cleanWorldName(String raw) {
        if (raw == null) return "";
        String clean = raw.trim().replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) clean = clean.substring(slash + 1);
        if (clean.endsWith(".slime.gz")) clean = clean.substring(0, clean.length() - ".slime.gz".length());
        else if (clean.endsWith(".slime")) clean = clean.substring(0, clean.length() - ".slime".length());
        else if (clean.endsWith(".swm")) clean = clean.substring(0, clean.length() - ".swm".length());
        else if (clean.endsWith(".dat")) clean = clean.substring(0, clean.length() - ".dat".length());
        return clean.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private Object createPropertyMap() {
        String[] names = new String[]{
                "com.infernalsuite.asp.api.world.properties.SlimePropertyMap",
                "com.grinderwolf.swm.api.world.properties.SlimePropertyMap",
                "com.grinderwolf.swm.api.SlimePropertyMap"
        };
        for (String name : names) {
            try {
                Class<?> clazz = Class.forName(name);
                Constructor<?> constructor = clazz.getConstructor();
                return constructor.newInstance();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private Object advancedSlimePaperApi() {
        try {
            Class<?> clazz = Class.forName("com.infernalsuite.asp.api.AdvancedSlimePaperAPI");
            return clazz.getMethod("instance").invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Plugin findSlimePlugin() {
        String configured = plugin.getStaticConfig().getString("slimeworldmanager.plugin", "");
        if (!configured.trim().isEmpty()) {
            Plugin found = Bukkit.getPluginManager().getPlugin(configured);
            if (found != null) return found;
        }
        String[] names = new String[]{"SlimeWorldManager", "SlimeWorldPlugin", "AdvancedSlimePaper"};
        for (String name : names) {
            Plugin found = Bukkit.getPluginManager().getPlugin(name);
            if (found != null) return found;
        }
        return null;
    }

    private Object getAdvancedLoader(String loaderName) throws Exception {
        if (loaderName == null || loaderName.trim().isEmpty() || "file".equalsIgnoreCase(loaderName)) {
            return newFileLoader();
        }
        return getLegacyPluginLoader(loaderName);
    }

    private Object newFileLoader() throws Exception {
        String folderPath = plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds");
        String[] classNames = new String[]{
                "com.infernalsuite.asp.loaders.file.FileLoader",
                "net.fakepixel.housing.libs.asp.loaders.file.FileLoader"
        };
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                try {
                    return clazz.getConstructor(String.class).newInstance(folderPath);
                } catch (NoSuchMethodException ignored) {}
                try {
                    return clazz.getConstructor(File.class).newInstance(new File(folderPath));
                } catch (NoSuchMethodException ignored) {}
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private Object getLegacyPluginLoader(String loaderName) {
        Plugin swm = findSlimePlugin();
        if (swm == null) return null;
        return tryCall(swm, "getLoader", loaderName);
    }

    private World bukkitWorld(Object instance, String worldName) {
        if (instance instanceof World) return (World) instance;
        Object world = tryCall(instance, "getBukkitWorld");
        if (world instanceof World) return (World) world;
        return Bukkit.getWorld(worldName);
    }

    private void configureWorld(World world) {
        net.fakepixel.housing.util.Compat.setGameRule(world, "doMobSpawning", "false");
        net.fakepixel.housing.util.Compat.setGameRule(world, "doDaylightCycle", "false");
        world.setTime(6000L);
    }

    private Object call(Object target, Class[] types, Object... args) throws Exception {
        Method method = target.getClass().getMethod("getLoader", types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Object callAny(Object target, String name, Object... args) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) continue;
            if (!accepts(method.getParameterTypes(), args)) continue;
            try {
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (Throwable ignored) {}
        }
        throw new NoSuchMethodException(name);
    }

    private boolean accepts(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) continue;
            Class<?> type = wrap(types[i]);
            if (!type.isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private Object tryCall(Object target, String name, Object... args) {
        try { return callAny(target, name, args); } catch (Throwable throwable) { return null; }
    }

    private boolean invokeAny(Object target, String name, Object... args) {
        if (target == null) return false;
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) continue;
            if (!accepts(method.getParameterTypes(), args)) continue;
            try {
                method.setAccessible(true);
                method.invoke(target, args);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }
}
