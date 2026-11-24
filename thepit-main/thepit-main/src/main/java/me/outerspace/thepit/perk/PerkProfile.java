package me.outerspace.thepit.perk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PerkProfile {
    private final Set<String> unlockedPerks = new LinkedHashSet<>();
    private final Set<String> permanentPerks = new LinkedHashSet<>();
    private final Set<Integer> unlockedSlots = new LinkedHashSet<>();
    private final Set<Integer> permanentSlots = new LinkedHashSet<>();
    private final Map<Integer, String> equipped = new LinkedHashMap<>();
    private final Map<String, Long> purchases = new LinkedHashMap<>();
    private final Map<String, Long> cooldowns = new LinkedHashMap<>();

    public PerkProfile() {}

    public Set<String> unlockedPerks() { return Collections.unmodifiableSet(unlockedPerks); }
    public Set<String> permanentPerks() { return Collections.unmodifiableSet(permanentPerks); }
    public Set<Integer> unlockedSlots() { return Collections.unmodifiableSet(unlockedSlots); }
    public Map<Integer, String> equipped() { return Collections.unmodifiableMap(equipped); }
    public Map<String, Long> purchases() { return Collections.unmodifiableMap(purchases); }
    public Map<String, Long> cooldowns() { return Collections.unmodifiableMap(cooldowns); }
    public boolean isUnlocked(String id) { return unlockedPerks.contains(id) || permanentPerks.contains(id); }
    public boolean isTemporarilyUnlocked(String id) { return unlockedPerks.contains(id); }
    public boolean isPermanentlyUnlocked(String id) { return permanentPerks.contains(id); }
    public boolean isSlotUnlocked(int slot) { return unlockedSlots.contains(slot) || permanentSlots.contains(slot); }
    public String equipped(int slot) { return equipped.get(slot); }
    public boolean hasEquipped(String id) { return equipped.containsValue(id); }
    public void unlock(String id, long purchasedAt) { unlockedPerks.add(id); purchases.putIfAbsent(id, purchasedAt); }
    public void unlockPermanent(String id) { permanentPerks.add(id); }
    public void remove(String id) { unlockedPerks.remove(id); permanentPerks.remove(id); equipped.values().removeIf(id::equals); }
    public void unlockSlot(int slot) { if (slot >= 1 && slot <= 4) unlockedSlots.add(slot); }
    public void unlockPermanentSlot(int slot) { if (slot >= 1 && slot <= 4) permanentSlots.add(slot); }
    public void lockSlot(int slot) { if (slot >= 1 && slot <= 4 && !permanentSlots.contains(slot)) { unlockedSlots.remove(slot);equipped.remove(slot); } }
    public void equip(int slot, String id) { equipped.values().removeIf(id::equals); equipped.put(slot, id); }
    public void unequip(int slot) { equipped.remove(slot); }
    public void cooldown(String id, long until) { if (until <= 0) cooldowns.remove(id); else cooldowns.put(id, until); }
    public long cooldown(String id) { return cooldowns.getOrDefault(id, 0L); }

    public void resetForPrestige() {
        resetForPrestige(true, true);
    }

    public void resetForPrestige(boolean clearPurchases, boolean clearEquipped) {
        if (clearPurchases) {
            unlockedPerks.clear();
            purchases.clear();
        }
        if (clearEquipped) equipped.clear();
        cooldowns.clear();
        unlockedSlots.clear();
        unlockedSlots.addAll(permanentSlots);
    }

    public String serialize() {
        return join(unlockedPerks) + ";" + joinInts(unlockedSlots) + ";" + joinMap(equipped) + ";" + joinLongMap(purchases) + ";" + joinLongMap(cooldowns)
                + ";" + join(permanentPerks) + ";" + joinInts(permanentSlots);
    }

    public static PerkProfile deserialize(String raw) {
        PerkProfile profile = new PerkProfile();
        if (raw == null || raw.isEmpty()) return profile;
        String[] parts = raw.split(";", -1);
        if (parts.length > 0) for (String id : split(parts[0])) profile.unlockedPerks.add(id);
        if (parts.length > 1) for (String value : split(parts[1])) try { profile.unlockSlot(Integer.parseInt(value)); } catch (Exception ignored) { }
        if (parts.length > 2) for (String value : split(parts[2])) {
            int cut = value.indexOf('=');
            try { if (cut > 0) profile.equip(Integer.parseInt(value.substring(0, cut)), value.substring(cut + 1)); } catch (Exception ignored) { }
        }
        if (parts.length > 3) readLongMap(parts[3], profile.purchases);
        if (parts.length > 4) readLongMap(parts[4], profile.cooldowns);
        if (parts.length > 5) for (String id : split(parts[5])) profile.permanentPerks.add(id);
        if (parts.length > 6) for (String value : split(parts[6])) try { profile.unlockPermanentSlot(Integer.parseInt(value)); } catch (Exception ignored) { }
        return profile;
    }

    private static List<String> split(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        for (String value : raw.split(",")) if (!value.isEmpty()) values.add(value);
        return values;
    }
    private static String join(Iterable<String> values) { List<String> out=new ArrayList<>();for(String value:values)out.add(value);return String.join(",",out); }
    private static String joinInts(Iterable<Integer> values) { List<String> out=new ArrayList<>();for(Integer value:values)out.add(String.valueOf(value));return String.join(",",out); }
    private static String joinMap(Map<Integer,String> values) { List<String> out=new ArrayList<>();for(Map.Entry<Integer,String> value:values.entrySet())out.add(value.getKey()+"="+value.getValue());return String.join(",",out); }
    private static String joinLongMap(Map<String,Long> values) { List<String> out=new ArrayList<>();for(Map.Entry<String,Long> value:values.entrySet())out.add(value.getKey()+"="+value.getValue());return String.join(",",out); }
    private static void readLongMap(String raw,Map<String,Long> target){for(String value:split(raw)){int cut=value.indexOf('=');try{if(cut>0)target.put(value.substring(0,cut),Long.parseLong(value.substring(cut+1)));}catch(Exception ignored){}}}
}
