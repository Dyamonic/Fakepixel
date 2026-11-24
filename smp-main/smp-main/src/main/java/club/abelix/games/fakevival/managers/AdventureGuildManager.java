package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.util.CC;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public final class AdventureGuildManager implements org.bukkit.event.Listener {
    public enum Rank {
        E(1,2,1.0,"&e&l"),D(2,3,1.10,"&a&l"),C(3,4,1.25,"&2&l"),B(4,5,1.50,"&3&l"),A(5,6,1.75,"&6&l"),S(6,8,2.0,"&c&l");
        public final int order,questLimit; public final double multiplier; public final String color;
        Rank(int order,int questLimit,double multiplier,String color){this.order=order;this.questLimit=questLimit;this.multiplier=multiplier;this.color=color;}
        public Rank next(){return this==S?null:values()[ordinal()+1];}
    }
    public record Profile(boolean registered,Rank rank,int quests,int dungeons,int bosses,int tyrantKills,int bossQuests){}
    private final JavaPlugin plugin; private final club.abelix.games.fakevival.data.DataManager data; private final SkillManager skills;
    private final Map<UUID,Profile> cache=new ConcurrentHashMap<>();
    public AdventureGuildManager(JavaPlugin plugin,club.abelix.games.fakevival.data.DataManager data,SkillManager skills){this.plugin=plugin;this.data=data;this.skills=skills;}
    public Profile profile(Player p){return cache.getOrDefault(p.getUniqueId(),new Profile(false,Rank.E,0,0,0,0,0));}
    public void load(Player p){if(!data.isMySqlEnabled())return;CompletableFuture.supplyAsync(()->load(p.getUniqueId())).thenAccept(v->{if(v!=null)cache.put(p.getUniqueId(),v);});}
    @org.bukkit.event.EventHandler public void onJoin(org.bukkit.event.player.PlayerJoinEvent event){load(event.getPlayer());}
    @org.bukkit.event.EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event){cache.remove(event.getPlayer().getUniqueId());}
    private Profile load(UUID id){try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("SELECT rank_id,completed_quests,completed_dungeons,defeated_bosses,tyrant_kills,boss_quests FROM smp_adventure_guild_profiles WHERE uuid=?")){s.setString(1,id.toString());try(ResultSet r=s.executeQuery()){if(!r.next())return null;return new Profile(true,rank(r.getString(1)),r.getInt(2),r.getInt(3),r.getInt(4),r.getInt(5),r.getInt(6));}}catch(SQLException e){plugin.getLogger().warning("Could not load Adventure Guild profile: "+e.getMessage());return null;}}
    public void register(Player p){if(profile(p).registered()){p.sendMessage(CC.color("&eYou are already registered with the Adventurer's Guild."));return;}Profile v=new Profile(true,Rank.E,0,0,0,0,0);cache.put(p.getUniqueId(),v);save(p.getUniqueId(),v);p.sendMessage(CC.color("&aYou registered with the Adventurer's Guild! &7Rank: &eE"));}
    public int activeLimit(Player p){return profile(p).rank.questLimit;} public double rewardMultiplier(Player p){return profile(p).rank.multiplier;}
    public int activeQuests(Player p){int count=0;for(String period:List.of("daily","weekly")){var s=data.getData().getConfigurationSection("players."+p.getUniqueId()+".adventure-guild."+period);if(s!=null)for(String id:s.getKeys(false))if(s.getBoolean(id+".accepted")&&!s.getBoolean(id+".claimed"))count++;}return count;}
    public boolean canAccept(Player p){return profile(p).registered()&&activeQuests(p)<activeLimit(p);}
    public void questCompleted(Player p,boolean boss){Profile q=profile(p);if(!q.registered())return;Profile n=new Profile(true,q.rank,q.quests+1,q.dungeons,q.bosses,q.tyrantKills,q.bossQuests+(boss?1:0));cache.put(p.getUniqueId(),n);save(p.getUniqueId(),n);}
    public void dungeonCompleted(Player p,boolean tyrant){Profile q=profile(p);if(!q.registered())return;Profile n=new Profile(true,q.rank,q.quests,q.dungeons+1,q.bosses+1,q.tyrantKills+(tyrant?1:0),q.bossQuests);cache.put(p.getUniqueId(),n);save(p.getUniqueId(),n);}
    public List<String> requirements(Player p,Rank target){Profile q=profile(p);Map<String,Integer> need=switch(target){case D->Map.of("quests",5,"combat",5,"mining",3,"fishing",3,"dungeons",1);case C->Map.of("quests",15,"combat",15,"mining",10,"farming",10,"dungeons",3,"bosses",1);case B->Map.of("quests",35,"combat",30,"mining",20,"farming",20,"fishing",15,"dungeons",10,"bosses",5);case A->Map.of("quests",75,"combat",50,"mining",35,"farming",35,"fishing",30,"dungeons",25,"tyrant",1);case S->Map.of("quests",150,"combat",75,"mining",50,"farming",50,"fishing",45,"dungeons",50,"tyrant",5,"bossquests",10);default->Map.of();};List<String> out=new ArrayList<>();for(var e:need.entrySet()){int have=value(p,q,e.getKey());out.add((have>=e.getValue()?"&a✔ ":"&c✖ ")+pretty(e.getKey())+" &7"+have+"/"+e.getValue());}return out;}
    public boolean rankUp(Player p){Profile q=profile(p);Rank next=q.rank.next();if(!q.registered()||next==null||requirements(p,next).stream().anyMatch(x->x.startsWith("&c")))return false;Profile n=new Profile(true,next,q.quests,q.dungeons,q.bosses,q.tyrantKills,q.bossQuests);cache.put(p.getUniqueId(),n);save(p.getUniqueId(),n);p.sendMessage(CC.color("&aAdventure rank increased to "+next.color+next+"&a!"));return true;}
    private int value(Player p,Profile q,String key){return switch(key){case "quests"->q.quests;case "dungeons"->q.dungeons;case "bosses"->q.bosses;case "tyrant"->q.tyrantKills;case "bossquests"->q.bossQuests;default->skills.getLevel(p,key);};}
    private void save(UUID id,Profile p){CompletableFuture.runAsync(()->{try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO smp_adventure_guild_profiles(uuid,rank_id,registered_at,completed_quests,completed_dungeons,defeated_bosses,tyrant_kills,boss_quests,updated_at) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE rank_id=VALUES(rank_id),completed_quests=VALUES(completed_quests),completed_dungeons=VALUES(completed_dungeons),defeated_bosses=VALUES(defeated_bosses),tyrant_kills=VALUES(tyrant_kills),boss_quests=VALUES(boss_quests),updated_at=VALUES(updated_at)")){long now=System.currentTimeMillis();s.setString(1,id.toString());s.setString(2,p.rank.name());s.setLong(3,now);s.setInt(4,p.quests);s.setInt(5,p.dungeons);s.setInt(6,p.bosses);s.setInt(7,p.tyrantKills);s.setInt(8,p.bossQuests);s.setLong(9,now);s.executeUpdate();}catch(SQLException e){plugin.getLogger().warning("Could not save Adventure Guild profile: "+e.getMessage());}});}
    private Rank rank(String v){try{return Rank.valueOf(v);}catch(Exception ignored){return Rank.E;}}private String pretty(String s){return Arrays.stream(s.split("_")).map(x->Character.toUpperCase(x.charAt(0))+x.substring(1)).reduce((a,b)->a+" "+b).orElse(s);}
}
