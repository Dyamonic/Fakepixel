package net.fakepixel.housing.config;
import java.util.*;
public final class PlotSizeRegistry { private static final Map<Integer,Integer> PRICES; static{Map<Integer,Integer> m=new LinkedHashMap<>();m.put(31,0);m.put(41,1500);m.put(51,2500);m.put(101,3500);m.put(151,5000);m.put(255,10000);PRICES=Collections.unmodifiableMap(m);} private PlotSizeRegistry(){} public static Map<Integer,Integer> all(){return PRICES;} public static Integer get(int id){return PRICES.get(id);} public static boolean contains(int id){return PRICES.containsKey(id);} }
