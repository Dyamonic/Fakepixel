package net.fakepixel.housing.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public final class HousingDatabaseManager implements AutoCloseable {
    private final Logger logger; private HikariDataSource dataSource;
    public HousingDatabaseManager(Logger logger){this.logger=logger;}
    public boolean connect(HousingDatabaseSettings settings){
        if(!settings.isEnabled()){logger.warning("Housing database is disabled. Persistent housing data will not be available.");return false;}
        logger.info("Housing database enabled.");
        if(!settings.isComplete()){logger.severe("Housing database is enabled but required environment variables are missing: "+settings.missingDescription());return false;}
        try{HikariConfig c=new HikariConfig();c.setJdbcUrl(settings.getJdbcUrl());c.setUsername(settings.getUser());c.setPassword(settings.getPassword());c.setMaximumPoolSize(settings.getPoolSize());c.setMinimumIdle(1);c.setPoolName("Housing-MariaDB");c.setConnectionTimeout(10000);c.setInitializationFailTimeout(10000);dataSource=new HikariDataSource(c);try(Connection ignored=dataSource.getConnection()){logger.info("Connected to MariaDB.");}return true;}catch(Exception ex){logger.severe("Could not connect to MariaDB: "+ex.getMessage());close();return false;}
    }
    public boolean isConnected(){return dataSource!=null&&!dataSource.isClosed();}
    public Connection connection() throws SQLException {if(!isConnected())throw new SQLException("Housing database is unavailable");return dataSource.getConnection();}
    public int count(String table) { if (!table.matches("housing_[a-z_]+")) return 0; try(Connection c=connection();java.sql.Statement s=c.createStatement();java.sql.ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.next()?r.getInt(1):0;}catch(Exception ignored){return 0;} }
    public void close(){if(dataSource!=null){dataSource.close();dataSource=null;}}
}
