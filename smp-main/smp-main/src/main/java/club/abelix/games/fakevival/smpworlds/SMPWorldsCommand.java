package club.abelix.games.fakevival.smpworlds;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public final class SMPWorldsCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final SMPWorldManager manager;

    public SMPWorldsCommand(JavaPlugin plugin, SMPWorldManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smpworlds.admin") && !sender.hasPermission("fakevival.smpworld.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "dbstatus" -> {
                report(sender, manager.dbStatus());
                return true;
            }
            case "list" -> {
                manager.listSummariesAsync().whenComplete((lines, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) sender.sendMessage(ChatColor.RED + "SMPWorlds error: " + rootMessage(throwable));
                    else {
                        sender.sendMessage(ChatColor.GOLD + "SMPWorlds database worlds:");
                        for (String line : lines) sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + line);
                    }
                }));
                return true;
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds info <worldId>");
                    return true;
                }
                report(sender, manager.info(args[1]));
                return true;
            }
            case "import" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds import <vanillaWorldFolder> <worldId> [template]");
                    return true;
                }
                boolean template = args.length >= 4 && Boolean.parseBoolean(args[3]);
                sender.sendMessage(ChatColor.GRAY + "Compressing and saving world into the SMP database...");
                report(sender, manager.importWorld(args[1], args[2], template));
                return true;
            }
            case "importfile" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds importfile <fileName> <worldId> [template]");
                    return true;
                }
                boolean template = args.length >= 4 && Boolean.parseBoolean(args[3]);
                reportImport(sender, args[1], args[2], template);
                return true;
            }
            case "importslime" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds importslime <fileName> <worldId> [template]");
                    return true;
                }
                boolean template = args.length >= 4 && Boolean.parseBoolean(args[3]);
                reportSlimeImport(sender, args[1], args[2], template);
                return true;
            }
            case "load" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds load <worldId>");
                    return true;
                }
                sender.sendMessage(ChatColor.GRAY + "Loading SMP database world " + args[1] + "...");
                reportWorld(sender, manager.loadWorld(args[1]));
                return true;
            }
            case "save" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworld save <worldname> [--force]");
                    return true;
                }
                boolean force = args.length >= 3 && args[2].equalsIgnoreCase("--force");
                String saverUuid = sender instanceof Player player ? player.getUniqueId().toString() : null;
                manager.saveWorldToDatabase(args[1], force, false, saverUuid, sender.getName(), null).whenComplete((message, throwable) ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (throwable == null) {
                                sender.sendMessage(ChatColor.GREEN + "Saved SMPWorld " + ChatColor.YELLOW + args[1]
                                        + ChatColor.GREEN + " to the database.");
                                return;
                            }
                            Throwable root = rootCause(throwable);
                            if (root instanceof SMPWorldManager.WorldNotLoadedException) {
                                sender.sendMessage(ChatColor.RED + "World " + ChatColor.YELLOW + args[1] + ChatColor.RED + " is not loaded.");
                            } else if (root instanceof SMPWorldManager.EmptyWorldOverwriteException) {
                                sender.sendMessage(ChatColor.RED + rootMessage(throwable));
                            } else {
                                plugin.getLogger().severe("[SMPWorlds] Save failed for " + args[1] + ": " + rootMessage(throwable));
                                sender.sendMessage(ChatColor.RED + "Could not save SMPWorld " + ChatColor.YELLOW + args[1]
                                        + ChatColor.RED + ". Check console.");
                            }
                        }));
                return true;
            }
            case "unload" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds unload <worldId> [save]");
                    return true;
                }
                boolean save = args.length >= 3 && Boolean.parseBoolean(args[2]);
                sender.sendMessage(ChatColor.GRAY + "Unloading SMP world " + args[1] + " save=" + save + "...");
                report(sender, manager.unloadWorld(args[1], save));
                return true;
            }
            case "clone" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds clone <templateId> <newWorldId>");
                    return true;
                }
                report(sender, manager.cloneWorld(args[1], args[2]));
                return true;
            }
            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds delete <worldId>");
                    return true;
                }
                report(sender, manager.deleteWorld(args[1]));
                return true;
            }
            case "create" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds create <worldId> [description]");
                    return true;
                }
                sender.sendMessage(ChatColor.GRAY + "Creating void SMP world with one bedrock block at y=64...");
                report(sender, manager.createVoidWorld(args[1]));
                return true;
            }
            case "goto" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use /smpworlds goto.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpworlds goto <worldId>");
                    return true;
                }
                String worldId = args[1];
                if (!worldId.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) { sender.sendMessage(ChatColor.RED + "World not found: " + worldId); return true; }
                World loaded = manager.getBukkitWorld(worldId);
                if (loaded != null) {
                    teleport(player, loaded);
                    return true;
                }
                loaded = Bukkit.getWorld(worldId);
                if (loaded != null) { teleport(player, loaded); return true; }
                java.nio.file.Path root=plugin.getServer().getWorldContainer().toPath().normalize();
                java.nio.file.Path folder=root.resolve(worldId).normalize();
                if(folder.startsWith(root)&&java.nio.file.Files.isDirectory(folder)&&java.nio.file.Files.isRegularFile(folder.resolve("level.dat"))){
                    World normal=Bukkit.createWorld(new org.bukkit.WorldCreator(worldId));
                    if(normal==null)sender.sendMessage(ChatColor.RED+"Failed to load world "+worldId+". Check console.");else teleport(player,normal);
                    return true;
                }
                sender.sendMessage(ChatColor.GRAY + "World is not loaded. Loading " + worldId + " from the database...");
                manager.loadWorld(worldId).whenComplete((world, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) sender.sendMessage(ChatColor.RED + "World not found: " + worldId);
                    else teleport(player, world.bukkitWorld());
                }));
                return true;
            }
            default -> {
                help(sender);
                return true;
            }
        }
    }

    private void teleport(Player player, World world) {
        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.YELLOW + world.getName() + ChatColor.GREEN + ".");
    }

    private void report(CommandSender sender, CompletableFuture<String> future) {
        future.whenComplete((message, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (throwable != null) sender.sendMessage(ChatColor.RED + "SMPWorlds error: " + rootMessage(throwable));
            else if (message.startsWith("Error:")) sender.sendMessage(ChatColor.RED + message);
            else sender.sendMessage(ChatColor.GREEN + message);
        }));
    }

    private void reportWorld(CommandSender sender, CompletableFuture<SMPWorld> future) {
        future.whenComplete((world, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (throwable != null) sender.sendMessage(ChatColor.RED + "SMPWorlds error: " + rootMessage(throwable));
            else sender.sendMessage(ChatColor.GREEN + "Loaded " + world.worldId()
                    + ChatColor.GRAY + " as " + ChatColor.YELLOW + world.bukkitWorld().getName());
        }));
    }

    private void reportImport(CommandSender sender, String fileName, String worldId, boolean template) {
        manager.importFile(fileName, worldId, template).whenComplete((message, throwable) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        Throwable root = throwable;
                        while (root.getCause() != null) root = root.getCause();
                        if (root instanceof SMPWorldManager.WorldAlreadyExistsException) {
                            sender.sendMessage(ChatColor.RED + "A SMPWorld with id " + worldId + " already exists.");
                        } else {
                            plugin.getLogger().severe("[SMPWorlds] Database import failed: " + rootMessage(throwable));
                            sender.sendMessage(ChatColor.RED + "Failed to import SMPWorld into database. Check console.");
                        }
                    } else if (message.startsWith("Error: File not found:")) {
                        sender.sendMessage(ChatColor.RED + message.substring("Error: ".length()));
                    } else if (message.startsWith("Error: Invalid world id.")) {
                        sender.sendMessage(ChatColor.RED + "Invalid world id. Use only letters, numbers, underscore, and dash.");
                    } else if (message.startsWith("Error:")) {
                        sender.sendMessage(ChatColor.RED + message.substring("Error: ".length()));
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Imported SMPWorld " + ChatColor.YELLOW + worldId
                                + ChatColor.GREEN + " to database. " + ChatColor.GRAY + "Template: " + ChatColor.YELLOW + template);
                    }
                }));
    }

    private void reportSlimeImport(CommandSender sender, String fileName, String worldId, boolean template) {
        manager.importSlimeFile(manager.importPath().resolve(fileName).toFile(), worldId, template)
                .whenComplete((result, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        plugin.getLogger().severe("[SMPWorlds] Slime import failed: " + rootMessage(throwable));
                        sender.sendMessage(ChatColor.RED + "Failed to import Slime world into SMPWorlds database. Check console.");
                    } else if (!result.success()) {
                        sender.sendMessage(ChatColor.RED + result.message());
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Imported Slime world " + ChatColor.YELLOW + fileName
                                + ChatColor.GREEN + " as SMPWorld " + ChatColor.YELLOW + result.worldId()
                                + ChatColor.GREEN + ". Template: " + ChatColor.YELLOW + template);
                    }
                }));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = rootCause(throwable);
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/smpworlds dbstatus");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds list");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds info <worldId>");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds import <vanillaWorldFolder> <worldId> [template]");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds importfile <fileName> <worldId> [template]");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds importslime <fileName> <worldId> [template]");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds create <worldId> [description]");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds load <worldId>");
        sender.sendMessage(ChatColor.GOLD + "/smpworld save <worldName> [--force]");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds unload <worldId> [save]");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds clone <templateId> <newWorldId>");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds delete <worldId>");
        sender.sendMessage(ChatColor.GOLD + "/smpworlds goto <worldId>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("smpworlds.admin") && !sender.hasPermission("fakevival.smpworld.admin")) return List.of();
        if (args.length == 1) {
            return filter(List.of("dbstatus", "list", "info", "import", "importfile", "importslime", "create", "load", "save", "unload", "clone", "delete", "goto"), args[0]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("import")) return filter(List.of("true", "false"), args[3]);
        if (args.length == 4 && args[0].equalsIgnoreCase("importfile")) return filter(List.of("true", "false"), args[3]);
        if (args.length == 2 && args[0].equalsIgnoreCase("importslime")) return slimeImportFiles(args[1]);
        if (args.length == 4 && args[0].equalsIgnoreCase("importslime")) return filter(List.of("true", "false"), args[3]);
        if (args.length == 3 && args[0].equalsIgnoreCase("unload")) return filter(List.of("false", "true"), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("save")) return filter(List.of("--force"), args[2]);
        return new ArrayList<>();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase();
        return values.stream().filter(value -> value.startsWith(lower)).toList();
    }

    private List<String> slimeImportFiles(String prefix) {
        try (var files = Files.list(manager.importPath())) {
            return filter(files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".slime"))
                    .sorted().toList(), prefix);
        } catch (IOException ignored) {
            return List.of();
        }
    }
}
