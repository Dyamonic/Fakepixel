package club.abelix.games.fakevival.data;

import club.abelix.games.fakevival.config.DeploymentConfig;
import club.abelix.games.fakevival.registries.DatabaseRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DataManager {
    private final JavaPlugin plugin;
    private FileConfiguration data;
    private HikariDataSource dataSource;
    private boolean mysqlEnabled;
    private String mysqlTable;
    private String liveControlsTable;
    private String sqlStorageName;
    private final Object saveLock = new Object();
    private volatile long nextSaveErrorLogAt;
    private volatile long nextGuildSaveErrorLogAt;
    private volatile long saveRetryAfter;
    private final AtomicBoolean saveQueued = new AtomicBoolean(false);
    private final AtomicBoolean retryQueued = new AtomicBoolean(false);

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        File dataFile = new File(plugin.getDataFolder(), "data.yml");
        data = dataFile.isFile() ? YamlConfiguration.loadConfiguration(dataFile) : new YamlConfiguration();
        if (dataFile.isFile()) plugin.getLogger().info("Loaded legacy data.yml for database migration; it will not be modified.");
        setupMySql();
    }

    private void setupMySql() {
        DeploymentConfig.DatabaseSettings settings = DeploymentConfig.database(plugin);
        mysqlEnabled = settings.enabled();
        if (!mysqlEnabled) return;

        sqlStorageName = settings.type().equalsIgnoreCase("MARIADB") ? "MariaDB" : "MySQL";
        mysqlTable = settings.table();
        liveControlsTable = settings.liveControlsTable();

        try {
            String url = "jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.database()
                    + "?useSSL=" + settings.useSsl() + "&useUnicode=true&characterEncoding=utf8"
                    + "&autoReconnect=true&tcpKeepAlive=true&connectTimeout=10000&socketTimeout=30000";
            HikariConfig pool = new HikariConfig();
            pool.setPoolName("Fakevival-MariaDB"); pool.setJdbcUrl(url); pool.setUsername(settings.user()); pool.setPassword(settings.password());
            pool.setMaximumPoolSize(Math.max(1, settings.poolSize())); pool.setMinimumIdle(Math.min(2, Math.max(1, settings.poolSize())));
            pool.setConnectionTimeout(10_000L); pool.setValidationTimeout(5_000L); pool.setIdleTimeout(120_000L);
            pool.setMaxLifetime(600_000L); pool.setKeepaliveTime(60_000L); pool.setConnectionTestQuery("SELECT 1");
            dataSource = new HikariDataSource(pool);
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + mysqlTable + "` (`data_key` VARCHAR(64) PRIMARY KEY, `data_value` MEDIUMTEXT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_special_item_counters` (`counter_id` VARCHAR(128) NOT NULL PRIMARY KEY, `next_value` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_player_skills` (`player_uuid` VARCHAR(36) NOT NULL,`skill_id` VARCHAR(32) NOT NULL,`xp` INT NOT NULL DEFAULT 0,`updated_at` BIGINT NOT NULL,PRIMARY KEY (`player_uuid`,`skill_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_player_enchant_unlocks` (`player_uuid` VARCHAR(36) NOT NULL,`enchant_id` VARCHAR(96) NOT NULL,`unlock_source` VARCHAR(64) NOT NULL,`unlocked_at` BIGINT NOT NULL,PRIMARY KEY (`player_uuid`,`enchant_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_shop_default_versions` (`version_key` VARCHAR(128) NOT NULL PRIMARY KEY, `version` INT NOT NULL, `updated_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_npc_skin_cache` (" +
                        "`skin_name` VARCHAR(32) PRIMARY KEY, `uuid` VARCHAR(36), " +
                        "`texture_value` TEXT NOT NULL, `texture_signature` TEXT NOT NULL, " +
                        "`fetched_at` BIGINT NOT NULL, `last_used_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_goldstore_purchase_limits` (`player_uuid` VARCHAR(36) NOT NULL,`product_id` VARCHAR(96) NOT NULL,`limit_window` VARCHAR(32) NOT NULL,`window_start` BIGINT NOT NULL,`purchases` INT NOT NULL DEFAULT 0,`last_purchase_at` BIGINT NOT NULL DEFAULT 0,PRIMARY KEY (`player_uuid`,`product_id`,`limit_window`,`window_start`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_goldstore_coin_exchange_usage` (`player_uuid` VARCHAR(36) NOT NULL,`window_type` VARCHAR(32) NOT NULL,`window_start` BIGINT NOT NULL,`exchange_count` INT NOT NULL DEFAULT 0,`coins_received` DOUBLE NOT NULL DEFAULT 0,`last_exchange_at` BIGINT NOT NULL DEFAULT 0,PRIMARY KEY (`player_uuid`,`window_type`,`window_start`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_network_events` (" +
                        "`event_id` VARCHAR(64) PRIMARY KEY, `display_name` VARCHAR(128) NOT NULL, " +
                        "`state` VARCHAR(32) NOT NULL, `starts_at` BIGINT NOT NULL, `ends_at` BIGINT NOT NULL, " +
                        "`allowed_server_types` TEXT NOT NULL, `created_by` VARCHAR(36) NOT NULL DEFAULT '', " +
                        "`updated_at` BIGINT NOT NULL, INDEX `idx_smp_network_events_state_end` (`state`,`ends_at`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_event_scores` (" +
                        "`event_id` VARCHAR(64) NOT NULL, `player_uuid` VARCHAR(36) NOT NULL, " +
                        "`server_id` VARCHAR(64) NOT NULL, `score` DOUBLE NOT NULL DEFAULT 0, `updated_at` BIGINT NOT NULL, " +
                        "PRIMARY KEY (`event_id`,`player_uuid`,`server_id`), " +
                        "INDEX `idx_smp_event_scores_event_player` (`event_id`,`player_uuid`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + DatabaseRegistry.DISCORD_LINKS_TABLE + "` (" +
                        "`discord_user_id` VARCHAR(32) NULL, " +
                        "`minecraft_uuid` VARCHAR(36) NOT NULL, " +
                        "`minecraft_name` VARCHAR(32) NOT NULL, " +
                        "`link_code` VARCHAR(16) NULL, " +
                        "`linked_at` BIGINT NULL, " +
                        "`created_at` BIGINT NOT NULL DEFAULT (UNIX_TIMESTAMP() * 1000), " +
                        "PRIMARY KEY (`minecraft_uuid`), " +
                        "UNIQUE KEY `uniq_discord_user_id` (`discord_user_id`), " +
                        "KEY `idx_link_code` (`link_code`), " +
                        "KEY `idx_minecraft_name` (`minecraft_name`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + DatabaseRegistry.PLAYER_NETWORTH_CACHE_TABLE + "` (" +
                        "`player_uuid` VARCHAR(36) NOT NULL, " +
                        "`player_name` VARCHAR(32) NOT NULL, " +
                        "`total_networth` BIGINT NOT NULL DEFAULT 0, " +
                        "`purse_coins` BIGINT NOT NULL DEFAULT 0, " +
                        "`bank_coins` BIGINT NOT NULL DEFAULT 0, " +
                        "`inventory_value` BIGINT NOT NULL DEFAULT 0, " +
                        "`storage_value` BIGINT NOT NULL DEFAULT 0, " +
                        "`auction_value` BIGINT NOT NULL DEFAULT 0, " +
                        "`updated_at` BIGINT NOT NULL, " +
                        "PRIMARY KEY (`player_uuid`), " +
                        "KEY `idx_player_name` (`player_name`), " +
                        "KEY `idx_total_networth` (`total_networth`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + DatabaseRegistry.EVENT_NOTIFICATIONS_TABLE + "` (" +
                        "`discord_user_id` VARCHAR(32) NOT NULL, " +
                        "`event_pings_enabled` TINYINT NOT NULL DEFAULT 1, " +
                        "`updated_at` BIGINT NOT NULL, " +
                        "PRIMARY KEY (`discord_user_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_mayor_elections` (`election_id` VARCHAR(64) PRIMARY KEY,`phase` VARCHAR(32) NOT NULL,`registration_starts_at` BIGINT NOT NULL,`registration_ends_at` BIGINT NOT NULL,`voting_starts_at` BIGINT NOT NULL,`voting_ends_at` BIGINT NOT NULL,`term_starts_at` BIGINT NOT NULL,`term_ends_at` BIGINT NOT NULL,`winner_uuid` VARCHAR(36) NULL,`winner_name` VARCHAR(16) NULL,`active_perk_id` VARCHAR(64) NULL,`created_at` BIGINT NOT NULL,`updated_at` BIGINT NOT NULL,INDEX `idx_mayor_phase_updated` (`phase`,`updated_at`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_mayor_candidates` (`election_id` VARCHAR(64) NOT NULL,`player_uuid` VARCHAR(36) NOT NULL,`player_name` VARCHAR(16) NOT NULL,`player_level` INT NOT NULL,`perk_id` VARCHAR(64) NOT NULL,`campaign_message` VARCHAR(160) NOT NULL DEFAULT '',`registered_at` BIGINT NOT NULL,`withdrawn` BOOLEAN NOT NULL DEFAULT FALSE,`votes` INT NOT NULL DEFAULT 0,PRIMARY KEY (`election_id`,`player_uuid`),INDEX `idx_mayor_candidates_election_votes` (`election_id`,`withdrawn`,`votes`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_mayor_votes` (`election_id` VARCHAR(64) NOT NULL,`voter_uuid` VARCHAR(36) NOT NULL,`candidate_uuid` VARCHAR(36) NOT NULL,`voted_at` BIGINT NOT NULL,PRIMARY KEY (`election_id`,`voter_uuid`),INDEX `idx_mayor_votes_candidate` (`election_id`,`candidate_uuid`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_mayor_effects` (`perk_id` VARCHAR(64) PRIMARY KEY,`display_name` VARCHAR(64) NOT NULL,`description` TEXT NOT NULL,`effect_type` VARCHAR(64) NOT NULL,`amount` DOUBLE NOT NULL,`enabled` BOOLEAN NOT NULL DEFAULT TRUE)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_mayor_locations` (`id` VARCHAR(64) PRIMARY KEY,`server_id` VARCHAR(64) NOT NULL,`world` VARCHAR(64) NOT NULL,`x` DOUBLE NOT NULL,`y` DOUBLE NOT NULL,`z` DOUBLE NOT NULL,`yaw` FLOAT NOT NULL,`pitch` FLOAT NOT NULL,`updated_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_mayor_audit` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY,`election_id` VARCHAR(64) NOT NULL,`actor_uuid` VARCHAR(36) NOT NULL,`action` VARCHAR(64) NOT NULL,`details` TEXT NOT NULL,`created_at` BIGINT NOT NULL,INDEX `idx_mayor_audit_election_time` (`election_id`,`created_at`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_mayor_bans` (`player_uuid` VARCHAR(36) PRIMARY KEY,`player_name` VARCHAR(16) NOT NULL,`reason` VARCHAR(160) NOT NULL,`banned_by` VARCHAR(36) NOT NULL,`created_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + liveControlsTable + "` (" +
                        "`control_key` VARCHAR(128) PRIMARY KEY, " +
                        "`control_value` MEDIUMTEXT NOT NULL, " +
                        "`value_type` VARCHAR(32) NOT NULL, " +
                        "`category` VARCHAR(64) NOT NULL, " +
                        "`reason` VARCHAR(255) NOT NULL DEFAULT '', " +
                        "`updated_by` VARCHAR(64) NOT NULL DEFAULT '', " +
                        "`updated_at` BIGINT NOT NULL" +
                        ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_hub_spawns` (" +
                        "`id` VARCHAR(64) PRIMARY KEY, " +
                        "`world` VARCHAR(128) NOT NULL, " +
                        "`x` DOUBLE NOT NULL, " +
                        "`y` DOUBLE NOT NULL, " +
                        "`z` DOUBLE NOT NULL, " +
                        "`yaw` FLOAT NOT NULL, " +
                        "`pitch` FLOAT NOT NULL, " +
                        "`updated_by` VARCHAR(64) NOT NULL DEFAULT '', " +
                        "`updated_at` BIGINT NOT NULL" +
                        ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_hub_data` (" +
                        "`id` VARCHAR(64) PRIMARY KEY, " +
                        "`yaml_data` LONGTEXT NOT NULL, " +
                        "`static_registry_json` LONGTEXT, " +
                        "`updated_by` VARCHAR(64) NOT NULL DEFAULT '', " +
                        "`updated_at` BIGINT NOT NULL" +
                        ")");
                statement.executeUpdate("ALTER TABLE `smp_hub_data` MODIFY `yaml_data` LONGTEXT NOT NULL");
                statement.executeUpdate("ALTER TABLE `smp_hub_data` MODIFY `static_registry_json` LONGTEXT");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_player_bank` (" +
                        "`uuid` VARCHAR(36) PRIMARY KEY, " +
                        "`purse` DOUBLE NOT NULL DEFAULT 0, " +
                        "`bank_balance` DOUBLE NOT NULL DEFAULT 0, " +
                        "`bank_tier` VARCHAR(64) NOT NULL DEFAULT 'starter', " +
                        "`last_interest_claim` BIGINT NOT NULL DEFAULT 0, " +
                        "`created_at` BIGINT, " +
                        "`updated_at` BIGINT" +
                        ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_bank_transactions` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "`uuid` VARCHAR(36), " +
                        "`type` VARCHAR(64), " +
                        "`amount` DOUBLE, " +
                        "`balance_before` DOUBLE, " +
                        "`balance_after` DOUBLE, " +
                        "`source` VARCHAR(128), " +
                        "`created_at` BIGINT, " +
                        "INDEX `idx_smp_bank_transactions_uuid` (`uuid`), " +
                        "INDEX `idx_smp_bank_transactions_created` (`created_at`)" +
                        ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_custom_items` (" +
                        "`id` VARCHAR(64) PRIMARY KEY, " +
                        "`item_type` VARCHAR(32) NOT NULL DEFAULT '', " +
                        "`material` VARCHAR(64) NOT NULL, " +
                        "`display_name` VARCHAR(128) NOT NULL DEFAULT '', " +
                        "`lore` TEXT, " +
                        "`rarity` VARCHAR(32) NOT NULL DEFAULT '', " +
                        "`stats_json` TEXT, " +
                        "`abilities_json` TEXT, " +
                        "`flags_json` TEXT, " +
                        "`nbt_json` TEXT, " +
                        "`created_by` VARCHAR(36) NOT NULL DEFAULT '', " +
                        "`updated_by` VARCHAR(36) NOT NULL DEFAULT '', " +
                        "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guilds` (" +
                        "`guild_id` VARCHAR(64) PRIMARY KEY, " +
                        "`guild_tag_color` VARCHAR(32) NOT NULL DEFAULT 'GRAY'" +
                        ")");
                statement.executeUpdate("ALTER TABLE `smp_guilds` ADD COLUMN IF NOT EXISTS `guild_tag_color` VARCHAR(32) NOT NULL DEFAULT 'GRAY'");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_members` (`guild_id` VARCHAR(64) NOT NULL, `uuid` VARCHAR(36) NOT NULL, `player_name` VARCHAR(64) NOT NULL, `guild_role` VARCHAR(64) NOT NULL DEFAULT 'MEMBER', `joined_at` BIGINT NOT NULL, `last_seen` BIGINT NOT NULL, PRIMARY KEY (`guild_id`,`uuid`))");
                statement.executeUpdate("ALTER TABLE `smp_guild_members` ADD COLUMN IF NOT EXISTS `guild_role` VARCHAR(64) NOT NULL DEFAULT 'MEMBER'");
                statement.executeUpdate("ALTER TABLE `smp_guild_members` ADD COLUMN IF NOT EXISTS `joined_at` BIGINT NOT NULL DEFAULT 0");
                statement.executeUpdate("ALTER TABLE `smp_guild_members` ADD COLUMN IF NOT EXISTS `last_seen` BIGINT NOT NULL DEFAULT 0");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_settings` (`guild_id` VARCHAR(64) PRIMARY KEY, `description` TEXT NOT NULL, `join_type` VARCHAR(32) NOT NULL DEFAULT 'INVITE_ONLY', `guild_chat_enabled` BOOLEAN NOT NULL DEFAULT TRUE, `war_requests` VARCHAR(32) NOT NULL DEFAULT 'ENABLED', `ally_requests` VARCHAR(32) NOT NULL DEFAULT 'ENABLED', `public_member_list` BOOLEAN NOT NULL DEFAULT TRUE, `friendly_fire` BOOLEAN NOT NULL DEFAULT FALSE, `updated_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_wars` (`war_id` VARCHAR(64) PRIMARY KEY, `guild_one_id` VARCHAR(64) NOT NULL, `guild_two_id` VARCHAR(64) NOT NULL, `status` VARCHAR(32) NOT NULL, `started_at` BIGINT NOT NULL, `ended_at` BIGINT NULL, `guild_one_kills` INT NOT NULL DEFAULT 0, `guild_two_kills` INT NOT NULL DEFAULT 0)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_war_requests` (`request_id` VARCHAR(64) PRIMARY KEY, `from_guild_id` VARCHAR(64) NOT NULL, `to_guild_id` VARCHAR(64) NOT NULL, `status` VARCHAR(32) NOT NULL, `created_at` BIGINT NOT NULL, `expires_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_alliances` (`alliance_id` VARCHAR(64) PRIMARY KEY, `guild_one_id` VARCHAR(64) NOT NULL, `guild_two_id` VARCHAR(64) NOT NULL, `status` VARCHAR(32) NOT NULL, `started_at` BIGINT NOT NULL, `ended_at` BIGINT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_ally_requests` (`request_id` VARCHAR(64) PRIMARY KEY, `from_guild_id` VARCHAR(64) NOT NULL, `to_guild_id` VARCHAR(64) NOT NULL, `status` VARCHAR(32) NOT NULL, `created_at` BIGINT NOT NULL, `expires_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_roles` (`guild_id` VARCHAR(64) NOT NULL, `role_id` VARCHAR(64) NOT NULL, `role_name` VARCHAR(32) NOT NULL, `priority` INT NOT NULL, `color` VARCHAR(32) NOT NULL DEFAULT 'GRAY', `created_at` BIGINT NOT NULL, PRIMARY KEY (`guild_id`,`role_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_guild_role_permissions` (`guild_id` VARCHAR(64) NOT NULL, `role_id` VARCHAR(64) NOT NULL, `permission_id` VARCHAR(64) NOT NULL, `enabled` BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY (`guild_id`,`role_id`,`permission_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_homes` (`uuid` CHAR(36) NOT NULL, `home_name` VARCHAR(64) NOT NULL, `server_id` VARCHAR(64) NOT NULL, `server_type` VARCHAR(32) NOT NULL, `world` VARCHAR(64) NOT NULL, `x` DOUBLE NOT NULL, `y` DOUBLE NOT NULL, `z` DOUBLE NOT NULL, `yaw` FLOAT NOT NULL, `pitch` FLOAT NOT NULL, `created_at` BIGINT NOT NULL, `updated_at` BIGINT NOT NULL, PRIMARY KEY (`uuid`,`home_name`))");
                statement.executeUpdate("ALTER TABLE `smp_homes` ADD COLUMN IF NOT EXISTS `server_id` VARCHAR(64) NOT NULL DEFAULT ''");
                statement.executeUpdate("ALTER TABLE `smp_homes` ADD COLUMN IF NOT EXISTS `server_type` VARCHAR(32) NOT NULL DEFAULT 'OVERWORLD'");
                statement.executeUpdate("ALTER TABLE `smp_homes` ADD COLUMN IF NOT EXISTS `created_at` BIGINT NOT NULL DEFAULT 0");
                statement.executeUpdate("ALTER TABLE `smp_homes` ADD COLUMN IF NOT EXISTS `updated_at` BIGINT NOT NULL DEFAULT 0");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_pending_cross_server_teleports` (`teleport_id` VARCHAR(64) PRIMARY KEY, `uuid` CHAR(36) NOT NULL, `target_server_id` VARCHAR(64) NOT NULL, `target_world` VARCHAR(64) NOT NULL, `x` DOUBLE NOT NULL, `y` DOUBLE NOT NULL, `z` DOUBLE NOT NULL, `yaw` FLOAT NOT NULL, `pitch` FLOAT NOT NULL, `reason` VARCHAR(64) NOT NULL, `created_at` BIGINT NOT NULL, `expires_at` BIGINT NOT NULL, INDEX `idx_smp_pending_home_uuid_server` (`uuid`,`target_server_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_player_storage_pages` (`uuid` CHAR(36) NOT NULL, `page` INT NOT NULL, `contents_blob` LONGBLOB NOT NULL, `updated_at` BIGINT NOT NULL, PRIMARY KEY (`uuid`,`page`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_player_storage_unlocks` (`uuid` CHAR(36) PRIMARY KEY, `unlocked_pages` INT NOT NULL DEFAULT 1, `updated_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_player_storage_sessions` (`uuid` CHAR(36) PRIMARY KEY, `page` INT NOT NULL, `server_id` VARCHAR(64) NOT NULL, `opened_at` BIGINT NOT NULL, `expires_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_templates` (" +
                        "`dungeon_id` VARCHAR(128) PRIMARY KEY, `server_type` VARCHAR(32) NOT NULL DEFAULT 'DUNGEON', `template_world_id` VARCHAR(128) NOT NULL, " +
                        "`template_world_name` VARCHAR(128) NOT NULL, `lobby_world` VARCHAR(128) NULL, " +
                        "`spawn_world` VARCHAR(128) NULL, `spawn_x` DOUBLE NULL, `spawn_y` DOUBLE NULL, " +
                        "`spawn_z` DOUBLE NULL, `spawn_yaw` FLOAT NULL, `spawn_pitch` FLOAT NULL, " +
                        "`enabled` BOOLEAN NOT NULL DEFAULT TRUE, `metadata_json` LONGTEXT NULL, `updated_at` BIGINT NOT NULL)");
                statement.executeUpdate("ALTER TABLE `smp_dungeon_templates` ADD COLUMN IF NOT EXISTS `server_type` VARCHAR(32) NOT NULL DEFAULT 'DUNGEON'");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_spawn_points` (" +
                        "`dungeon_id` VARCHAR(128) NOT NULL, `spawn_type` VARCHAR(64) NOT NULL, " +
                        "`spawn_id` VARCHAR(128) NOT NULL, `mob_or_boss_id` VARCHAR(128) NULL, " +
                        "`world` VARCHAR(128) NOT NULL, `x` DOUBLE NOT NULL, `y` DOUBLE NOT NULL, `z` DOUBLE NOT NULL, " +
                        "`yaw` FLOAT NOT NULL, `pitch` FLOAT NOT NULL, `metadata_json` LONGTEXT NULL, " +
                        "PRIMARY KEY (`dungeon_id`,`spawn_type`,`spawn_id`))");
                statement.executeUpdate("ALTER TABLE `smp_dungeon_spawn_points` ADD COLUMN IF NOT EXISTS `level` INT NOT NULL DEFAULT 1");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_runs` (`run_id` VARCHAR(64) PRIMARY KEY, `dungeon_id` VARCHAR(128) NOT NULL, `server_id` VARCHAR(64) NOT NULL, `instance_world` VARCHAR(128) NULL, `state` VARCHAR(32) NOT NULL, `participants_json` LONGTEXT NOT NULL, `spectators_json` LONGTEXT NOT NULL, `boss_name` VARCHAR(128) NULL, `boss_alive` BOOLEAN NOT NULL DEFAULT FALSE, `completed` BOOLEAN NOT NULL DEFAULT FALSE, `created_at` BIGINT NOT NULL, `last_player_left_at` BIGINT NOT NULL DEFAULT 0, `expires_at` BIGINT NOT NULL DEFAULT 0, INDEX `idx_smp_dungeon_runs_server_state` (`server_id`,`state`), INDEX `idx_smp_dungeon_runs_dungeon` (`dungeon_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_run_players` (`uuid` CHAR(36) NOT NULL, `run_id` VARCHAR(64) NOT NULL, `server_id` VARCHAR(64) NOT NULL, `world` VARCHAR(128) NOT NULL, `x` DOUBLE NOT NULL, `y` DOUBLE NOT NULL, `z` DOUBLE NOT NULL, `yaw` FLOAT NOT NULL, `pitch` FLOAT NOT NULL, `online` BOOLEAN NOT NULL DEFAULT FALSE, `updated_at` BIGINT NOT NULL, PRIMARY KEY (`uuid`,`run_id`), INDEX `idx_smp_dungeon_run_players_run` (`run_id`))");
                statement.executeUpdate("ALTER TABLE `smp_dungeon_run_players` ADD COLUMN IF NOT EXISTS `current_health` DOUBLE NOT NULL DEFAULT 0");
                statement.executeUpdate("ALTER TABLE `smp_dungeon_run_players` ADD COLUMN IF NOT EXISTS `max_health` DOUBLE NOT NULL DEFAULT 0");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_loot_chests` (`dungeon_id` VARCHAR(96) NOT NULL, `chest_id` VARCHAR(96) NOT NULL, `loot_table_id` VARCHAR(96) NOT NULL, `world_template_id` VARCHAR(128) NULL, `x` INT NOT NULL, `y` INT NOT NULL, `z` INT NOT NULL, `facing` VARCHAR(16) NOT NULL DEFAULT 'NORTH', `enabled` TINYINT NOT NULL DEFAULT 1, `created_by` VARCHAR(36) NULL, `created_at` BIGINT NOT NULL, `updated_at` BIGINT NOT NULL, PRIMARY KEY (`dungeon_id`,`chest_id`))");
                statement.executeUpdate("ALTER TABLE `smp_dungeon_loot_chests` ADD COLUMN IF NOT EXISTS `facing` VARCHAR(16) NOT NULL DEFAULT 'NORTH'");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_loot_chest_state` (`run_id` VARCHAR(96) NOT NULL, `dungeon_id` VARCHAR(96) NOT NULL, `chest_id` VARCHAR(96) NOT NULL, `looted_by` VARCHAR(36) NULL, `looted_at` BIGINT NULL, `generated_at` BIGINT NOT NULL, `state` VARCHAR(32) NOT NULL DEFAULT 'SPAWNED', PRIMARY KEY (`run_id`,`chest_id`), INDEX `idx_dungeon_loot_state_dungeon` (`dungeon_id`,`run_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_loot_table_entries` (`loot_table_id` VARCHAR(96) NOT NULL, `entry_id` VARCHAR(96) NOT NULL, `custom_item_id` VARCHAR(128) NULL, `vanilla_material` VARCHAR(96) NULL, `min_amount` INT NOT NULL DEFAULT 1, `max_amount` INT NOT NULL DEFAULT 1, `chance` DOUBLE NOT NULL DEFAULT 100, `weight` DOUBLE NOT NULL DEFAULT 1, `rarity` VARCHAR(32) NULL, `coins_min` BIGINT NOT NULL DEFAULT 0, `coins_max` BIGINT NOT NULL DEFAULT 0, `enabled` TINYINT NOT NULL DEFAULT 1, PRIMARY KEY (`loot_table_id`,`entry_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_traps` (`dungeon_id` VARCHAR(96) NOT NULL, `trap_id` VARCHAR(96) NOT NULL, `trap_type` VARCHAR(48) NOT NULL, `world_template_id` VARCHAR(128) NULL, `x` INT NOT NULL, `y` INT NOT NULL, `z` INT NOT NULL, `radius` DOUBLE NOT NULL DEFAULT 1.25, `damage` DOUBLE NOT NULL DEFAULT 25, `cooldown_seconds` INT NOT NULL DEFAULT 5, `one_time` TINYINT NOT NULL DEFAULT 0, `enabled` TINYINT NOT NULL DEFAULT 1, `data_json` MEDIUMTEXT NULL, `created_by` VARCHAR(36) NULL, `created_at` BIGINT NOT NULL, `updated_at` BIGINT NOT NULL, PRIMARY KEY (`dungeon_id`,`trap_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_dungeon_trap_state` (`run_id` VARCHAR(96) NOT NULL, `dungeon_id` VARCHAR(96) NOT NULL, `trap_id` VARCHAR(96) NOT NULL, `state` VARCHAR(32) NOT NULL DEFAULT 'ARMED', `triggered_by` VARCHAR(36) NULL, `triggered_at` BIGINT NULL, `last_triggered_at` BIGINT NULL, `trigger_count` INT NOT NULL DEFAULT 0, PRIMARY KEY (`run_id`,`trap_id`), INDEX `idx_dungeon_trap_state_dungeon` (`dungeon_id`,`run_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_pending_dungeon_spectates` (`spectate_id` VARCHAR(64) PRIMARY KEY, `staff_uuid` CHAR(36) NOT NULL, `target_uuid` CHAR(36) NOT NULL, `dungeon_run_id` VARCHAR(64) NOT NULL, `target_server_id` VARCHAR(64) NOT NULL, `created_at` BIGINT NOT NULL, `expires_at` BIGINT NOT NULL, INDEX `idx_smp_pending_spectate_staff` (`staff_uuid`,`target_server_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_furniture_packs` (" +
                        "`pack_id` VARCHAR(64) PRIMARY KEY, `source_url` VARCHAR(1024) NOT NULL, " +
                        "`source_json` LONGTEXT NOT NULL, `enabled` BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "`installed_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_furniture_entries` (" +
                        "`furniture_id` VARCHAR(96) PRIMARY KEY, `pack_id` VARCHAR(64) NOT NULL, " +
                        "`display_name` VARCHAR(128) NOT NULL, `category` VARCHAR(32) NOT NULL, " +
                        "`source_id` VARCHAR(128) NOT NULL DEFAULT '', `entity_json` LONGTEXT NOT NULL, " +
                        "`preview_material` VARCHAR(64) NOT NULL DEFAULT 'ARMOR_STAND', `price` DOUBLE NOT NULL DEFAULT 100, " +
                        "`permission` VARCHAR(128) NOT NULL DEFAULT '', `placeable` BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "`player_breakable` BOOLEAN NOT NULL DEFAULT TRUE, `spawn_protected` BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "INDEX `idx_smp_furniture_pack` (`pack_id`))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_furniture_model_meta` (" +
                        "`furniture_id` VARCHAR(96) PRIMARY KEY, `created_by` VARCHAR(64) NOT NULL DEFAULT '', " +
                        "`created_at` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_placed_furniture` (" +
                        "`entity_uuid` VARCHAR(36) PRIMARY KEY, `furniture_id` VARCHAR(96) NOT NULL, `pack_id` VARCHAR(64) NOT NULL, " +
                        "`world` VARCHAR(128) NOT NULL, `x` DOUBLE NOT NULL, `y` DOUBLE NOT NULL, `z` DOUBLE NOT NULL, " +
                        "`yaw` FLOAT NOT NULL, `pitch` FLOAT NOT NULL, `owner_uuid` VARCHAR(36) NOT NULL, " +
                        "`placed_at` BIGINT NOT NULL, `protected_spawn` BOOLEAN NOT NULL DEFAULT FALSE, `facing` VARCHAR(8) NOT NULL DEFAULT 'NORTH', " +
                        "INDEX `idx_smp_placed_world` (`world`))");
                statement.executeUpdate("ALTER TABLE `smp_placed_furniture` ADD COLUMN IF NOT EXISTS `facing` VARCHAR(8) NOT NULL DEFAULT 'NORTH'");
                statement.executeUpdate("ALTER TABLE `smp_placed_furniture` ADD COLUMN IF NOT EXISTS `server_id` VARCHAR(64) NOT NULL DEFAULT ''");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_furniture_collision_blocks` (" +
                        "`group_id` VARCHAR(36) NOT NULL, `furniture_id` VARCHAR(96) NOT NULL, `world` VARCHAR(128) NOT NULL, " +
                        "`x` INT NOT NULL, `y` INT NOT NULL, `z` INT NOT NULL, `original_type` VARCHAR(64) NOT NULL, " +
                        "`original_data` TEXT NOT NULL, `owner_uuid` VARCHAR(36) NOT NULL, `protected_block` BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "PRIMARY KEY (`group_id`,`world`,`x`,`y`,`z`), INDEX `idx_smp_furniture_collision_location` (`world`,`x`,`y`,`z`))");
                statement.executeUpdate("ALTER TABLE `smp_furniture_collision_blocks` ADD COLUMN IF NOT EXISTS `server_id` VARCHAR(64) NOT NULL DEFAULT ''");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_bazaar_market` (" +
                        "`item_id` VARCHAR(64) PRIMARY KEY, " +
                        "`base_price` DOUBLE NOT NULL, " +
                        "`current_buy_price` DOUBLE NOT NULL, " +
                        "`current_sell_price` DOUBLE NOT NULL, " +
                        "`recent_buys` BIGINT NOT NULL DEFAULT 0, " +
                        "`recent_sells` BIGINT NOT NULL DEFAULT 0, " +
                        "`total_buys` BIGINT NOT NULL DEFAULT 0, " +
                        "`total_sells` BIGINT NOT NULL DEFAULT 0, " +
                        "`last_updated` BIGINT NOT NULL" +
                        ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `smp_bazaar_transactions` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "`player_uuid` VARCHAR(36) NOT NULL, " +
                        "`item_id` VARCHAR(64) NOT NULL, " +
                        "`action` VARCHAR(8) NOT NULL, " +
                        "`amount` INT NOT NULL, " +
                        "`unit_price` DOUBLE NOT NULL, " +
                        "`total_price` DOUBLE NOT NULL, " +
                        "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "INDEX `idx_smp_bazaar_transactions_item` (`item_id`), " +
                        "INDEX `idx_smp_bazaar_transactions_player` (`player_uuid`)" +
                        ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS smp_macrocheck_logs (check_id VARCHAR(64) PRIMARY KEY, staff_uuid CHAR(36) NOT NULL, staff_name VARCHAR(32) NOT NULL, target_uuid CHAR(36) NOT NULL, target_name VARCHAR(32) NOT NULL, result VARCHAR(32) NOT NULL, server_id VARCHAR(64) NOT NULL, summary_json LONGTEXT NULL, started_at BIGINT NOT NULL, ended_at BIGINT NOT NULL, INDEX idx_macrocheck_target_time (target_uuid,ended_at))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS smp_adventure_guild_profiles (uuid CHAR(36) PRIMARY KEY, rank_id VARCHAR(8) NOT NULL DEFAULT 'E', registered_at BIGINT NOT NULL, completed_quests INT NOT NULL DEFAULT 0, completed_dungeons INT NOT NULL DEFAULT 0, defeated_bosses INT NOT NULL DEFAULT 0, tyrant_kills INT NOT NULL DEFAULT 0, boss_quests INT NOT NULL DEFAULT 0, updated_at BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS smp_adventure_guild_rank_history (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid CHAR(36) NOT NULL, old_rank VARCHAR(8) NOT NULL, new_rank VARCHAR(8) NOT NULL, ranked_up_at BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS smp_player_recipe_progress (uuid CHAR(36) NOT NULL, recipe_id VARCHAR(128) NOT NULL, unlocked BOOLEAN NOT NULL DEFAULT FALSE, progress_json LONGTEXT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (uuid,recipe_id))");
            }
            loadFromMySql();
            plugin.getLogger().info("Fakevival " + sqlStorageName + " storage enabled.");
        } catch (SQLException e) {
            mysqlEnabled = false;
            if (dataSource != null) { dataSource.close(); dataSource = null; }
            plugin.getLogger().severe("Could not connect to " + sqlStorageName + ". Runtime data will remain memory-only; data.yml writes are disabled.");
            plugin.getLogger().severe("Error: " + e.getMessage());
        }
    }

    private void loadFromMySql() throws SQLException {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT `data_value` FROM `" + mysqlTable + "` WHERE `data_key`=?")) {
            statement.setString(1, "data");
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    data = new YamlConfiguration();
                    try {
                        data.loadFromString(result.getString("data_value"));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Could not read Fakevival " + sqlStorageName + " data. Keeping the migration/runtime snapshot in memory.");
                        plugin.getLogger().severe("Error: " + e.getMessage());
                    }
                } else {
                    saveToMySql();
                }
            }
        }
    }

    public void save() {
        if (!mysqlEnabled) return;
        String snapshot = dataSnapshotWithoutHubSpawn();
        if (!plugin.isEnabled()) {
            saveSnapshot(snapshot);
            return;
        }
        if (!saveQueued.compareAndSet(false, true)) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try { saveSnapshot(snapshot); }
            finally { saveQueued.set(false); }
        });
    }

    public void saveNow() {
        if (!mysqlEnabled) return;
        saveSnapshot(dataSnapshotWithoutHubSpawn());
    }

    private void saveSnapshot(String snapshot) {
        synchronized (saveLock) {
            if (mysqlEnabled) saveToMySql(snapshot);
        }
    }

    private void saveToMySql() {
        saveToMySql(dataSnapshotWithoutHubSpawn());
    }

    private void saveToMySql(String snapshot) {
        if (dataSource == null || dataSource.isClosed()) {
            long now = System.currentTimeMillis();
            if (now >= nextSaveErrorLogAt) {
                nextSaveErrorLogAt = now + 60_000L;
                plugin.getLogger().warning("[SMP] MariaDB runtime-data save skipped because the connection pool is closed.");
            }
            return;
        }
        if (System.currentTimeMillis() < saveRetryAfter) return;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(
                "REPLACE INTO `" + mysqlTable + "` (`data_key`, `data_value`) VALUES (?, ?)")) {
            statement.setString(1, "data");
            statement.setString(2, snapshot);
            statement.executeUpdate();
            saveRetryAfter = 0L;
        } catch (SQLException e) {
            long now = System.currentTimeMillis(); saveRetryAfter = now + 60_000L;
            if (now >= nextSaveErrorLogAt) {
                nextSaveErrorLogAt = now + 60_000L;
                plugin.getLogger().warning("[SMP] MariaDB save failed for runtime-data. Retrying in 60s. Reason: " + e.getMessage());
            }
            queueSaveRetry();
        }
    }

    private void queueSaveRetry() {
        if (!plugin.isEnabled() || !retryQueued.compareAndSet(false, true)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            retryQueued.set(false);
            saveRetryAfter = 0L;
            save();
        }, 20L * 60L);
    }

    private String dataSnapshotWithoutHubSpawn() {
        YamlConfiguration copy = new YamlConfiguration();
        try {
            copy.loadFromString(data.saveToString());
            copy.set("spawn", null);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not create filtered data snapshot without hub spawn: " + exception.getMessage());
        }
        return copy.saveToString();
    }

    public void close() {
        mysqlEnabled = false;
        saveQueued.set(false);
        retryQueued.set(false);
        if (dataSource != null) { dataSource.close(); dataSource = null; }
    }

    public FileConfiguration getData() {
        return data;
    }

    public boolean isMySqlEnabled() {
        return mysqlEnabled && dataSource != null && !dataSource.isClosed();
    }

    public Connection getConnection() throws SQLException {
        if (!isMySqlEnabled()) throw new SQLException("MariaDB pool is unavailable");
        return dataSource.getConnection();
    }

    public int getShopDefaultVersion(String key) {
        int fallback = data == null ? 0 : data.getInt("shop-default-versions." + key, 0);
        if (!isMySqlEnabled()) return fallback;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT `version` FROM `smp_shop_default_versions` WHERE `version_key`=?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getInt(1) : fallback; }
        } catch (SQLException error) {
            plugin.getLogger().warning("[SMP Shops] Could not read default version: " + error.getMessage());
            return fallback;
        }
    }

    public void setShopDefaultVersion(String key, int version) {
        if (data != null) data.set("shop-default-versions." + key, version);
        if (!isMySqlEnabled()) return;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `smp_shop_default_versions` (`version_key`,`version`,`updated_at`) VALUES (?,?,?) " +
                        "ON DUPLICATE KEY UPDATE `version`=VALUES(`version`),`updated_at`=VALUES(`updated_at`)")) {
            statement.setString(1, key); statement.setInt(2, version); statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException error) {
            plugin.getLogger().warning("[SMP Shops] Could not store default version: " + error.getMessage());
        }
    }

    public String getLiveControlsTable() {
        return liveControlsTable == null || liveControlsTable.isBlank() ? "fakevival_live_controls" : liveControlsTable;
    }

    public void saveGuildTagColorAsync(String guildId, String colorId) {
        if (!isMySqlEnabled() || guildId == null || guildId.isBlank()) return;
        String safeGuildId = guildId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
        String safeColor = colorId == null ? "GRAY" : colorId.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z_]", "");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `smp_guilds` (`guild_id`, `guild_tag_color`) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE `guild_tag_color`=VALUES(`guild_tag_color`)")) {
                statement.setString(1, safeGuildId);
                statement.setString(2, safeColor.isBlank() ? "GRAY" : safeColor);
                statement.executeUpdate();
            } catch (SQLException exception) {
                long now = System.currentTimeMillis();
                if (now >= nextGuildSaveErrorLogAt) {
                    nextGuildSaveErrorLogAt = now + 60_000L;
                    plugin.getLogger().warning("[SMP] Could not persist guild tag color: " + exception.getMessage());
                }
            }
        });
    }

    @FunctionalInterface
    public interface DatabaseOperation { void execute(Connection connection) throws SQLException; }

    /** Runs a guild/database mutation off-thread using a fresh pooled connection. */
    public void runDatabaseAsync(String operationName, DatabaseOperation operation) {
        if (!isMySqlEnabled() || operation == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection()) {
                operation.execute(connection);
            } catch (SQLException exception) {
                long now = System.currentTimeMillis();
                if (now >= nextGuildSaveErrorLogAt) {
                    nextGuildSaveErrorLogAt = now + 60_000L;
                    plugin.getLogger().warning("[SMP] Guild database operation " + operationName + " failed: " + exception.getMessage());
                }
            }
        });
    }
}
