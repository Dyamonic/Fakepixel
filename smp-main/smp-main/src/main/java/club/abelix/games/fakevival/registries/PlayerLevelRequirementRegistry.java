package club.abelix.games.fakevival.registries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerLevelRequirementRegistry {
    public PlayerLevelRequirement requirement(int level, long xpRequired) {
        PlayerLevelRequirementTier tier = getTierForLevel(level);
        if (tier == null) return null;
        return new PlayerLevelRequirement(level, xpRequired, tier.requiredSkills(), tier.requiredItems(), reward(level));
    }

    public PlayerLevelRequirementTier getTierForLevel(int level) {
        if (level < 1) return null;
        int start, end;
        if (level < 1000) { start=(level/100)*100; if(start==0)start=1; end=start==1?99:start+99; }
        else if(level < 10000) { start=(level/1000)*1000; end=start+999; }
        else { start=10000; end=Integer.MAX_VALUE; }
        return new PlayerLevelRequirementTier(start,end,end==Integer.MAX_VALUE?"10000+":start+"-"+end,
                skillRequirements(level),itemRequirements(level));
    }

    public int tierCount() { return 20; }

    public PlayerLevelStatReward totalReward(int level) {
        int reached = Math.max(0, level - 1);
        return new PlayerLevelStatReward(reached * 2.0D + (reached / 100) * 5.0D + (reached / 1000) * 25.0D,
                reached / 5 + (reached / 100) * 5.0D + (reached / 1000) * 25.0D,
                (reached / 10) * 2.0D + (reached / 1000) * 25.0D,
                reached / 25 + (reached / 1000) * 5.0D, 0.0D,
                reached / 100, reached / 50 + (reached / 1000) * 5.0D);
    }

    private PlayerLevelStatReward reward(int level) {
        boolean hundred = level % 100 == 0, prestige = level % 1000 == 0;
        return new PlayerLevelStatReward(2.0D + (hundred ? 5.0D : 0.0D) + (prestige ? 25.0D : 0.0D),
                (level % 5 == 0 ? 1.0D : 0.0D) + (hundred ? 5.0D : 0.0D) + (prestige ? 25.0D : 0.0D),
                (level % 10 == 0 ? 2.0D : 0.0D) + (prestige ? 25.0D : 0.0D),
                (level % 25 == 0 ? 1.0D : 0.0D) + (prestige ? 5.0D : 0.0D), 0.0D,
                hundred ? 1.0D : 0.0D, (level % 50 == 0 ? 1.0D : 0.0D) + (prestige ? 5.0D : 0.0D));
    }

    private Map<String, Integer> skillRequirements(int level) {
        if (level < 10) return Map.of();
        if (level < 20) return Map.of("mining", 1);
        if (level < 30) return Map.of("farming", 2);
        if (level < 40) return Map.of("combat", 3);
        if (level < 50) return Map.of("woodcutting", 4);
        if (level < 60) return Map.of("fishing", 5);
        if (level < 70) return skills(6, 0, 6, 0, 0, 0, 0);
        if (level < 80) return skills(0, 7, 0, 0, 7, 0, 0);
        if (level < 90) return skills(0, 0, 8, 8, 0, 0, 0);
        if (level < 100) return skills(9, 9, 9, 0, 0, 0, 0);
        if (level >= 1000) { int prestige = level >= 10000 ? 10 : level / 1000;
            int main = prestige == 1 ? 100 : Math.min(350, 100 + (prestige - 1) * 25); int support = prestige == 1 ? 75 : Math.min(300, main - 25);
            return skills(main,main,main,main,main,support,support); }
        int tier = level / 100;
        return switch (tier) {
            case 1 -> skills(10,10,10,0,0,0,0); case 2 -> skills(20,15,20,10,0,0,0);
            case 3 -> skills(30,0,30,20,20,0,0); case 4 -> skills(40,30,40,0,30,0,0);
            case 5 -> skills(50,40,50,35,40,0,0); case 6 -> skills(60,50,60,45,50,0,0);
            case 7 -> skills(70,60,70,55,60,30,0); case 8 -> skills(80,70,80,65,70,0,40);
            case 9 -> skills(90,80,90,75,80,50,50);
            default -> Map.of();
        };
    }

    private Map<String,Integer> skills(int mining,int farming,int combat,int fishing,int foraging,int alchemy,int enchanting) {
        Map<String,Integer> map=new LinkedHashMap<>();
        if(mining>0)map.put("mining",mining);if(farming>0)map.put("farming",farming);if(foraging>0)map.put("woodcutting",foraging);
        if(combat>0)map.put("combat",combat);if(fishing>0)map.put("fishing",fishing);if(alchemy>0)map.put("alchemy",alchemy);if(enchanting>0)map.put("enchanting",enchanting);
        return Map.copyOf(map);
    }

    private List<ItemRequirement> itemRequirements(int level) {
        if(level<100)return List.of(); int tier=level>=10000?20:level>=1000?10+level/1000-1:level/100;
        return switch(tier){
            case 1->items("enchanted_iron_ingot",16,"enchanted_wheat",16);case 2->items("enchanted_iron_ingot",32,"enchanted_oak_log",32,"enchanted_rotten_flesh",16);
            case 3->items("crystal_fragment",16,"enchanted_cod",32,"enchanted_bone",32);case 4->items("crystal_fragment",32,"shadow_cloth",16,"enchanted_spider_eye",32);
            case 5->items("ancient_dungeon_essence",64,"shadow_cloth",32,"frozen_bone",16);case 6->items("ancient_dungeon_essence",96,"toxic_vial",32,"crystal_fragment",64);
            case 7->items("ancient_dungeon_essence",128,"royal_crest_fragment",8,"shadow_cloth",64);case 8->items("ancient_dungeon_essence",160,"royal_crest_fragment",16,"frozen_bone",64,"toxic_vial",64);
            case 9->items("ancient_dungeon_essence",192,"royal_crest",1,"crystal_fragment",128,"shadow_cloth",128);
            case 10->items("prestige_core",1,"royal_crest",2,"ancient_dungeon_essence",256,"event_token",128);
            case 11->items("prestige_core",2,"ascended_crystal",1,"ancient_dungeon_essence",384,"event_token",256);
            case 12->items("prestige_core",3,"ascended_crystal",2,"royal_crest",3,"event_token",384);
            case 13->items("prestige_core",4,"ascended_crystal",3,"mythic_essence",1,"event_token",512);
            case 14->items("prestige_core",5,"ascended_crystal",4,"mythic_essence",2,"ancient_dungeon_essence",640);
            case 15->items("prestige_core",6,"ascended_crystal",5,"mythic_essence",3,"event_token",768);
            case 16->items("prestige_core",7,"ascended_crystal",6,"mythic_essence",4,"royal_crest",6);
            case 17->items("prestige_core",8,"ascended_crystal",7,"mythic_essence",5,"ancient_dungeon_essence",896);
            case 18->items("prestige_core",9,"ascended_crystal",8,"mythic_essence",6,"event_token",1000);
            default->items("prestige_core",10,"ascended_crystal",10,"mythic_essence",8,"rainbow_core",1);
        };
    }

    private List<ItemRequirement> items(Object... values){var list=new java.util.ArrayList<ItemRequirement>();for(int i=0;i<values.length;i+=2)list.add(new ItemRequirement((String)values[i],(Integer)values[i+1]));return List.copyOf(list);}

    public record PlayerLevelRequirement(int level, long xpRequired, Map<String, Integer> requiredSkills,
                                         List<ItemRequirement> requiredItems, PlayerLevelStatReward statReward) {}
    public record PlayerLevelRequirementTier(int minLevel,int maxLevel,String display,Map<String,Integer> requiredSkills,List<ItemRequirement> requiredItems) {}
    public record ItemRequirement(String customItemId, int amount) {}
    public record PlayerLevelStatReward(double health, double defense, double mana, double strength, double damage,
                                        double critChance, double critDamage) {}
}
