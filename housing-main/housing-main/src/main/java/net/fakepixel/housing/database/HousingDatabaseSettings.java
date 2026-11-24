package net.fakepixel.housing.database;

import java.util.Map;

public final class HousingDatabaseSettings {
    private final boolean enabled;
    private final String host, database, user, password;
    private final int port, poolSize;

    private HousingDatabaseSettings(boolean enabled, String host, int port, String database, String user, String password, int poolSize) {
        this.enabled=enabled; this.host=host; this.port=port; this.database=database; this.user=user; this.password=password; this.poolSize=poolSize;
    }
    public static HousingDatabaseSettings fromEnvironment() { return from(System.getenv()); }
    static HousingDatabaseSettings from(Map<String,String> env) {
        boolean enabled=Boolean.parseBoolean(env.getOrDefault("HOUSING_DATABASE_ENABLED", "false"));
        return new HousingDatabaseSettings(enabled, trim(env.get("HOUSING_DATABASE_HOST")), integer(env.get("HOUSING_DATABASE_PORT"),3306), trim(env.get("HOUSING_DATABASE_NAME")), trim(env.get("HOUSING_DATABASE_USER")), env.getOrDefault("HOUSING_DATABASE_PASSWORD",""), Math.max(1,integer(env.get("HOUSING_DATABASE_POOL_SIZE"),10)));
    }
    private static String trim(String s){return s==null?"":s.trim();} private static int integer(String s,int d){try{return Integer.parseInt(s);}catch(Exception ignored){return d;}}
    public boolean isEnabled(){return enabled;} public boolean isComplete(){return !host.isEmpty()&&!database.isEmpty()&&!user.isEmpty();}
    public String missingDescription(){StringBuilder b=new StringBuilder();if(host.isEmpty())b.append(" HOUSING_DATABASE_HOST");if(database.isEmpty())b.append(" HOUSING_DATABASE_NAME");if(user.isEmpty())b.append(" HOUSING_DATABASE_USER");return b.toString().trim();}
    public String getJdbcUrl(){return "jdbc:mariadb://"+host+":"+port+"/"+database+"?useUnicode=true&characterEncoding=utf8&useServerPrepStmts=true";}
    public String getUser(){return user;} public String getPassword(){return password;} public int getPoolSize(){return poolSize;}
}
