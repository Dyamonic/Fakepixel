package net.fakepixel.housing.database;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public final class HousingDatabaseMigrator {
    private final HousingDatabaseManager database; private final Logger logger;
    public HousingDatabaseMigrator(HousingDatabaseManager database,Logger logger){this.database=database;this.logger=logger;}
    public void migrate() throws Exception {
        List<String> sql=Arrays.asList(
          "CREATE TABLE IF NOT EXISTS housing_houses (house_id VARCHAR(80) PRIMARY KEY, owner_uuid CHAR(36) NOT NULL, owner_name VARCHAR(64) NOT NULL, plot_id INT NOT NULL, name VARCHAR(128) NOT NULL, description TEXT NULL, visibility VARCHAR(24) NOT NULL, selected_theme VARCHAR(64) NOT NULL, plot_size INT NOT NULL, cookies INT NOT NULL DEFAULT 0, weekly_cookies INT NOT NULL DEFAULT 0, template_name VARCHAR(128) NOT NULL, world_name VARCHAR(128) NOT NULL, guest_spawn_world VARCHAR(128) NULL, guest_spawn_x DOUBLE NULL, guest_spawn_y DOUBLE NULL, guest_spawn_z DOUBLE NULL, guest_spawn_yaw FLOAT NULL, guest_spawn_pitch FLOAT NULL, runtime_data LONGTEXT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, UNIQUE KEY housing_owner_plot(owner_uuid,plot_id), KEY housing_visibility(visibility)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS cookies INT NOT NULL DEFAULT 0 AFTER plot_size",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS weekly_cookies INT NOT NULL DEFAULT 0 AFTER cookies",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS tags_json LONGTEXT NULL AFTER weekly_cookies",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS owner_spawn_world VARCHAR(128) NULL AFTER world_name",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS owner_spawn_x DOUBLE NULL AFTER owner_spawn_world",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS owner_spawn_y DOUBLE NULL AFTER owner_spawn_x",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS owner_spawn_z DOUBLE NULL AFTER owner_spawn_y",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS owner_spawn_yaw FLOAT NULL AFTER owner_spawn_z",
          "ALTER TABLE housing_houses ADD COLUMN IF NOT EXISTS owner_spawn_pitch FLOAT NULL AFTER owner_spawn_yaw",
          "CREATE TABLE IF NOT EXISTS housing_players (player_uuid CHAR(36) PRIMARY KEY, player_name VARCHAR(64) NOT NULL, updated_at BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_house_invites (player_uuid CHAR(36) PRIMARY KEY, owner_uuid CHAR(36) NOT NULL, house_id VARCHAR(80) NOT NULL, created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, KEY housing_invite_house(house_id), KEY housing_invite_expiry(expires_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_house_members (house_id VARCHAR(80) NOT NULL, player_uuid CHAR(36) NOT NULL, group_id VARCHAR(64) NOT NULL, added_at BIGINT NOT NULL, PRIMARY KEY(house_id,player_uuid), CONSTRAINT fk_members_house FOREIGN KEY(house_id) REFERENCES housing_houses(house_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_house_groups (house_id VARCHAR(80) NOT NULL, group_id VARCHAR(64) NOT NULL, display_name VARCHAR(64) NOT NULL, tag VARCHAR(64) NOT NULL, permissions_json LONGTEXT NOT NULL, PRIMARY KEY(house_id,group_id), CONSTRAINT fk_groups_house FOREIGN KEY(house_id) REFERENCES housing_houses(house_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_house_bans (house_id VARCHAR(80) NOT NULL, player_uuid CHAR(36) NOT NULL, reason TEXT NULL, created_at BIGINT NOT NULL, PRIMARY KEY(house_id,player_uuid), CONSTRAINT fk_bans_house FOREIGN KEY(house_id) REFERENCES housing_houses(house_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_house_mutes (house_id VARCHAR(80) NOT NULL, player_uuid CHAR(36) NOT NULL, reason TEXT NULL, created_at BIGINT NOT NULL, PRIMARY KEY(house_id,player_uuid), CONSTRAINT fk_mutes_house FOREIGN KEY(house_id) REFERENCES housing_houses(house_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_cookies (house_id VARCHAR(80) NOT NULL, player_uuid CHAR(36) NOT NULL, amount INT NOT NULL, week_id VARCHAR(16) NOT NULL, created_at BIGINT NOT NULL, PRIMARY KEY(house_id,player_uuid,week_id), CONSTRAINT fk_cookies_house FOREIGN KEY(house_id) REFERENCES housing_houses(house_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_unlocks (player_uuid CHAR(36) NOT NULL, unlock_type VARCHAR(32) NOT NULL, unlock_id VARCHAR(128) NOT NULL, unlocked_at BIGINT NOT NULL, PRIMARY KEY(player_uuid,unlock_type,unlock_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_npcs (npc_id VARCHAR(80) PRIMARY KEY, house_id VARCHAR(80) NULL, name VARCHAR(128) NOT NULL, skin LONGTEXT NULL, world VARCHAR(128) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, action_type VARCHAR(32) NULL, action_data_json LONGTEXT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, KEY npcs_house(house_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_holograms (hologram_id VARCHAR(80) PRIMARY KEY, house_id VARCHAR(80) NOT NULL, world VARCHAR(128) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, lines_json LONGTEXT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, KEY holograms_house(house_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_parkour (house_id VARCHAR(80) PRIMARY KEY, start_location_json LONGTEXT NULL, end_location_json LONGTEXT NULL, checkpoints_json LONGTEXT NULL, leaderboard_hologram_json LONGTEXT NULL, updated_at BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_parkour_times (house_id VARCHAR(80) NOT NULL, player_uuid CHAR(36) NOT NULL, best_time_millis BIGINT NOT NULL, completed_at BIGINT NOT NULL, PRIMARY KEY(house_id,player_uuid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_teleport_pads (pad_id VARCHAR(80) PRIMARY KEY, house_id VARCHAR(80) NOT NULL, name VARCHAR(128) NOT NULL, world VARCHAR(128) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, linked_pad_id VARCHAR(80) NULL, particles_enabled BOOLEAN NOT NULL, sound_enabled BOOLEAN NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, KEY pads_house(house_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_live_controls (control_key VARCHAR(128) PRIMARY KEY, control_value LONGTEXT NOT NULL, value_type VARCHAR(32) NOT NULL, reason TEXT NULL, updated_by VARCHAR(128) NULL, updated_at BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_player_browse (player_uuid CHAR(36) PRIMARY KEY, browse_data LONGTEXT NOT NULL, updated_at BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_instances (server_id VARCHAR(64) PRIMARY KEY, role VARCHAR(16) NOT NULL, player_count INT NOT NULL, loaded_house_count INT NOT NULL, max_loaded_houses INT NOT NULL, online BOOLEAN NOT NULL, last_heartbeat BIGINT NOT NULL, KEY housing_instance_selection(role,online,last_heartbeat,loaded_house_count)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_house_locks (house_id VARCHAR(80) PRIMARY KEY, server_id VARCHAR(64) NOT NULL, locked_at BIGINT NOT NULL, lock_expires_at BIGINT NOT NULL, KEY housing_lock_server(server_id), KEY housing_lock_expiry(lock_expires_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
          "CREATE TABLE IF NOT EXISTS housing_house_locations (house_id VARCHAR(80) PRIMARY KEY, server_id VARCHAR(64) NOT NULL, updated_at BIGINT NOT NULL, KEY housing_location_server(server_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
          ,"CREATE TABLE IF NOT EXISTS housing_pending_routes (player_uuid CHAR(36) PRIMARY KEY, action VARCHAR(24) NOT NULL, house_id VARCHAR(80) NOT NULL, target_server VARCHAR(64) NOT NULL, created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, KEY housing_pending_expiry(expires_at), KEY housing_pending_target(target_server)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
        try(Connection c=database.connection();Statement s=c.createStatement()){for(String statement:sql)s.execute(statement);} logger.info("Created/verified Housing tables.");
    }
}
