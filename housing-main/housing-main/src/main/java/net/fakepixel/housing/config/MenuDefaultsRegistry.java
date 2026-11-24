package net.fakepixel.housing.config;
import java.util.*;
public final class MenuDefaultsRegistry { private static final Map<String,Integer> SLOTS; static{Map<String,Integer> m=new LinkedHashMap<>();m.put("owner-menu",8);m.put("guest-menu",8);m.put("cookies",4);m.put("staff",7);SLOTS=Collections.unmodifiableMap(m);} private MenuDefaultsRegistry(){} public static Map<String,Integer> all(){return SLOTS;} public static Integer get(String id){return SLOTS.get(id);} public static boolean contains(String id){return SLOTS.containsKey(id);} }
