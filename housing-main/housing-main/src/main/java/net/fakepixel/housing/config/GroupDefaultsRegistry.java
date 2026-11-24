package net.fakepixel.housing.config;
import java.util.*;
public final class GroupDefaultsRegistry { private static final List<String> VALUES=Collections.unmodifiableList(Arrays.asList("owner","co-owner","resident","guest")); private GroupDefaultsRegistry(){} public static List<String> all(){return VALUES;} public static String get(String id){if(id==null)return null;for(String v:VALUES)if(v.equalsIgnoreCase(id))return v;return null;} public static boolean contains(String id){return get(id)!=null;} }
