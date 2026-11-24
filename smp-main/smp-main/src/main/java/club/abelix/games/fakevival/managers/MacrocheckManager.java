package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.network.SMPServerIdentity;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MacrocheckManager implements CommandExecutor, Listener {
    private static final long CHECK_MS = 60_000L;
    private static final long STAFF_COOLDOWN_MS = 10_000L;
    private final Game plugin;
    private final DataManager data;
    private final String serverId;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> staffCooldowns = new ConcurrentHashMap<>();

    public MacrocheckManager(Game plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
        this.serverId = SMPServerIdentity.resolve(plugin).serverId();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm") && sender instanceof Player target) {
            Session session = sessions.get(target.getUniqueId());
            if (session == null) { target.sendMessage(CC.color("&cYou do not have an active macro check.")); return true; }
            session.responded = true;
            finish(session, Result.PASSED);
            return true;
        }
        if (!sender.hasPermission("fakevival.staff.macrocheck")) {
            sender.sendMessage(CC.color("&cYou do not have permission to macrocheck players."));
            return true;
        }
        if (args.length == 0) { sender.sendMessage(CC.color("&cUsage: /macrocheck <player>")); return true; }
        if (args[0].equalsIgnoreCase("cancel")) return cancel(sender, args);
        if (args[0].equalsIgnoreCase("status")) return status(sender, args);
        if (args[0].equalsIgnoreCase("history")) return history(sender, args);
        if (!(sender instanceof Player staff)) { sender.sendMessage(CC.color("&cThis command requires a player staff account.")); return true; }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        if (target.equals(staff)) { sender.sendMessage(CC.color("&cYou cannot macrocheck yourself.")); return true; }
        if (sessions.containsKey(target.getUniqueId())) { sender.sendMessage(CC.color("&cThat player already has an active macrocheck.")); return true; }
        if (target.hasPermission("fakevival.staff.macrocheck.exempt") || plugin.getVanishManager().isVanished(target)) {
            sender.sendMessage(CC.color("&cYou cannot macrocheck that player.")); return true;
        }
        long now = System.currentTimeMillis();
        if (staffCooldowns.getOrDefault(staff.getUniqueId(), 0L) > now) {
            sender.sendMessage(CC.color("&cWait before starting another macrocheck.")); return true;
        }
        staffCooldowns.put(staff.getUniqueId(), now + STAFF_COOLDOWN_MS);
        Session session = new Session(UUID.randomUUID().toString(), staff, target, now);
        sessions.put(target.getUniqueId(), session);
        sender.sendMessage(CC.color("&aStarted macrocheck for &e" + target.getName() + "&a."));
        target.sendMessage(CC.color("&c&lMACRO CHECK"));
        target.sendMessage(CC.color("&7A staff member is checking if you are active."));
        target.sendMessage(CC.color("&7Please respond in chat or move normally within &e60 seconds&7."));
        target.sendTitle(CC.color("&c&lMACRO CHECK"), CC.color("&7Respond or move within 60 seconds"), 10, 70, 20);
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 0.7F);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Session active = sessions.get(target.getUniqueId());
            if (active == session) finish(session, Result.FAILED_NO_RESPONSE);
        }, 20L * 60L);
        return true;
    }

    private boolean cancel(CommandSender sender, String[] args) {
        if (args.length != 2) { sender.sendMessage(CC.color("&cUsage: /macrocheck cancel <player>")); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        Session session = target == null ? null : sessions.get(target.getUniqueId());
        if (session == null) { sender.sendMessage(CC.color("&cNo active macrocheck for that player.")); return true; }
        finish(session, Result.CANCELLED);
        sender.sendMessage(CC.color("&eMacrocheck cancelled."));
        return true;
    }

    private boolean status(CommandSender sender, String[] args) {
        if (args.length != 2) { sender.sendMessage(CC.color("&cUsage: /macrocheck status <player>")); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        Session session = target == null ? null : sessions.get(target.getUniqueId());
        if (session == null) { sender.sendMessage(CC.color("&cNo active macrocheck for that player.")); return true; }
        sendReport(sender, session, null);
        return true;
    }

    private boolean history(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.staff.macrocheck.history")) { sender.sendMessage(CC.color("&cYou do not have permission to view macrocheck history.")); return true; }
        String target = args.length > 1 ? args[1] : "";
        if (!data.isMySqlEnabled()) { sender.sendMessage(CC.color("&cMacrocheck history requires MariaDB.")); return true; }
        CompletableFuture.runAsync(() -> {
            try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "SELECT target_name,result,staff_name,ended_at FROM smp_macrocheck_logs WHERE (?='' OR target_name=?) ORDER BY ended_at DESC LIMIT 10")) {
                statement.setString(1, target); statement.setString(2, target);
                try (ResultSet rs = statement.executeQuery()) {
                    var lines = new java.util.ArrayList<String>();
                    while (rs.next()) lines.add("&8- &e" + rs.getString(1) + " &7" + rs.getString(2) + " &8by &7" + rs.getString(3) + " &8(" + rs.getLong(4) + ")");
                    Bukkit.getScheduler().runTask(plugin, () -> { sender.sendMessage(CC.color("&6Recent Macrocheck Logs")); lines.forEach(line -> sender.sendMessage(CC.color(line))); });
                }
            } catch (Exception e) { plugin.getLogger().warning("Could not load macrocheck history: " + e.getMessage()); }
        });
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || event.getTo() == null) return;
        session.rotation += Math.abs(angle(event.getFrom().getYaw(), event.getTo().getYaw())) + Math.abs(event.getFrom().getPitch() - event.getTo().getPitch());
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) return;
        session.distance += event.getFrom().distance(event.getTo());
        session.moved = session.distance >= 2D;
        session.activity = "MOVEMENT";
        if (session.moved || session.rotation >= 45D) finish(session, Result.PASSED);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        session.responded = true; session.activity = "CHAT";
        Bukkit.getScheduler().runTask(plugin, () -> finish(session, Result.PASSED));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onBreak(BlockBreakEvent e) { Session s=sessions.get(e.getPlayer().getUniqueId()); if(s!=null){s.blocks++;s.activity="MINING/FARMING";} }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onClick(PlayerInteractEvent e) { Session s=sessions.get(e.getPlayer().getUniqueId()); if(s!=null){s.clicks++;s.activity="INTERACTING";} }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onCombat(EntityDamageByEntityEvent e) { if(e.getDamager() instanceof Player p){Session s=sessions.get(p.getUniqueId());if(s!=null){s.clicks++;s.activity="COMBAT";}} }
    @EventHandler public void onQuit(PlayerQuitEvent e) { Session s=sessions.get(e.getPlayer().getUniqueId()); if(s!=null) finish(s, Result.PLAYER_LEFT); }

    private synchronized void finish(Session session, Result result) {
        if (!sessions.remove(session.targetId, session)) return;
        session.ended = System.currentTimeMillis(); session.result = result;
        Player staff = Bukkit.getPlayer(session.staffId);
        Player target = Bukkit.getPlayer(session.targetId);
        if (staff != null) {
            if (result == Result.PLAYER_LEFT) staff.sendMessage(CC.color("&c" + session.targetName + " logged out during macrocheck."));
            else if (result == Result.FAILED_NO_RESPONSE) staff.sendMessage(CC.color("&c" + session.targetName + " did not respond to the macro check."));
            else if (result == Result.PASSED) staff.sendMessage(CC.color("&a" + session.targetName + " passed the macro check."));
            sendReport(staff, session, result);
        }
        if (target != null && result == Result.PASSED) target.sendMessage(CC.color("&aYou passed the macro check."));
        persist(session);
    }

    private void sendReport(CommandSender sender, Session s, Result result) {
        sender.sendMessage(CC.color("&6Macrocheck Report: &e" + s.targetName));
        sender.sendMessage(CC.color("&7Result: " + (result == null ? "&eIN_PROGRESS" : "&e" + result)));
        sender.sendMessage(CC.color("&7Responded: " + yes(s.responded)));
        sender.sendMessage(CC.color("&7Moved: " + yes(s.moved)));
        sender.sendMessage(CC.color("&7Activity: &e" + s.activity));
        sender.sendMessage(CC.color("&7Blocks broken during check: &e" + s.blocks));
        sender.sendMessage(CC.color("&7Clicks during check: &e" + s.clicks));
        sender.sendMessage(CC.color("&7Distance moved: &e" + String.format("%.2f", s.distance)));
        sender.sendMessage(CC.color("&7Rotation changed: &e" + String.format("%.1f", s.rotation)));
    }

    private void persist(Session s) {
        if (!data.isMySqlEnabled()) return;
        CompletableFuture.runAsync(() -> {
            try (Connection c=data.getConnection(); PreparedStatement p=c.prepareStatement("INSERT INTO smp_macrocheck_logs (check_id,staff_uuid,staff_name,target_uuid,target_name,result,server_id,summary_json,started_at,ended_at) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                p.setString(1,s.id);p.setString(2,s.staffId.toString());p.setString(3,s.staffName);p.setString(4,s.targetId.toString());p.setString(5,s.targetName);p.setString(6,s.result.name());p.setString(7,serverId);
                p.setString(8,"{\"world\":\""+s.start.getWorld().getName().replace("\"", "'")+"\",\"x\":"+s.start.getX()+",\"y\":"+s.start.getY()+",\"z\":"+s.start.getZ()+",\"responded\":"+s.responded+",\"moved\":"+s.moved+",\"activity\":\""+s.activity.replace("\"", "'")+"\",\"blocks\":"+s.blocks+",\"clicks\":"+s.clicks+",\"distance\":"+s.distance+",\"rotation\":"+s.rotation+"}");
                p.setLong(9,s.started);p.setLong(10,s.ended);p.executeUpdate();
            } catch(Exception e){plugin.getLogger().warning("Could not store macrocheck log: "+e.getMessage());}
        });
    }

    private static String yes(boolean value){return value?"&aYes":"&cNo";}
    private static float angle(float a,float b){float d=(b-a)%360F;return d>180F?d-360F:d<-180F?d+360F:d;}
    private enum Result { PASSED, FAILED_NO_RESPONSE, CANCELLED, PLAYER_LEFT }
    private static final class Session {
        final String id; final UUID staffId,targetId; final String staffName,targetName; final long started; final Location start;
        volatile boolean responded,moved; volatile int blocks,clicks; volatile double distance,rotation; volatile String activity="IDLE"; volatile long ended; volatile Result result;
        Session(String id,Player staff,Player target,long started){this.id=id;staffId=staff.getUniqueId();targetId=target.getUniqueId();staffName=staff.getName();targetName=target.getName();this.started=started;start=target.getLocation().clone();}
    }
}
