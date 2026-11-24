package club.abelix.commons.mysql.modern.impl;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.sql.SQLException;

@Slf4j
public class MySQL extends Database {

    public MySQL(DatabaseCredentials credentials) {
        super(credentials);
    }

    @Override
    public HikariDataSource initSource(DatabaseCredentials credentials) {
        val source = new HikariDataSource();

        val jbdcUrl = "jdbc:mariadb://" + credentials.getHost() + ":" + credentials.getPort()
                + (credentials.getDatabase() != null ? "/" + credentials.getDatabase() : "")
                + "?sslMode=DISABLED";

        source.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
        source.setMinimumIdle(3);
        source.setAutoCommit(true);
        source.setMaxLifetime(270000);
        source.setKeepaliveTime(150000);
        source.setIdleTimeout(180000);
        source.setConnectionTimeout(30000);
        source.setValidationTimeout(5000);
        source.setLeakDetectionThreshold(60000);
        source.setMaximumPoolSize(credentials.getMaximumPoolSize());
        source.setPoolName("MySQL-" + credentials.getDatabase());

        source.addDataSourceProperty("url", jbdcUrl);
        source.addDataSourceProperty("user", credentials.getUser());
        source.addDataSourceProperty("password", credentials.getPassword());

        try {
            source.setLoginTimeout(30);
        } catch (SQLException causes) {
            log.error("Failed connection to database! Details: ", causes);
        }

        return source;
    }
}