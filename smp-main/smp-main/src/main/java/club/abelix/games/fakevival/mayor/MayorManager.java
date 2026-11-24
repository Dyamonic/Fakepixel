package club.abelix.games.fakevival.mayor;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.managers.PlayerLevelManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Network-backed gameplay election state. This class never grants ranks or permissions. */
public final class MayorManager implements Listener {
    public static final String DISABLED_MESSAGE = "&cMayor Elections are currently disabled.";
    public enum Phase { NONE, REGISTRATION, CAMPAIGNING, VOTING, RESULTS, ACTIVE_TERM }

    public record Perk(String id, String displayName, String description, Material icon,
                       MayorModifierType type, double amount) {}
    public record Candidate(UUID uuid, String name, int playerLevel, String perkId,
                            String campaignMessage, int votes, boolean withdrawn) {}
    public record Election(String id, Phase phase, long registrationStarts, long registrationEnds,
                           long votingStarts, long votingEnds, long termStarts, long termEnds,
                           UUID winnerUuid, String winnerName, String activePerkId) {
        static Election none() { return new Election("", Phase.NONE, 0, 0, 0, 0, 0, 0, null, "", ""); }
    }
    public record VoteLocation(String serverId, String world, double x, double y, double z, float yaw, float pitch) {}
    public record Validation(boolean database, boolean electionLoaded, boolean locationSet, int validNpcActions,
                             int invalidNpcActions, int perks, int invalidPerks, int invalidCandidates,
                             boolean timestampsValid, boolean modifierActive) {}

    private static final Map<String, Perk> PERKS = createPerks();
    private final Game plugin;
    private final DataManager data;
    private final PlayerLevelManager levels;
    private final NamespacedKey perkMenuKey;
    private final NamespacedKey candidateMenuKey;
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Election election = Election.none();
    private volatile List<Candidate> candidates = List.of();
    private volatile VoteLocation voteLocation;
    private volatile long lastRefresh;
    private volatile long nextDbWarning;
    private BukkitTask refreshTask;

    public MayorManager(Game plugin, DataManager data, PlayerLevelManager levels) {
        this.plugin = plugin;
        this.data = data;
        this.levels = levels;
        this.perkMenuKey = new NamespacedKey(plugin, "mayor_perk_id");
        this.candidateMenuKey = new NamespacedKey(plugin, "mayor_candidate_uuid");
    }

    public void start() {
        if (!isMayorEnabled()) {
            election = Election.none();
            candidates = List.of();
            plugin.getLogger().info("[SMP Mayor] Mayor Elections are disabled.");
            return;
        }
        seedPerks();
        refreshNow();
        long period = Math.max(100L, RuntimeSettings.config(plugin).getLong("mayor.cacheRefreshSeconds", 5L) * 20L);
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshNow, period, period);
    }

    public void stop() { if (refreshTask != null) refreshTask.cancel(); refreshTask = null; }
    public Election election() { return election; }
    public List<Candidate> candidates() { return candidates; }
    public Perk perk(String id) { return id == null ? null : PERKS.get(id.toLowerCase(Locale.ROOT)); }
    public Map<String, Perk> perks() { return Map.copyOf(PERKS); }
    public long lastRefresh() { return lastRefresh; }
    public int requiredLevel() { return Math.max(1, RuntimeSettings.config(plugin).getInt("mayor.requiredPlayerLevel", 50)); }
    public boolean isMayorEnabled() { return RuntimeSettings.config(plugin).getBoolean("mayor.enabled", false); }

    public void refreshNow() {
        if (!isMayorEnabled()) {
            election = Election.none();
            candidates = List.of();
            return;
        }
        if (!data.isMySqlEnabled() || !refreshing.compareAndSet(false, true)) return;
        async(() -> {
            loadState();
            advancePhase();
            loadState();
            return null;
        }).whenComplete((ignored, error) -> {
            refreshing.set(false);
            if (error != null) warn("Could not refresh network election cache", error);
        });
    }

    private void loadState() throws SQLException {
        Election loaded = Election.none();
        List<Candidate> loadedCandidates = new ArrayList<>();
        VoteLocation loadedLocation = null;
        try (Connection c = data.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT * FROM smp_mayor_elections ORDER BY updated_at DESC LIMIT 1");
             ResultSet r = s.executeQuery()) {
            if (r.next()) loaded = readElection(r);
        }
        if (!loaded.id().isBlank()) {
            try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                    "SELECT * FROM smp_mayor_candidates WHERE election_id=? ORDER BY votes DESC,registered_at ASC")) {
                s.setString(1, loaded.id());
                try (ResultSet r = s.executeQuery()) {
                    while (r.next()) loadedCandidates.add(new Candidate(UUID.fromString(r.getString("player_uuid")),
                            r.getString("player_name"), r.getInt("player_level"), r.getString("perk_id"),
                            r.getString("campaign_message"), r.getInt("votes"), r.getBoolean("withdrawn")));
                }
            }
        }
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT * FROM smp_mayor_locations WHERE id='voting' LIMIT 1"); ResultSet r = s.executeQuery()) {
            if (r.next()) loadedLocation = new VoteLocation(r.getString("server_id"), r.getString("world"),
                    r.getDouble("x"), r.getDouble("y"), r.getDouble("z"), r.getFloat("yaw"), r.getFloat("pitch"));
        }
        election = loaded;
        candidates = List.copyOf(loadedCandidates);
        voteLocation = loadedLocation;
        lastRefresh = System.currentTimeMillis();
    }

    private Election readElection(ResultSet r) throws SQLException {
        String winner = r.getString("winner_uuid");
        Phase phase;
        try { phase = Phase.valueOf(r.getString("phase")); } catch (Exception ignored) { phase = Phase.NONE; }
        return new Election(r.getString("election_id"), phase, r.getLong("registration_starts_at"),
                r.getLong("registration_ends_at"), r.getLong("voting_starts_at"), r.getLong("voting_ends_at"),
                r.getLong("term_starts_at"), r.getLong("term_ends_at"),
                winner == null || winner.isBlank() ? null : UUID.fromString(winner), r.getString("winner_name"),
                r.getString("active_perk_id"));
    }

    private void advancePhase() throws SQLException {
        Election e = election;
        if (e.id().isBlank()) return;
        long now = System.currentTimeMillis();
        if (e.phase() == Phase.REGISTRATION && now >= e.registrationEnds()) updatePhase(e.id(), Phase.CAMPAIGNING);
        else if (e.phase() == Phase.CAMPAIGNING && now >= e.votingStarts()) updatePhase(e.id(), Phase.VOTING);
        else if (e.phase() == Phase.VOTING && now >= e.votingEnds()) finalizeElection(e.id(), now);
        else if (e.phase() == Phase.RESULTS && now >= e.termStarts()) updatePhase(e.id(), Phase.ACTIVE_TERM);
        else if (e.phase() == Phase.ACTIVE_TERM && now >= e.termEnds()) updatePhase(e.id(), Phase.NONE);
    }

    private void updatePhase(String id, Phase phase) throws SQLException {
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "UPDATE smp_mayor_elections SET phase=?,updated_at=? WHERE election_id=?")) {
            s.setString(1, phase.name()); s.setLong(2, System.currentTimeMillis()); s.setString(3, id); s.executeUpdate();
        }
    }

    public void startRegistration(Player actor) {
        if (!enabled(actor)) return;
        long now = System.currentTimeMillis();
        long reg = seconds("mayor.registrationSeconds", 86_400L), campaign = seconds("mayor.campaigningSeconds", 86_400L);
        long voting = seconds("mayor.votingSeconds", 86_400L), results = seconds("mayor.resultsSeconds", 600L);
        long term = seconds("mayor.termSeconds", 604_800L);
        String id = "mayor_" + now;
        async(() -> {
            try (Connection c = data.getConnection(); PreparedStatement clear = c.prepareStatement(
                    "UPDATE smp_mayor_elections SET phase='NONE',updated_at=? WHERE phase<>'NONE'")) {
                clear.setLong(1, now); clear.executeUpdate();
            }
            try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                    "INSERT INTO smp_mayor_elections VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                long regEnd = now + reg * 1000L, voteStart = regEnd + campaign * 1000L;
                long voteEnd = voteStart + voting * 1000L, termStart = voteEnd + results * 1000L;
                s.setString(1, id); s.setString(2, Phase.REGISTRATION.name()); s.setLong(3, now); s.setLong(4, regEnd);
                s.setLong(5, voteStart); s.setLong(6, voteEnd); s.setLong(7, termStart); s.setLong(8, termStart + term * 1000L);
                s.setNull(9, java.sql.Types.VARCHAR); s.setNull(10, java.sql.Types.VARCHAR); s.setNull(11, java.sql.Types.VARCHAR);
                s.setLong(12, now); s.setLong(13, now); s.executeUpdate();
            }
            audit(id, actor, "START_REGISTRATION", "registration opened"); return null;
        }).thenRun(() -> sync(() -> { refreshNow(); Bukkit.broadcastMessage(CC.color("&eMayor Election registration is now open!")); }));
    }

    public void openInfo(Player player) {
        if (!enabled(player)) return;
        Inventory inv = Bukkit.createInventory(new MenuHolder("mayor:info"), 45, CC.color("&8SMP Mayor"));
        fill(inv);
        Election e = election;
        inv.setItem(13, item(Material.GOLDEN_HELMET, "&6Current Mayor", List.of(
                "&7Mayor: &e" + (e.winnerName() == null || e.winnerName().isBlank() ? "None" : e.winnerName()),
                "&7Perk: &b" + perkName(e.activePerkId()), "&7Time Left: &f" + timeLeft(termOrPhaseEnd(e) - System.currentTimeMillis()))));
        inv.setItem(20, item(Material.CLOCK, "&bElection Status", List.of("&7Phase: &e" + pretty(e.phase().name()),
                "&7Time Left: &f" + phaseTimeLeft(), "&7Candidates: &e" + activeCandidates().size())));
        inv.setItem(22, item(Material.WRITABLE_BOOK, "&aRun for Mayor", List.of("&7Required PlayerLevel: &e" + requiredLevel(), "", "&eClick to choose a campaign perk.")));
        inv.setItem(24, item(Material.PLAYER_HEAD, "&aCandidates", candidateSummary()));
        if (voteLocation != null) inv.setItem(31, item(Material.COMPASS, "&eVoting Location", List.of("&7Server: &f" + voteLocation.serverId(), "&7World: &f" + voteLocation.world(), "&7Coordinates: &f" + (int)voteLocation.x() + ", " + (int)voteLocation.y() + ", " + (int)voteLocation.z())));
        inv.setItem(40, item(Material.BARRIER, "&cClose", List.of()));
        player.openInventory(inv);
    }

    public void openPerkMenu(Player player) {
        if (!enabled(player)) return;
        if (election.phase() != Phase.REGISTRATION) { player.sendMessage(CC.color("&cCandidate registration is not open.")); return; }
        if (levels.getLevel(player) < requiredLevel()) { player.sendMessage(CC.color("&cYou need Player Level " + requiredLevel() + " to run for Mayor.")); return; }
        Inventory inv = Bukkit.createInventory(new MenuHolder("mayor:perks"), 27, CC.color("&8Choose Mayor Perk")); fill(inv);
        int slot = 9;
        for (Perk perk : PERKS.values()) inv.setItem(slot++, taggedItem(perk.icon(), perk.displayName(),
                List.of(perk.description(), "", "&eClick to run with this perk!"), perkMenuKey, perk.id()));
        player.openInventory(inv);
    }

    public void openVoting(Player player, boolean fromNpc) {
        if (!enabled(player)) return;
        if (election.phase() != Phase.VOTING) { player.sendMessage(CC.color("&cVoting is not currently open.")); return; }
        if (!fromNpc && !RuntimeSettings.config(plugin).getBoolean("mayor.allowCommandVoting", false)) {
            player.sendMessage(CC.color("&cCast your vote at the Ballot Clerk NPC.")); return;
        }
        Inventory inv = Bukkit.createInventory(new MenuHolder("mayor:vote"), 54, CC.color("&8Mayor Election")); fill(inv);
        int slot = 10;
        for (Candidate candidate : activeCandidates()) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Candidate: &e" + candidate.name()); lore.add("&7Player Level: &a" + candidate.playerLevel());
            lore.add("&7Perk: &b" + perkName(candidate.perkId())); lore.add(""); lore.add("&fCampaign:");
            lore.add("&7\"" + (candidate.campaignMessage().isBlank() ? "No campaign message." : candidate.campaignMessage()) + "\"");
            lore.add(""); lore.add("&7Votes: &e" + candidate.votes()); lore.add(""); lore.add("&eClick to vote!");
            inv.setItem(slot, taggedItem(Material.PLAYER_HEAD, "&e" + candidate.name(), lore,
                    candidateMenuKey, candidate.uuid().toString()));
            slot++; if (slot % 9 == 8) slot += 2; if (slot >= 44) break;
        }
        player.openInventory(inv);
    }

    public void openResults(Player player) { if (enabled(player)) openInfo(player); }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder) || !holder.type().startsWith("mayor:")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isMayorEnabled()) {
            player.closeInventory();
            player.sendMessage(CC.color(DISABLED_MESSAGE));
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        String type = holder.type();
        if (type.equals("mayor:info") && event.getRawSlot() == 22) { openPerkMenu(player); return; }
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (type.equals("mayor:perks")) {
            String id = clicked.getItemMeta().getPersistentDataContainer().get(perkMenuKey, PersistentDataType.STRING);
            if (id != null) registerCandidate(player, id);
        } else if (type.equals("mayor:vote")) {
            String id = clicked.getItemMeta().getPersistentDataContainer().get(candidateMenuKey, PersistentDataType.STRING);
            if (id != null) { try { castVote(player, UUID.fromString(id)); } catch (IllegalArgumentException ignored) {} }
        }
    }

    public void registerCandidate(Player player, String perkId) {
        if (!enabled(player)) return;
        Perk perk = perk(perkId);
        if (perk == null || election.phase() != Phase.REGISTRATION || levels.getLevel(player) < requiredLevel()) return;
        String electionId = election.id();
        async(() -> {
            if (isElectionBanned(player.getUniqueId())) return -1;
            try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                    "INSERT IGNORE INTO smp_mayor_candidates(election_id,player_uuid,player_name,player_level,perk_id,campaign_message,registered_at,withdrawn,votes) VALUES(?,?,?,?,?,'',?,FALSE,0)")) {
                s.setString(1, electionId); s.setString(2, player.getUniqueId().toString()); s.setString(3, player.getName());
                s.setInt(4, levels.getLevel(player)); s.setString(5, perk.id()); s.setLong(6, System.currentTimeMillis());
                boolean added = s.executeUpdate() == 1;
                if (added) audit(electionId, player, "REGISTER", perk.id());
                return added ? 1 : 0;
            }
        }).thenAccept(result -> sync(() -> {
            if (result < 0) player.sendMessage(CC.color("&cYou are banned from mayor elections."));
            else if (result > 0) { player.closeInventory(); player.sendMessage(CC.color("&aYou are now running for Mayor with perk: " + perk.displayName() + "&a.")); refreshNow(); }
            else player.sendMessage(CC.color("&cYou are already registered in this election."));
        }));
    }

    public void setCampaign(Player player, String message) {
        if (!enabled(player)) return;
        if (!Set.of(Phase.REGISTRATION, Phase.CAMPAIGNING).contains(election.phase())) { player.sendMessage(CC.color("&cCampaigning is closed.")); return; }
        String clean = sanitizeCampaign(player, message);
        async(() -> {
            try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                    "UPDATE smp_mayor_candidates SET campaign_message=? WHERE election_id=? AND player_uuid=? AND withdrawn=FALSE")) {
                s.setString(1, clean); s.setString(2, election.id()); s.setString(3, player.getUniqueId().toString());
                boolean updated = s.executeUpdate() == 1; if (updated) audit(election.id(), player, "CAMPAIGN", clean); return updated;
            }
        }).thenAccept(ok -> sync(() -> { player.sendMessage(CC.color(ok ? "&aCampaign message updated." : "&cYou are not an active candidate.")); refreshNow(); }));
    }

    public void withdraw(Player player) {
        if (!enabled(player)) return;
        if (election.phase() != Phase.REGISTRATION) { player.sendMessage(CC.color("&cYou can only withdraw during registration.")); return; }
        async(() -> {
            try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                    "UPDATE smp_mayor_candidates SET withdrawn=TRUE WHERE election_id=? AND player_uuid=? AND withdrawn=FALSE")) {
                s.setString(1, election.id()); s.setString(2, player.getUniqueId().toString()); boolean ok = s.executeUpdate() == 1;
                if (ok) audit(election.id(), player, "WITHDRAW", "candidate withdrew"); return ok;
            }
        }).thenAccept(ok -> sync(() -> { player.sendMessage(CC.color(ok ? "&eYou withdrew from the Mayor Election." : "&cYou are not an active candidate.")); refreshNow(); }));
    }

    public void castVote(Player voter, UUID candidate) {
        if (!enabled(voter)) return;
        if (election.phase() != Phase.VOTING) return;
        if (!RuntimeSettings.config(plugin).getBoolean("mayor.allowSelfVote", false) && voter.getUniqueId().equals(candidate)) {
            voter.sendMessage(CC.color("&cYou cannot vote for yourself.")); return;
        }
        String electionId = election.id();
        async(() -> {
            try (Connection c = data.getConnection()) {
                c.setAutoCommit(false);
                try {
                    try (PreparedStatement check = c.prepareStatement("SELECT 1 FROM smp_mayor_candidates WHERE election_id=? AND player_uuid=? AND withdrawn=FALSE")) {
                        check.setString(1, electionId); check.setString(2, candidate.toString());
                        try (ResultSet r = check.executeQuery()) { if (!r.next()) { c.rollback(); return false; } }
                    }
                    try (PreparedStatement vote = c.prepareStatement("INSERT INTO smp_mayor_votes VALUES(?,?,?,?)")) {
                        vote.setString(1, electionId); vote.setString(2, voter.getUniqueId().toString()); vote.setString(3, candidate.toString()); vote.setLong(4, System.currentTimeMillis()); vote.executeUpdate();
                    }
                    try (PreparedStatement count = c.prepareStatement("UPDATE smp_mayor_candidates SET votes=votes+1 WHERE election_id=? AND player_uuid=?")) {
                        count.setString(1, electionId); count.setString(2, candidate.toString()); count.executeUpdate();
                    }
                    c.commit(); audit(electionId, voter, "VOTE", candidate.toString()); return true;
                } catch (SQLException ex) { c.rollback(); if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) return false; throw ex; }
                finally { c.setAutoCommit(true); }
            }
        }).thenAccept(ok -> sync(() -> {
            if (ok) {
                String name = candidates.stream().filter(c -> c.uuid().equals(candidate)).map(Candidate::name).findFirst().orElse("candidate");
                candidates = candidates.stream().map(c -> c.uuid().equals(candidate)
                        ? new Candidate(c.uuid(), c.name(), c.playerLevel(), c.perkId(), c.campaignMessage(), c.votes() + 1, c.withdrawn()) : c).toList();
                voter.sendMessage(CC.color("&aYou voted for &e" + name + "&a.")); refreshNow(); openVoting(voter, true);
            } else { voter.sendMessage(CC.color("&cYou already voted, or that candidate is unavailable.")); voter.closeInventory(); }
        }));
    }

    public void startVoting(Player actor) { if (enabled(actor)) setPhaseAdmin(actor, Phase.VOTING, "START_VOTING"); }
    public void endVoting(Player actor) { if (!enabled(actor)) return; async(() -> { finalizeElection(election.id(), System.currentTimeMillis()); audit(election.id(), actor, "END_VOTING", "manual"); return null; }).thenRun(this::refreshNow); }

    private void finalizeElection(String electionId, long now) throws SQLException {
        Candidate winner = null;
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT * FROM smp_mayor_candidates WHERE election_id=? AND withdrawn=FALSE ORDER BY votes DESC,registered_at ASC LIMIT 1")) {
            s.setString(1, electionId); try (ResultSet r = s.executeQuery()) { if (r.next()) winner = new Candidate(UUID.fromString(r.getString("player_uuid")), r.getString("player_name"), r.getInt("player_level"), r.getString("perk_id"), r.getString("campaign_message"), r.getInt("votes"), false); }
        }
        if (winner == null) { updatePhase(electionId, Phase.NONE); return; }
        long resultMs = seconds("mayor.resultsSeconds", 600L) * 1000L, termMs = seconds("mayor.termSeconds", 604_800L) * 1000L;
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "UPDATE smp_mayor_elections SET phase='RESULTS',term_starts_at=?,term_ends_at=?,winner_uuid=?,winner_name=?,active_perk_id=?,updated_at=? WHERE election_id=? AND phase IN ('VOTING','CAMPAIGNING','REGISTRATION')")) {
            s.setLong(1, now + resultMs); s.setLong(2, now + resultMs + termMs);
            if (winner == null) { s.setNull(3, java.sql.Types.VARCHAR); s.setNull(4, java.sql.Types.VARCHAR); s.setNull(5, java.sql.Types.VARCHAR); }
            else { s.setString(3, winner.uuid().toString()); s.setString(4, winner.name()); s.setString(5, winner.perkId()); }
            s.setLong(6, now); s.setString(7, electionId); if (s.executeUpdate() == 1 && winner != null) {
                Candidate finalWinner = winner;
                sync(() -> Bukkit.broadcastMessage(CC.color("&6" + finalWinner.name() + " &ehas won the Mayor Election! &aMayor perk active soon: &b" + perkName(finalWinner.perkId()))));
            }
        }
    }

    public void forceWinner(Player actor, OfflinePlayer target) {
        if (!enabled(actor)) return;
        Candidate candidate = candidates.stream().filter(c -> c.uuid().equals(target.getUniqueId()) && !c.withdrawn()).findFirst().orElse(null);
        if (candidate == null) { actor.sendMessage(CC.color("&cThat player is not an active candidate.")); return; }
        long now = System.currentTimeMillis(), resultMs = seconds("mayor.resultsSeconds", 600L) * 1000L, termMs = seconds("mayor.termSeconds", 604_800L) * 1000L;
        async(() -> { try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE smp_mayor_elections SET phase='RESULTS',winner_uuid=?,winner_name=?,active_perk_id=?,term_starts_at=?,term_ends_at=?,updated_at=? WHERE election_id=?")) {
            s.setString(1, candidate.uuid().toString()); s.setString(2, candidate.name()); s.setString(3, candidate.perkId()); s.setLong(4, now + resultMs); s.setLong(5, now + resultMs + termMs); s.setLong(6, now); s.setString(7, election.id()); s.executeUpdate(); }
            audit(election.id(), actor, "FORCE_WINNER", candidate.uuid().toString()); return null; }).thenRun(this::refreshNow);
    }

    public void cancel(Player actor) { if (enabled(actor)) setPhaseAdmin(actor, Phase.NONE, "CANCEL"); }
    private void setPhaseAdmin(Player actor, Phase phase, String action) {
        async(() -> { updatePhase(election.id(), phase); audit(election.id(), actor, action, phase.name()); return null; }).thenRun(this::refreshNow);
    }

    public void setVoteLocation(Player actor) {
        if (!enabled(actor)) return;
        Location l = actor.getLocation(); String server = plugin.getNetworkService().getCurrentServerId();
        async(() -> { try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO smp_mayor_locations VALUES('voting',?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE server_id=VALUES(server_id),world=VALUES(world),x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch),updated_at=VALUES(updated_at)")) {
            s.setString(1, server); s.setString(2, l.getWorld().getName()); s.setDouble(3, l.getX()); s.setDouble(4, l.getY()); s.setDouble(5, l.getZ()); s.setFloat(6, l.getYaw()); s.setFloat(7, l.getPitch()); s.setLong(8, System.currentTimeMillis());
            s.executeUpdate(); }
            audit(election.id(), actor, "SET_VOTE_LOCATION", server + ":" + l.getWorld().getName()); return null; }).thenRun(() -> sync(() -> { actor.sendMessage(CC.color("&aMayor voting location saved network-wide.")); refreshNow(); }));
    }

    public void spawnNpc(Player actor, String kind) {
        if (!enabled(actor)) return;
        String normalized = kind.toLowerCase(Locale.ROOT);
        String id = switch (normalized) { case "register" -> "mayor_registration_npc"; case "vote" -> "mayor_vote_npc"; case "results" -> "mayor_results_npc"; default -> null; };
        if (id == null) { actor.sendMessage(CC.color("&cUse register, vote, or results.")); return; }
        String name = switch (normalized) { case "register" -> "&eElection Clerk"; case "vote" -> "&aBallot Clerk"; default -> "&6Mayor Results"; };
        String action = "mayor:" + normalized;
        if (!plugin.getNpcManager().createNpc(id, name, actor.getName(), actor.getLocation())) {
            plugin.getNpcManager().teleportNpc(id, actor.getLocation()); plugin.getNpcManager().setName(id, name);
        }
        plugin.getNpcManager().setAction(id, action);
        actor.sendMessage(CC.color("&aElection NPC ready: &f" + id + " &7(" + action + ")"));
        audit(election.id(), actor, "SPAWN_NPC", id);
    }

    public boolean handleNpcAction(Player player, String action) {
        String lower = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        if (Set.of("mayor:register", "mayor:vote", "mayor:results").contains(lower) && !isMayorEnabled()) {
            player.sendMessage(CC.color(DISABLED_MESSAGE));
            return true;
        }
        return switch (lower) {
            case "mayor:register" -> { openPerkMenu(player); yield true; }
            case "mayor:vote" -> {
                if (!isAtVotingLocation(player)) player.sendMessage(CC.color("&cThis Ballot Clerk is not at the configured voting area."));
                else openVoting(player, true);
                yield true;
            }
            case "mayor:results" -> { openResults(player); yield true; }
            default -> false;
        };
    }

    public double applyMayorModifier(Player player, MayorModifierType type, double baseValue) {
        if (!isMayorEnabled()) return baseValue;
        Election e = election;
        if (e.phase() != Phase.ACTIVE_TERM || e.termEnds() <= System.currentTimeMillis()) return baseValue;
        Perk active = perk(e.activePerkId()); if (active == null) return baseValue;
        boolean applies = active.type() == type || active.id().equals("soft_debuff_challenge") && type == MayorModifierType.REWARDS;
        if (!applies) return baseValue;
        if (type == MayorModifierType.MOB_DAMAGE && player != null) {
            if (levels.getLevel(player) < 10 || plugin.getNetworkService().isHub()
                    || plugin.getZoneManager().isProtectedSafeZone(player.getLocation())) return baseValue;
        }
        double amount = active.id().equals("soft_debuff_challenge") && type == MayorModifierType.REWARDS ? 0.10D : active.amount();
        return baseValue * (1.0D + amount);
    }

    public String placeholder(String placeholder) {
        if (!isMayorEnabled()) {
            return switch (placeholder.toLowerCase(Locale.ROOT)) {
                case "%smp_mayor_name%", "smp_mayor_name" -> "None";
                case "%smp_mayor_perk%", "smp_mayor_perk",
                     "%smp_mayor_time_left%", "smp_mayor_time_left",
                     "%smp_election_phase%", "smp_election_phase",
                     "%smp_election_time_left%", "smp_election_time_left" -> "Disabled";
                case "%smp_election_candidate_count%", "smp_election_candidate_count" -> "0";
                default -> placeholder;
            };
        }
        return switch (placeholder.toLowerCase(Locale.ROOT)) {
            case "%smp_mayor_name%", "smp_mayor_name" -> election.winnerName() == null || election.winnerName().isBlank() ? "None" : election.winnerName();
            case "%smp_mayor_perk%", "smp_mayor_perk" -> perkName(election.activePerkId());
            case "%smp_mayor_time_left%", "smp_mayor_time_left" -> timeLeft(election.termEnds() - System.currentTimeMillis());
            case "%smp_election_phase%", "smp_election_phase" -> pretty(election.phase().name());
            case "%smp_election_time_left%", "smp_election_time_left" -> phaseTimeLeft();
            case "%smp_election_candidate_count%", "smp_election_candidate_count" -> String.valueOf(activeCandidates().size());
            default -> placeholder;
        };
    }

    public Validation validate() {
        int validActions = 0, invalidActions = 0;
        for (String id : List.of("mayor_registration_npc", "mayor_vote_npc", "mayor_results_npc")) {
            if (!plugin.getNpcManager().getNpcIds().contains(id)) continue;
            String action = plugin.getNpcManager().getEffectiveNpcAction(id);
            if (Set.of("mayor:register", "mayor:vote", "mayor:results").contains(action.toLowerCase(Locale.ROOT))) validActions++; else invalidActions++;
        }
        int invalidPerks = (int) PERKS.values().stream().filter(p -> p.displayName().isBlank() || p.description().isBlank() || p.type() == null).count();
        int invalidCandidates = (int) candidates.stream().filter(c -> perk(c.perkId()) == null).count();
        Election e = election;
        boolean timestamps = e.phase() == Phase.NONE || e.registrationStarts() <= e.registrationEnds()
                && e.registrationEnds() <= e.votingStarts() && e.votingStarts() <= e.votingEnds()
                && e.termStarts() <= e.termEnds();
        return new Validation(data.isMySqlEnabled(), !e.id().isBlank(), voteLocation != null, validActions, invalidActions,
                PERKS.size(), invalidPerks, invalidCandidates, timestamps,
                e.phase() != Phase.ACTIVE_TERM || perk(e.activePerkId()) != null);
    }

    public String debugLine() { return !isMayorEnabled() ? "Mayor Elections: Disabled" : "election=" + election.id() + " phase=" + election.phase() + " mayor=" + election.winnerName() + " perk=" + election.activePerkId() + " candidates=" + activeCandidates().size() + " dbRefreshAge=" + (System.currentTimeMillis() - lastRefresh) + "ms"; }
    public String phaseTimeLeft() { return !isMayorEnabled() ? "Disabled" : timeLeft(termOrPhaseEnd(election) - System.currentTimeMillis()); }
    public VoteLocation voteLocation() { return voteLocation; }

    public boolean isAtVotingLocation(Player player) {
        VoteLocation target = voteLocation;
        if (player == null || target == null || !plugin.getNetworkService().getCurrentServerId().equalsIgnoreCase(target.serverId())
                || !player.getWorld().getName().equalsIgnoreCase(target.world())) return false;
        double dx = player.getLocation().getX() - target.x(), dy = player.getLocation().getY() - target.y(), dz = player.getLocation().getZ() - target.z();
        return dx * dx + dy * dy + dz * dz <= 144.0D;
    }

    private long termOrPhaseEnd(Election e) { return switch (e.phase()) { case REGISTRATION -> e.registrationEnds(); case CAMPAIGNING -> e.votingStarts(); case VOTING -> e.votingEnds(); case RESULTS -> e.termStarts(); case ACTIVE_TERM -> e.termEnds(); default -> 0L; }; }
    private boolean enabled(Player player) {
        if (isMayorEnabled()) return true;
        if (player != null) player.sendMessage(CC.color(DISABLED_MESSAGE));
        return false;
    }
    private List<Candidate> activeCandidates() { return candidates.stream().filter(c -> !c.withdrawn()).toList(); }
    private String perkName(String id) { Perk p = perk(id); return p == null ? "None" : ChatColor.stripColor(CC.color(p.displayName())); }
    private boolean isElectionBanned(UUID uuid) {
        if (RuntimeSettings.config(plugin).getBoolean("mayor.banned." + uuid, false)) return true;
        if (!data.isMySqlEnabled()) return false;
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement("SELECT 1 FROM smp_mayor_bans WHERE player_uuid=?")) {
            s.setString(1, uuid.toString()); try (ResultSet r = s.executeQuery()) { return r.next(); }
        } catch (SQLException error) { warn("Could not check election ban", error); return true; }
    }

    public void setElectionBan(Player actor, OfflinePlayer target, boolean banned, String reason) {
        if (!enabled(actor)) return;
        async(() -> {
            try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(banned
                    ? "INSERT INTO smp_mayor_bans VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name),reason=VALUES(reason),banned_by=VALUES(banned_by),created_at=VALUES(created_at)"
                    : "DELETE FROM smp_mayor_bans WHERE player_uuid=?")) {
                s.setString(1, target.getUniqueId().toString());
                if (banned) { s.setString(2, target.getName() == null ? "Unknown" : target.getName()); s.setString(3, sanitizePlain(reason, 160)); s.setString(4, actor.getUniqueId().toString()); s.setLong(5, System.currentTimeMillis()); }
                s.executeUpdate();
            }
            audit(election.id(), actor, banned ? "BAN" : "UNBAN", target.getUniqueId().toString()); return null;
        }).thenRun(() -> sync(() -> actor.sendMessage(CC.color(banned ? "&cPlayer banned from Mayor Elections." : "&aElection ban removed."))));
    }
    private long seconds(String path, long fallback) { return Math.max(1L, RuntimeSettings.config(plugin).getLong(path, fallback)); }

    private String sanitizeCampaign(Player player, String input) {
        String value = input == null ? "" : input.trim();
        if (!player.hasPermission("fakevival.mayor.admin")) value = ChatColor.stripColor(CC.color(value));
        value = value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").replace('\n', ' ').replace('\r', ' ').trim();
        return value.substring(0, Math.min(160, value.length()));
    }
    private String sanitizePlain(String input, int limit) { String value = ChatColor.stripColor(CC.color(input == null ? "" : input)); value = value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ").trim(); return value.substring(0, Math.min(limit, value.length())); }

    private void seedPerks() {
        if (!data.isMySqlEnabled()) return;
        async(() -> { try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO smp_mayor_effects(perk_id,display_name,description,effect_type,amount,enabled) VALUES(?,?,?,?,?,TRUE) ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),description=VALUES(description),effect_type=VALUES(effect_type),amount=VALUES(amount)")) {
            for (Perk p : PERKS.values()) { s.setString(1, p.id()); s.setString(2, p.displayName()); s.setString(3, p.description()); s.setString(4, p.type().name()); s.setDouble(5, p.amount()); s.addBatch(); } s.executeBatch(); } return null; });
    }

    private void audit(String electionId, Player actor, String action, String details) {
        if (!data.isMySqlEnabled()) return;
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement("INSERT INTO smp_mayor_audit(election_id,actor_uuid,action,details,created_at) VALUES(?,?,?,?,?)")) {
            s.setString(1, electionId == null ? "" : electionId); s.setString(2, actor == null ? "SYSTEM" : actor.getUniqueId().toString()); s.setString(3, action); s.setString(4, details == null ? "" : details); s.setLong(5, System.currentTimeMillis()); s.executeUpdate();
        } catch (SQLException error) { warn("Could not write mayor audit", error); }
    }

    private <T> CompletableFuture<T> async(SqlSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> { try { future.complete(supplier.get()); } catch (Throwable error) { future.completeExceptionally(error); } });
        return future;
    }
    private void sync(Runnable runnable) { if (Bukkit.isPrimaryThread()) runnable.run(); else Bukkit.getScheduler().runTask(plugin, runnable); }
    private void warn(String message, Throwable error) { long now = System.currentTimeMillis(); if (now >= nextDbWarning) { nextDbWarning = now + 60_000L; plugin.getLogger().warning("[SMP Mayor] " + message + ": " + error.getMessage()); } }

    private ItemStack item(Material material, String name, List<String> lore) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(CC.color(name)); meta.setLore(lore.stream().map(CC::color).toList()); item.setItemMeta(meta); return item; }
    private ItemStack taggedItem(Material material, String name, List<String> lore, NamespacedKey key, String value) { ItemStack item = item(material, name, lore); ItemMeta meta = item.getItemMeta(); meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value); item.setItemMeta(meta); return item; }
    private void fill(Inventory inv) { ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()); for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane); }
    private List<String> candidateSummary() { List<String> lines = new ArrayList<>(); for (Candidate c : activeCandidates()) lines.add("&7- &e" + c.name() + " &7(&b" + perkName(c.perkId()) + "&7)"); if (lines.isEmpty()) lines.add("&7No candidates registered."); return lines; }
    private String pretty(String raw) { StringBuilder out = new StringBuilder(); for (String part : raw.toLowerCase(Locale.ROOT).split("_")) { if (!out.isEmpty()) out.append(' '); out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)); } return out.toString(); }
    public static String timeLeft(long millis) { if (millis <= 0) return "0s"; long seconds = millis / 1000L, days = seconds / 86400L; seconds %= 86400L; long hours = seconds / 3600L; seconds %= 3600L; long minutes = seconds / 60L; seconds %= 60L; if (days > 0) return days + "d " + hours + "h"; if (hours > 0) return hours + "h " + minutes + "m"; if (minutes > 0) return minutes + "m " + seconds + "s"; return seconds + "s"; }

    private static Map<String, Perk> createPerks() {
        Map<String, Perk> map = new LinkedHashMap<>();
        add(map, "farming_xp_boost", "&aFarming Festival", "&7All players gain &a+10% Farming XP&7.", Material.WHEAT, MayorModifierType.FARMING_XP, .10);
        add(map, "mining_xp_boost", "&bMiner's Mandate", "&7All players gain &b+10% Mining XP&7.", Material.DIAMOND_PICKAXE, MayorModifierType.MINING_XP, .10);
        add(map, "fishing_luck_boost", "&3Fishing Boom", "&7All players gain &3+5% Fishing Luck&7.", Material.FISHING_ROD, MayorModifierType.FISHING_LUCK, .05);
        add(map, "mob_coin_boost", "&6Bounty Tax", "&7Mobs drop &6+5% Coins&7.", Material.GOLD_NUGGET, MayorModifierType.MOB_COINS, .05);
        add(map, "shop_discount", "&eMarket Relief", "&7NPC shops are &e5% cheaper&7.", Material.EMERALD, MayorModifierType.SHOP_PRICE, -.05);
        add(map, "auction_fee_reduce", "&dTrade Reform", "&7Auction/Tradepost fees are &d5% lower&7.", Material.CHEST, MayorModifierType.AUCTION_FEE, -.05);
        add(map, "dungeon_xp_boost", "&cDungeon Rally", "&7Dungeon mobs and bosses give &c+8% XP&7.", Material.WITHER_SKELETON_SKULL, MayorModifierType.DUNGEON_XP, .08);
        add(map, "event_token_boost", "&5Festival Mayor", "&7Events give &5+10% Event Tokens&7.", Material.AMETHYST_SHARD, MayorModifierType.EVENT_TOKENS, .10);
        add(map, "soft_debuff_challenge", "&4Hard Times", "&7Mobs gain &c+5% damage&7, but rewards increase by &a+10%&7.", Material.IRON_SWORD, MayorModifierType.MOB_DAMAGE, .05);
        return map;
    }
    private static void add(Map<String, Perk> map, String id, String name, String description, Material icon, MayorModifierType type, double amount) { map.put(id, new Perk(id, name, description, icon, type, amount)); }
    @FunctionalInterface private interface SqlSupplier<T> { T get() throws Exception; }
}
