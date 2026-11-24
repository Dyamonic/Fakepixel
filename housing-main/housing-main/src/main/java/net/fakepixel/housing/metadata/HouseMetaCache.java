package net.fakepixel.housing.metadata;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.cluster.HousingPerformanceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public final class HouseMetaCache {
    private final HousingPlugin plugin; private final HouseSummaryRepository repository;
    private volatile Map<UUID,List<HouseSummary>> byOwner=Collections.emptyMap(); private BukkitTask task;
    public HouseMetaCache(HousingPlugin plugin,HouseSummaryRepository repository){this.plugin=plugin;this.repository=repository;}
    public void start(){task=Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,this::refresh,1L,HousingPerformanceRegistry.BROWSE_CACHE_REFRESH_SECONDS*20L);}
    public void stop(){if(task!=null)task.cancel();byOwner=Collections.emptyMap();}
    public void refresh(){try{Map<UUID,List<HouseSummary>> next=new HashMap<>();for(HouseSummary summary:repository.loadAll())next.computeIfAbsent(summary.getOwner(),k->new ArrayList<>()).add(summary);Map<UUID,List<HouseSummary>> safe=new HashMap<>();for(Map.Entry<UUID,List<HouseSummary>> e:next.entrySet())safe.put(e.getKey(),Collections.unmodifiableList(e.getValue()));byOwner=Collections.unmodifiableMap(safe);}catch(Exception ex){plugin.getLogger().warning("Could not refresh lobby house metadata cache: "+ex.getMessage());}}
    public List<HouseSummary> getByOwner(UUID owner){List<HouseSummary> result=byOwner.get(owner);return result==null?Collections.<HouseSummary>emptyList():result;}
    public int totalCookies(UUID owner){long total=0;for(HouseSummary h:getByOwner(owner))total+=Math.max(0,h.getCookies());return (int)Math.min(Integer.MAX_VALUE,total);}
    public int totalWeeklyCookies(UUID owner){long total=0;for(HouseSummary h:getByOwner(owner))total+=Math.max(0,h.getWeeklyCookies());return (int)Math.min(Integer.MAX_VALUE,total);}
}
