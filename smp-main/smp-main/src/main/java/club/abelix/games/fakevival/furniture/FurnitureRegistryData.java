package club.abelix.games.fakevival.furniture;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import java.util.List;

public final class FurnitureRegistryData {
    private FurnitureRegistryData() {}
    public static int registerAll(FurnitureRegistry registry) {
        registry.registerBuiltIn(christmas_armchair());
        registry.registerBuiltIn(christmas_big_candy());
        registry.registerBuiltIn(christmas_big_gift());
        registry.registerBuiltIn(christmas_chair());
        registry.registerBuiltIn(christmas_chicken());
        registry.registerBuiltIn(christmas_cola());
        registry.registerBuiltIn(christmas_fireplace());
        registry.registerBuiltIn(christmas_glass_ball());
        registry.registerBuiltIn(christmas_lamp_post());
        registry.registerBuiltIn(christmas_lights());
        registry.registerBuiltIn(christmas_mail_box());
        registry.registerBuiltIn(christmas_mid_candy());
        registry.registerBuiltIn(christmas_mid_gift());
        registry.registerBuiltIn(christmas_milk());
        registry.registerBuiltIn(christmas_plate());
        registry.registerBuiltIn(christmas_sleigh());
        registry.registerBuiltIn(christmas_small_candy());
        registry.registerBuiltIn(christmas_small_gift());
        registry.registerBuiltIn(christmas_snow_pyramid());
        registry.registerBuiltIn(christmas_snowman());
        registry.registerBuiltIn(christmas_sock());
        registry.registerBuiltIn(christmas_table_3x2());
        registry.registerBuiltIn(christmas_table_4x3());
        registry.registerBuiltIn(christmas_tree());
        registry.registerBuiltIn(christmas_wreath());
        registry.registerBuiltIn(default_bed_large());
        registry.registerBuiltIn(default_bed_small());
        registry.registerBuiltIn(default_bench());
        registry.registerBuiltIn(default_chandelier());
        registry.registerBuiltIn(default_chimney());
        registry.registerBuiltIn(default_decorative_plant());
        registry.registerBuiltIn(default_door_frame());
        registry.registerBuiltIn(default_enchants());
        registry.registerBuiltIn(default_fridge());
        registry.registerBuiltIn(default_glass_table());
        registry.registerBuiltIn(default_kitchen_cabinet());
        registry.registerBuiltIn(default_kitchen_cabinet_1());
        registry.registerBuiltIn(default_kitchen_cabinet_2());
        registry.registerBuiltIn(default_kitchen_chair());
        registry.registerBuiltIn(default_kitchen_sink());
        registry.registerBuiltIn(default_kitchen_table());
        registry.registerBuiltIn(default_lamp());
        registry.registerBuiltIn(default_laptop());
        registry.registerBuiltIn(default_pizza_box());
        registry.registerBuiltIn(default_sofa());
        registry.registerBuiltIn(default_sofa_large());
        registry.registerBuiltIn(default_stove());
        registry.registerBuiltIn(default_tv());
        registry.registerBuiltIn(default_wardrobe());
        registry.registerBuiltIn(default_washing_machine());
        registry.registerBuiltIn(default_window());
        registry.registerBuiltIn(default_window_curtains());
        registry.registerBuiltIn(farm_barrel());
        registry.registerBuiltIn(farm_bench());
        registry.registerBuiltIn(farm_cart());
        registry.registerBuiltIn(farm_drink_barrel());
        registry.registerBuiltIn(farm_fence());
        registry.registerBuiltIn(farm_fence_corner());
        registry.registerBuiltIn(farm_food_trough());
        registry.registerBuiltIn(farm_item_sign());
        registry.registerBuiltIn(farm_ladder());
        registry.registerBuiltIn(farm_lamp());
        registry.registerBuiltIn(farm_log_axe());
        registry.registerBuiltIn(farm_milk_tank());
        registry.registerBuiltIn(farm_pitchforks());
        registry.registerBuiltIn(farm_scarecrow());
        registry.registerBuiltIn(farm_shelf());
        registry.registerBuiltIn(farm_shovel());
        registry.registerBuiltIn(farm_tractor());
        registry.registerBuiltIn(farm_wall_item_sign());
        registry.registerBuiltIn(farm_water_trough());
        registry.registerBuiltIn(farm_water_well());
        registry.registerBuiltIn(farm_weather_vane());
        registry.registerBuiltIn(farm_windmill());
        registry.registerBuiltIn(farm_window());
        registry.registerBuiltIn(farm_woods());
        registry.registerBuiltIn(halloween_basket());
        registry.registerBuiltIn(halloween_bones());
        registry.registerBuiltIn(halloween_bottle());
        registry.registerBuiltIn(halloween_candle());
        registry.registerBuiltIn(halloween_candy());
        registry.registerBuiltIn(halloween_cauldron());
        registry.registerBuiltIn(halloween_chair());
        registry.registerBuiltIn(halloween_cobweb());
        registry.registerBuiltIn(halloween_coffin());
        registry.registerBuiltIn(halloween_coffin_stand());
        registry.registerBuiltIn(halloween_cutter());
        registry.registerBuiltIn(halloween_eye());
        registry.registerBuiltIn(halloween_fence());
        registry.registerBuiltIn(halloween_fence_corner());
        registry.registerBuiltIn(halloween_gravestone_v1());
        registry.registerBuiltIn(halloween_gravestone_v2());
        registry.registerBuiltIn(halloween_gravestone_v3());
        registry.registerBuiltIn(halloween_hand());
        registry.registerBuiltIn(halloween_pumpkin());
        registry.registerBuiltIn(halloween_pumpkin_small());
        registry.registerBuiltIn(halloween_skeleton_v1());
        registry.registerBuiltIn(halloween_skeleton_v2());
        registry.registerBuiltIn(halloween_table());
        registry.registerBuiltIn(halloween_whisk());
        registry.registerBuiltIn(modern_big_bed());
        registry.registerBuiltIn(modern_computer_chair());
        registry.registerBuiltIn(modern_computer_desk());
        registry.registerBuiltIn(modern_computer_monitor());
        registry.registerBuiltIn(modern_kitchen_cabinet());
        registry.registerBuiltIn(modern_kitchen_chair());
        registry.registerBuiltIn(modern_kitchen_table());
        registry.registerBuiltIn(modern_lamp1());
        registry.registerBuiltIn(modern_lamp2());
        registry.registerBuiltIn(modern_models_big_bed());
        registry.registerBuiltIn(modern_models_computer_chair());
        registry.registerBuiltIn(modern_models_computer_desk());
        registry.registerBuiltIn(modern_models_computer_monitor());
        registry.registerBuiltIn(modern_models_kitchen_cabinet());
        registry.registerBuiltIn(modern_models_kitchen_chair());
        registry.registerBuiltIn(modern_models_kitchen_table());
        registry.registerBuiltIn(modern_models_lamp1());
        registry.registerBuiltIn(modern_models_lamp2());
        registry.registerBuiltIn(modern_models_plant());
        registry.registerBuiltIn(modern_models_sofa());
        registry.registerBuiltIn(modern_models_sofa2());
        registry.registerBuiltIn(modern_models_table());
        registry.registerBuiltIn(modern_models_table_lamp());
        registry.registerBuiltIn(modern_models_tv());
        registry.registerBuiltIn(modern_models_upper_kitchen_cabinet());
        registry.registerBuiltIn(modern_models_wood_panel());
        registry.registerBuiltIn(modern_plant());
        registry.registerBuiltIn(modern_sofa());
        registry.registerBuiltIn(modern_sofa2());
        registry.registerBuiltIn(modern_table());
        registry.registerBuiltIn(modern_table_lamp());
        registry.registerBuiltIn(modern_tv());
        registry.registerBuiltIn(modern_upper_kitchen_cabinet());
        registry.registerBuiltIn(modern_wood_panel());
        registry.registerBuiltIn(outdoor_bbq_grill());
        registry.registerBuiltIn(outdoor_beach_towel());
        registry.registerBuiltIn(outdoor_beach_umbrella());
        registry.registerBuiltIn(outdoor_bench());
        registry.registerBuiltIn(outdoor_cabinet());
        registry.registerBuiltIn(outdoor_cart());
        registry.registerBuiltIn(outdoor_chair());
        registry.registerBuiltIn(outdoor_lawn_mower());
        registry.registerBuiltIn(outdoor_outdoor_lights());
        registry.registerBuiltIn(outdoor_plant());
        registry.registerBuiltIn(outdoor_pool_chair());
        registry.registerBuiltIn(outdoor_swing());
        registry.registerBuiltIn(outdoor_table_bench());
        registry.registerBuiltIn(outdoor_table_big());
        registry.registerBuiltIn(outdoor_table_small());
        registry.registerBuiltIn(outdoor_watering_can());
        return 150;
    }

    private static SmpFurnitureModel christmas_armchair() {
        return model("christmas_armchair", "Armchair", FurnitureDefinition.Category.DECORATIONS, "QUARTZ_STAIRS", 200, 1.360788, 1.730370, List.of(
            part("QUARTZ_PILLAR", -0.625000, 0.484375, -0.990000, 0.000f, 0.000f, -12.500f, 0.187500f, 0.515625f, 0.812500f),
            part("QUARTZ_PILLAR", -1.562500, 0.484375, -0.990000, 0.000f, 0.000f, 12.500f, 0.187500f, 0.515625f, 0.812500f),
            part("QUARTZ_PILLAR", -0.562500, 0.000000, -0.303125, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("QUARTZ_PILLAR", -1.562500, 0.000000, -0.303125, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("QUARTZ_PILLAR", -1.562500, 0.312500, -0.990625, 0.000f, 0.000f, 0.000f, 1.125000f, 0.250000f, 0.812500f),
            part("RED_CONCRETE_POWDER", -1.500000, 0.562500, -0.928125, 0.000f, 0.000f, 0.000f, 1.000000f, 0.187500f, 0.625000f),
            part("QUARTZ_PILLAR", -1.562500, 0.000000, -0.990625, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("QUARTZ_PILLAR", -0.562500, 0.000000, -0.990625, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("QUARTZ_PILLAR", -1.562500, 0.562500, -0.303125, 0.000f, 8.500f, 0.000f, 1.125000f, 1.062500f, 0.125000f),
            part("RED_CONCRETE_POWDER", -1.437500, 0.562500, -0.396875, 0.000f, 8.500f, 0.000f, 0.875000f, 0.875000f, 0.093750f),
            part("CHISELED_QUARTZ_BLOCK", -0.553981, 0.976597, -1.036250, 0.000f, 0.000f, 0.000f, 0.234375f, 0.109375f, 0.859375f),
            part("CHISELED_QUARTZ_BLOCK", -1.680394, 0.976597, -1.036250, 0.000f, 0.000f, 0.000f, 0.234375f, 0.109375f, 0.859375f),
            part("QUARTZ_PILLAR", -0.687500, 0.542870, -0.299681, 1.833f, 8.301f, -12.367f, 0.250000f, 1.187500f, 0.125000f),
            part("QUARTZ_PILLAR", -1.562500, 0.542870, -0.299681, -1.833f, 8.301f, 12.367f, 0.250000f, 1.187500f, 0.125000f),
            part("SIT", -1.000000, 0.750000, -0.553125, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel christmas_big_candy() {
        return model("christmas_big_candy", "Big candy", FurnitureDefinition.Category.DECORATIONS, "TARGET", 200, 1.250000, 3.125000, List.of(
            part("RED_WOOL", -0.718750, 0.000000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.656250, 2.625000, -0.718750, 0.000f, 0.000f, 0.000f, 1.125000f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", 0.156250, 2.437500, -0.718750, 0.000f, 0.000f, 0.000f, 0.375000f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", -0.531250, 2.812500, -0.718750, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.406250, 3.000000, -0.718750, 0.000f, 0.000f, 0.000f, 0.625000f, 0.125000f, 0.437500f),
            part("WHITE_WOOL", -0.718750, 0.187500, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.718750, 0.375000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", -0.718750, 0.562500, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.718750, 0.750000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", -0.718750, 0.937500, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.718750, 1.125000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", -0.718750, 1.312500, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.718750, 1.500000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", -0.718750, 1.687500, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.718750, 1.875000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", -0.718750, 2.062500, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", 0.156250, 2.250000, -0.718750, 0.000f, 0.000f, 0.000f, 0.375000f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", 0.156250, 2.062500, -0.718750, 0.000f, 0.000f, 0.000f, 0.375000f, 0.187500f, 0.437500f),
            part("WHITE_WOOL", -0.718750, 2.437500, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", -0.718750, 2.250000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.187500f, 0.437500f),
            part("RED_WOOL", 0.156250, 2.000000, -0.718750, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.437500f)
        ));
    }
    private static SmpFurnitureModel christmas_big_gift() {
        return model("christmas_big_gift", "Big gift", FurnitureDefinition.Category.DECORATIONS, "WAXED_COPPER_BULB", 200, 0.887500, 1.031250, List.of(
            part("RED_SHULKER_BOX", -0.875000, 0.012500, -0.875000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.687500f, 0.750000f),
            part("RED_SHULKER_BOX", -0.937500, 0.687500, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.875000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.562500, 0.000000, -0.881250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.762500f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.881250, 0.000063, -0.562500, 0.000f, 0.000f, 0.000f, 0.762500f, 0.687500f, 0.125000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.943750, 0.681250, -0.562500, 0.000f, 0.000f, 0.000f, 0.887500f, 0.200000f, 0.125000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.562500, 0.681250, -0.943750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.200625f, 0.887500f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.562500, 0.875000, -0.756250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.069375f, 0.512500f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.750000, 0.875000, -0.568750, 0.000f, 0.000f, 0.000f, 0.500000f, 0.068750f, 0.137500f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.656250, 0.937500, -0.537500, 0.000f, 0.000f, 0.000f, 0.312500f, 0.068750f, 0.075000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.531250, 0.937500, -0.662500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.325000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.546875, 0.937500, -0.553125, 0.000f, 0.000f, 0.000f, 0.093750f, 0.093750f, 0.106250f)
        ));
    }
    private static SmpFurnitureModel christmas_chair() {
        return model("christmas_chair", "Chair", FurnitureDefinition.Category.DECORATIONS, "RED_NETHER_BRICK_STAIRS", 200, 0.750000, 1.625000, List.of(
            part("SIT", -0.562500, 0.734375, -0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.010000f, 0.010000f),
            part("RED_WOOL", -0.812500, 0.687500, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.046875f, 0.500000f),
            part("OAK_WOOD", -0.250000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("OAK_WOOD", -0.250000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("OAK_WOOD", -0.875000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("OAK_WOOD", -0.875000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("OAK_WOOD", -0.875000, 0.562500, -0.875000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("OAK_WOOD", -0.875000, 0.687500, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.875000f, 0.125000f),
            part("OAK_WOOD", -0.187500, 0.687500, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.875000f, 0.125000f),
            part("OAK_WOOD", -0.812500, 1.437500, -0.250000, 0.000f, 0.000f, 0.000f, 0.625000f, 0.062500f, 0.125000f),
            part("RED_WOOL", -0.593750, 1.375000, -0.281250, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.156250f),
            part("RED_WOOL", -0.593750, 0.687500, -0.281250, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.156250f),
            part("RED_WOOL", -0.656250, 0.750000, -0.281250, 0.000f, 0.000f, 0.000f, 0.312500f, 0.625000f, 0.156250f),
            part("RED_WOOL", -0.718750, 0.812500, -0.281250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.156250f),
            part("RED_WOOL", -0.343750, 0.812500, -0.281250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.156250f),
            part("RED_WOOL", -0.781250, 0.937500, -0.281250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 0.156250f),
            part("RED_WOOL", -0.281250, 0.937500, -0.281250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 0.156250f),
            part("RED_WOOL", -0.812500, 1.000000, -0.281250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.125000f, 0.156250f),
            part("RED_WOOL", -0.218750, 1.000000, -0.281250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.125000f, 0.156250f),
            part("RED_WOOL", -0.187500, 1.562500, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.125000f),
            part("RED_WOOL", -0.875000, 1.562500, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.125000f),
            part("RED_WOOL", -0.875000, 1.562500, -0.218750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.093750f)
        ));
    }
    private static SmpFurnitureModel christmas_chicken() {
        return model("christmas_chicken", "Chicken plate", FurnitureDefinition.Category.DECORATIONS, "COOKED_CHICKEN", 200, 0.875000, 0.302494, List.of(
            part("QUARTZ_BLOCK", -0.187500, 0.015625, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.875000f),
            part("QUARTZ_BLOCK", -0.812500, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.031250f, 0.625000f),
            part("QUARTZ_BLOCK", -0.937500, 0.015625, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.875000f),
            part("QUARTZ_BLOCK", -0.812500, 0.015625, -0.937500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.031250f, 0.125000f),
            part("QUARTZ_BLOCK", -0.812500, 0.015625, -0.187500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.031250f, 0.125000f),
            part("BROWN_WOOL", -0.718750, 0.156250, -0.390625, -13.834f, -23.211f, 5.855f, 0.171875f, 0.125000f, 0.187500f),
            part("BROWN_WOOL", -0.718750, 0.031250, -0.593750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.218750f, 0.343750f),
            part("BROWN_WOOL", -0.687500, 0.031250, -0.750000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.140625f, 0.156250f),
            part("TERRACOTTA", -0.684394, 0.201616, -0.219566, -13.834f, -23.211f, 5.855f, 0.075000f, 0.075000f, 0.031250f),
            part("BONE_BLOCK", -0.670607, 0.235042, -0.191678, -13.834f, -23.211f, 5.855f, 0.031250f, 0.031250f, 0.093750f),
            part("BONE_BLOCK", -0.711029, 0.271244, -0.104280, -13.834f, -23.211f, 5.855f, 0.062500f, 0.031250f, 0.031250f),
            part("BROWN_WOOL", -0.453125, 0.156250, -0.390625, 13.834f, -23.211f, -5.855f, 0.171875f, 0.125000f, 0.187500f),
            part("TERRACOTTA", -0.390606, 0.201616, -0.219566, 13.834f, -23.211f, -5.855f, 0.075000f, 0.075000f, 0.031250f),
            part("BONE_BLOCK", -0.360643, 0.235042, -0.191678, 13.834f, -23.211f, -5.855f, 0.031250f, 0.031250f, 0.093750f),
            part("BONE_BLOCK", -0.351471, 0.271244, -0.104280, 13.834f, -23.211f, -5.855f, 0.062500f, 0.031250f, 0.031250f)
        ));
    }
    private static SmpFurnitureModel christmas_cola() {
        return model("christmas_cola", "Cola", FurnitureDefinition.Category.DECORATIONS, "OMINOUS_BOTTLE", 200, 0.250000, 0.406250, List.of(
            part("GLASS", -0.890625, 0.000000, -0.234375, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.125000f),
            part("GLASS", -0.906250, 0.031250, -0.250000, 0.000f, 0.000f, 0.000f, 0.156250f, 0.218750f, 0.015625f),
            part("GLASS", -0.906250, 0.031250, -0.109375, 0.000f, 0.000f, 0.000f, 0.156250f, 0.218750f, 0.015625f),
            part("GLASS", -0.765625, 0.031313, -0.234375, 0.000f, 0.000f, 0.000f, 0.015625f, 0.218750f, 0.125000f),
            part("GLASS", -0.906250, 0.031313, -0.234375, 0.000f, 0.000f, 0.000f, 0.015625f, 0.218750f, 0.125000f),
            part("BLACK_TERRACOTTA", -0.890625, 0.031250, -0.234375, 0.000f, 0.000f, 0.000f, 0.125000f, 0.218750f, 0.125000f),
            part("GLASS", -0.781250, 0.250062, -0.218750, 0.000f, 0.000f, 0.000f, 0.015625f, 0.031250f, 0.093750f),
            part("GLASS", -0.890625, 0.250000, -0.234375, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.015625f),
            part("GLASS", -0.890625, 0.250062, -0.218750, 0.000f, 0.000f, 0.000f, 0.015625f, 0.031250f, 0.093750f),
            part("GLASS", -0.890625, 0.250000, -0.125000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.015625f),
            part("GLASS", -0.875000, 0.281250, -0.218750, 0.000f, 0.000f, 0.000f, 0.093750f, 0.093750f, 0.015625f),
            part("GLASS", -0.875000, 0.281312, -0.203125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.093750f, 0.062500f),
            part("GLASS", -0.875000, 0.281250, -0.140625, 0.000f, 0.000f, 0.000f, 0.093750f, 0.093750f, 0.015625f),
            part("GLASS", -0.796875, 0.281312, -0.203125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.093750f, 0.062500f),
            part("BLACK_TERRACOTTA", -0.875000, 0.250000, -0.218750, 0.000f, 0.000f, 0.000f, 0.093750f, 0.031250f, 0.093750f),
            part("BLACK_TERRACOTTA", -0.859375, 0.281250, -0.203125, 0.000f, 0.000f, 0.000f, 0.062500f, 0.093750f, 0.062500f),
            part("RED_CONCRETE", -0.859375, 0.375000, -0.203125, 0.000f, 0.000f, 0.000f, 0.062500f, 0.031250f, 0.062500f),
            part("RED_CONCRETE", -0.750000, 0.118812, -0.250000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.093750f, 0.156250f),
            part("RED_CONCRETE", -0.906875, 0.118812, -0.250000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.093750f, 0.156250f),
            part("RED_CONCRETE", -0.906250, 0.118812, -0.250625, 0.000f, 0.000f, 0.000f, 0.156250f, 0.093750f, 0.010000f),
            part("SNOW_BLOCK", -0.906250, 0.212562, -0.250625, 0.000f, 0.000f, 0.000f, 0.156250f, 0.012500f, 0.010000f),
            part("SNOW_BLOCK", -0.906250, 0.106312, -0.250625, 0.000f, 0.000f, 0.000f, 0.156250f, 0.012500f, 0.010000f),
            part("SNOW_BLOCK", -0.750000, 0.212563, -0.250000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.156250f),
            part("SNOW_BLOCK", -0.750000, 0.106312, -0.250000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.156250f),
            part("SNOW_BLOCK", -0.906875, 0.106312, -0.250000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.156250f),
            part("SNOW_BLOCK", -0.906875, 0.212563, -0.250000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.156250f),
            part("SNOW_BLOCK", -0.906250, 0.212562, -0.093750, 0.000f, 0.000f, 0.000f, 0.156250f, 0.012500f, 0.010000f),
            part("RED_CONCRETE", -0.906250, 0.118812, -0.093750, 0.000f, 0.000f, 0.000f, 0.156250f, 0.093750f, 0.010000f),
            part("SNOW_BLOCK", -0.906250, 0.106312, -0.093750, 0.000f, 0.000f, 0.000f, 0.156250f, 0.012500f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel christmas_fireplace() {
        return model("christmas_fireplace", "Fireplace", FurnitureDefinition.Category.DECORATIONS, "CAMPFIRE", 200, 2.625000, 2.000000, List.of(
            part("BLACK", -1.312500, 0.312500, -0.062500, 0.000f, 0.000f, 0.000f, 1.625000f, 1.437500f, 0.062500f),
            part("BODY", 0.312500, 0.312500, -0.875000, 0.000f, 0.000f, 0.000f, 0.187500f, 1.125000f, 0.875000f),
            part("BODY", -1.500000, 0.312500, -0.875000, 0.000f, 0.000f, 0.000f, 0.187500f, 1.125000f, 0.875000f),
            part("BLACK", -1.625000, 0.125000, -1.125000, 0.000f, 0.000f, 0.000f, 2.250000f, 0.187500f, 1.125000f),
            part("BODY", -1.312500, 0.312500, -0.875000, 0.000f, 0.000f, 0.000f, 1.625000f, 0.125000f, 0.125000f),
            part("BLACK", 0.375000, 0.312500, -1.000000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.125000f, 0.062500f),
            part("SIDE", 0.312500, 1.437500, -1.000000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.312500f, 1.000000f),
            part("BLACK", -1.437500, 0.312500, -1.000000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.125000f, 0.062500f),
            part("SIDE", -1.562500, 1.437500, -1.000000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.312500f, 1.000000f),
            part("BODY", -1.687500, 1.750000, -1.000000, 0.000f, 0.000f, 0.000f, 2.375000f, 0.250000f, 1.000000f),
            part("BODY", -1.312500, 1.562500, -0.875000, 0.000f, 0.000f, 0.000f, 1.625000f, 0.187500f, 0.125000f),
            part("BLACK", -1.312500, 1.625000, -1.000000, 0.000f, 0.000f, 0.000f, 1.625000f, 0.062500f, 0.062500f),
            part("FIRE", -1.125000, 0.312500, -0.125000, 0.000f, 0.000f, 0.000f, 1.250000f, 0.875000f, 0.010000f),
            part("FIRE", -1.125000, 0.312500, -0.750000, 0.000f, 0.000f, 0.000f, 0.625000f, 0.250000f, 0.010000f),
            part("FIRE", -0.500000, 0.312500, -0.750000, 0.000f, 0.000f, 0.000f, 0.625000f, 0.250000f, 0.010000f),
            part("WOOD", -0.312500, 0.500000, -0.250000, 25.000f, -90.000f, 0.000f, 0.187500f, 0.562500f, 0.187500f),
            part("WOOD", -0.187500, 0.312500, -0.625000, 0.000f, 0.000f, 90.000f, 0.187500f, 0.562500f, 0.187500f),
            part("WOOD", -1.125000, 0.312500, -0.375000, 0.000f, 0.000f, -57.500f, 0.187500f, 0.562500f, 0.187500f),
            part("WOOD", -0.687500, 0.375000, -0.375000, 0.000f, 0.000f, -90.000f, 0.187500f, 0.562500f, 0.187500f),
            part("BLACK", -1.812500, 0.000000, -1.312500, 0.000f, 0.000f, 0.000f, 2.625000f, 0.125000f, 1.312500f),
            part("GLASS", -1.312500, 0.437500, -0.812500, 0.000f, 0.000f, 0.000f, 1.625000f, 0.750000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel christmas_glass_ball() {
        return model("christmas_glass_ball", "Glass ball", FurnitureDefinition.Category.DECORATIONS, "GLASS", 200, 0.281250, 0.343750, List.of(
            part("GLASS", -0.625000, 0.093750, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.250000f, 0.250000f),
            part("OAK_WOOD", -0.593750, 0.031250, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.187500f),
            part("OAK_WOOD", -0.640625, 0.000000, -0.640625, 0.000f, 0.000f, 0.000f, 0.281250f, 0.031250f, 0.281250f),
            part("SNOW_BLOCK", -0.609375, 0.093750, -0.609375, 0.000f, 0.000f, 0.000f, 0.218750f, 0.015625f, 0.218750f),
            part("OAK_WOOD", -0.515625, 0.109375, -0.515625, 0.000f, 0.000f, 0.000f, 0.031250f, 0.046875f, 0.031250f),
            part("GREEN_WOOL", -0.562500, 0.156250, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.125000f),
            part("GREEN_WOOL", -0.546875, 0.187500, -0.546875, 0.000f, 0.000f, 0.000f, 0.093750f, 0.031250f, 0.093750f),
            part("GREEN_WOOL", -0.531250, 0.218750, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.031250f, 0.062500f),
            part("GREEN_WOOL", -0.515625, 0.250000, -0.515625, 0.000f, 0.000f, 0.000f, 0.031250f, 0.031250f, 0.031250f),
            part("SNOW_BLOCK", -0.515625, 0.187500, -0.453125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.453125, 0.187500, -0.421875, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.453125, 0.109375, -0.453125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.453125, 0.187500, -0.515625, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.453125, 0.296875, -0.515625, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.500000, 0.218750, -0.578125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.562500, 0.187500, -0.421875, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.562500, 0.109375, -0.578125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.562500, 0.187500, -0.515625, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.562500, 0.265625, -0.515625, 0.000f, 0.000f, 0.000f, 0.015625f, 0.015625f, 0.015625f),
            part("SNOW_BLOCK", -0.562500, 0.187500, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.010000f, 0.125000f),
            part("SNOW_BLOCK", -0.546875, 0.218750, -0.546875, 0.000f, 0.000f, 0.000f, 0.093750f, 0.010000f, 0.093750f),
            part("SNOW_BLOCK", -0.531250, 0.250000, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.010000f, 0.062500f),
            part("SNOW_BLOCK", -0.515625, 0.281250, -0.515625, 0.000f, 0.000f, 0.000f, 0.031250f, 0.010000f, 0.031250f)
        ));
    }
    private static SmpFurnitureModel christmas_lamp_post() {
        return model("christmas_lamp_post", "Lamp post", FurnitureDefinition.Category.DECORATIONS, "LANTERN", 200, 0.781250, 3.750000, List.of(
            part("NETHERITE_BLOCK", -0.750000, 0.125000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.125000f, 0.500000f),
            part("NETHERITE_BLOCK", -0.687500, 0.250000, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.250000f, 0.375000f),
            part("NETHERITE_BLOCK", -0.625000, 0.500000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 1.000000f, 0.250000f),
            part("NETHERITE_BLOCK", -0.812500, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.125000f, 0.625000f),
            part("NETHERITE_BLOCK", -0.593750, 1.500000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 1.000000f, 0.187500f),
            part("NETHERITE_BLOCK", -0.625000, 2.500000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("BLACKSTONE", -0.687500, 2.625000, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.375000f),
            part("GLASS", -0.687500, 2.687500, -0.375000, 0.000f, 10.000f, 0.000f, 0.375000f, 0.500000f, 0.062500f),
            part("GLASS", -0.375000, 2.687500, -0.687500, 0.000f, 0.000f, -10.000f, 0.062500f, 0.500000f, 0.375000f),
            part("GLASS", -0.687500, 2.687500, -0.687500, 0.000f, -10.000f, 0.000f, 0.375000f, 0.500000f, 0.062500f),
            part("GLASS", -0.687500, 2.687500, -0.687500, 0.000f, 0.000f, 10.000f, 0.062500f, 0.500000f, 0.375000f),
            part("BLACKSTONE", -0.375000, 2.687500, -0.687500, 0.434f, -7.491f, -7.538f, 0.062500f, 0.500000f, 0.062500f),
            part("BLACKSTONE", -0.375000, 2.687500, -0.375000, -0.434f, 7.491f, -7.538f, 0.062500f, 0.500000f, 0.062500f),
            part("BLACKSTONE", -0.687500, 2.687500, -0.375000, 0.434f, 7.491f, 7.538f, 0.062500f, 0.500000f, 0.062500f),
            part("BLACKSTONE", -0.687500, 2.687500, -0.687500, -0.434f, -7.491f, 7.538f, 0.062500f, 0.500000f, 0.062500f),
            part("BLACKSTONE", -0.750000, 3.175000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.500000f),
            part("BLACKSTONE", -0.812500, 3.237500, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.062500f, 0.625000f),
            part("BLACKSTONE", -0.687500, 3.300000, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.375000f),
            part("LANTERN", -0.890625, 2.687500, -0.890625, 0.000f, 0.000f, 0.000f, 0.781250f, 1.062500f, 0.781250f),
            part("BLACKSTONE", -0.562500, 3.362500, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.125000f),
            part("SNOW_BLOCK", -0.812500, 0.125000, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.010000f, 0.625000f),
            part("SNOW_BLOCK", -0.750000, 0.250000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.010000f, 0.500000f),
            part("SNOW_BLOCK", -0.812500, 3.300000, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.010000f, 0.625000f),
            part("SNOW_BLOCK", -0.687500, 3.362500, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.010000f, 0.375000f),
            part("SNOW_BLOCK", -0.562500, 3.425000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.010000f, 0.125000f),
            part("SNOW_BLOCK", -0.687500, 0.500000, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.010000f, 0.375000f),
            part("NETHERITE_BLOCK", -0.781250, 3.175000, -0.781250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.125000f),
            part("NETHERITE_BLOCK", -0.781250, 3.175000, -0.343750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.125000f),
            part("NETHERITE_BLOCK", -0.343750, 3.175000, -0.343750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.125000f),
            part("NETHERITE_BLOCK", -0.343750, 3.175000, -0.781250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel christmas_lights() {
        return model("christmas_lights", "Christmas lights", FurnitureDefinition.Category.DECORATIONS, "SOUL_LANTERN", 200, 1.000000, 0.687500, List.of(
            part("WIRE", -0.125000, 0.625000, -0.006250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.010000f),
            part("WIRE", -1.000000, 0.687500, -0.006250, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.010000f),
            part("HOLDER", -0.125000, 0.687500, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("HOLDER", -0.250000, 0.500000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("HOLDER", -0.375000, 0.625000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("HOLDER", -0.500000, 0.500000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("HOLDER", -0.625000, 0.687500, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("HOLDER", -0.750000, 0.625000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("HOLDER", -0.875000, 0.750000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("HOLDER", -1.000000, 0.625000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.031250f),
            part("RED", -0.250000, 0.312500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("YELLOW", -0.375000, 0.687500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("GREEN", -0.500000, 0.312500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("RED", -0.625000, 0.750000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("BLUE", -0.750000, 0.437500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("GREEN", -0.875000, 0.812500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("YELLOW", -1.000000, 0.437500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("BLUE", -0.125000, 0.750000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.062500f),
            part("WIRE", -0.500000, 0.562500, -0.006250, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.010000f),
            part("WIRE", -0.625000, 0.625000, -0.006250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel christmas_mail_box() {
        return model("christmas_mail_box", "Mail box", FurnitureDefinition.Category.DECORATIONS, "PAPER", 200, 0.506875, 1.481250, List.of(
            part("SOUL_SOIL", -0.562500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 1.000000f, 0.125000f),
            part("RED_SHULKER_BOX", -0.656250, 1.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.312500f, 0.250000f, 0.500000f),
            part("RED_SHULKER_BOX", -0.656250, 1.250000, -0.750000, 0.000f, 0.000f, 0.000f, 0.312500f, 0.031250f, 0.500000f),
            part("RED_SHULKER_BOX", -0.625000, 1.281250, -0.750000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.031250f, 0.500000f),
            part("RED_SHULKER_BOX", -0.578125, 1.312500, -0.734375, 0.000f, 0.000f, 0.000f, 0.156250f, 0.031250f, 0.468750f),
            part("BLACK_WOOL", -0.593750, 1.171875, -0.750625, 0.000f, 0.000f, 0.000f, 0.187500f, 0.015625f, 0.010000f),
            part("SOUL_SOIL", -0.671875, 1.156250, -0.625000, 0.000f, 0.000f, 0.000f, 0.015625f, 0.281250f, 0.046875f),
            part("SOUL_SOIL", -0.671875, 1.257812, -0.445312, 0.000f, 90.000f, 0.000f, 0.015625f, 0.062500f, 0.046875f),
            part("OAK_WOOD", -0.515625, 1.343750, -0.515625, 0.000f, 0.000f, 0.000f, 0.031250f, 0.046875f, 0.031250f),
            part("GREEN_WOOL", -0.546875, 1.390625, -0.546875, 0.000f, 0.000f, 0.000f, 0.093750f, 0.021875f, 0.093750f),
            part("GREEN_WOOL", -0.531250, 1.412500, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.021875f, 0.062500f),
            part("GREEN_WOOL", -0.515625, 1.434375, -0.515625, 0.000f, 0.000f, 0.000f, 0.031250f, 0.021875f, 0.031250f),
            part("GREEN_WOOL", -0.509375, 1.456250, -0.509375, 0.000f, 0.000f, 0.000f, 0.018750f, 0.015625f, 0.018750f),
            part("GOLD_BLOCK", -0.503125, 1.471875, -0.503125, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("BLACK_WOOL", -0.662500, 1.096875, -0.749375, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.248750f),
            part("BLACK_WOOL", -0.662500, 1.109375, -0.500625, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.123750f),
            part("BLACK_WOOL", -0.662500, 1.121875, -0.376875, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.126875f),
            part("BLUE_CONCRETE", -0.662500, 1.053125, -0.718125, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("GREEN_CONCRETE", -0.662500, 1.109375, -0.655625, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("YELLOW_CONCRETE", -0.662500, 1.053125, -0.593125, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLUE_CONCRETE", -0.662500, 1.109375, -0.530625, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("GREEN_CONCRETE", -0.662500, 1.065625, -0.469375, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("YELLOW_CONCRETE", -0.662500, 1.121875, -0.406875, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLUE_CONCRETE", -0.662500, 1.078125, -0.344375, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("GREEN_CONCRETE", -0.662500, 1.134375, -0.280000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLUE_CONCRETE", -0.343750, 1.053125, -0.718125, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLACK_WOOL", -0.343750, 1.096875, -0.749375, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.248750f),
            part("GREEN_CONCRETE", -0.343750, 1.109375, -0.655625, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("YELLOW_CONCRETE", -0.343750, 1.053125, -0.593125, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLUE_CONCRETE", -0.343750, 1.109375, -0.530625, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("GREEN_CONCRETE", -0.343750, 1.065625, -0.469375, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLACK_WOOL", -0.343750, 1.109375, -0.500625, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.123750f),
            part("YELLOW_CONCRETE", -0.343750, 1.121875, -0.406875, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLACK_WOOL", -0.343750, 1.121875, -0.376875, 0.000f, 0.000f, 0.000f, 0.010000f, 0.012500f, 0.126875f),
            part("BLUE_CONCRETE", -0.343750, 1.078125, -0.344375, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("GREEN_CONCRETE", -0.343750, 1.134375, -0.280000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.043750f, 0.030000f),
            part("BLACK_WOOL", -0.662500, 1.121875, -0.250000, 0.000f, 0.000f, 0.000f, 0.325000f, 0.012500f, 0.010000f),
            part("YELLOW_CONCRETE", -0.640625, 1.078125, -0.250000, 0.000f, 0.000f, 0.000f, 0.030000f, 0.043750f, 0.010000f),
            part("BLUE_CONCRETE", -0.578125, 1.134375, -0.250000, 0.000f, 0.000f, 0.000f, 0.030000f, 0.043750f, 0.010000f),
            part("GREEN_CONCRETE", -0.515625, 1.078125, -0.250000, 0.000f, 0.000f, 0.000f, 0.030000f, 0.043750f, 0.010000f),
            part("YELLOW_CONCRETE", -0.453125, 1.134375, -0.250000, 0.000f, 0.000f, 0.000f, 0.030000f, 0.043750f, 0.010000f),
            part("BLUE_CONCRETE", -0.390625, 1.078125, -0.250000, 0.000f, 0.000f, 0.000f, 0.030000f, 0.043750f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel christmas_mid_candy() {
        return model("christmas_mid_candy", "Mid candy", FurnitureDefinition.Category.DECORATIONS, "RED_CANDLE", 200, 0.687500, 1.250000, List.of(
            part("RED_WOOL", -0.843750, 0.875000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("RED_WOOL", -0.375000, 0.875000, -0.625000, 0.000f, 0.000f, 0.000f, 0.218750f, 0.125000f, 0.250000f),
            part("WHITE_WOOL", -0.781250, 1.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.562500f, 0.125000f, 0.250000f),
            part("RED_WOOL", -0.718750, 1.125000, -0.625000, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.250000f),
            part("RED_WOOL", -0.656250, 1.187500, -0.625000, 0.000f, 0.000f, 0.000f, 0.312500f, 0.062500f, 0.250000f),
            part("WHITE_WOOL", -0.843750, 0.750000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("RED_WOOL", -0.843750, 0.625000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("WHITE_WOOL", -0.843750, 0.500000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("RED_WOOL", -0.843750, 0.375000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("WHITE_WOOL", -0.843750, 0.250000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("RED_WOOL", -0.843750, 0.125000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("WHITE_WOOL", -0.843750, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.250000f),
            part("WHITE_WOOL", -0.375000, 0.750000, -0.625000, 0.000f, 0.000f, 0.000f, 0.218750f, 0.125000f, 0.250000f)
        ));
    }
    private static SmpFurnitureModel christmas_mid_gift() {
        return model("christmas_mid_gift", "Mid gift", FurnitureDefinition.Category.DECORATIONS, "WAXED_WEATHERED_COPPER_BULB", 200, 0.887500, 0.372500, List.of(
            part("BLUE_SHULKER_BOX", -0.937500, 0.000625, -0.437500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.250000f, 0.375000f),
            part("CHISELED_QUARTZ_BLOCK", -0.390625, 0.000063, -0.443750, 0.000f, 0.000f, 0.000f, 0.093750f, 0.255625f, 0.387500f),
            part("CHISELED_QUARTZ_BLOCK", -0.943750, 0.000000, -0.296875, 0.000f, 0.000f, 0.000f, 0.887500f, 0.256250f, 0.093750f),
            part("CHISELED_QUARTZ_BLOCK", -0.384375, 0.185000, -0.209375, 0.000f, -47.500f, 0.000f, 0.081250f, 0.125000f, 0.187500f),
            part("CHISELED_QUARTZ_BLOCK", -0.384375, 0.185000, -0.418750, 0.000f, -47.500f, 0.000f, 0.081250f, 0.187500f, 0.125000f),
            part("CHISELED_QUARTZ_BLOCK", -0.378125, 0.200625, -0.312500, 0.000f, -47.500f, 0.000f, 0.068750f, 0.093750f, 0.093750f)
        ));
    }
    private static SmpFurnitureModel christmas_milk() {
        return model("christmas_milk", "Milk", FurnitureDefinition.Category.DECORATIONS, "GLASS_BOTTLE", 200, 0.250000, 0.250063, List.of(
            part("LIGHT_GRAY_STAINED_GLASS", -0.906250, 0.000000, -0.218750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.125000f),
            part("LIGHT_GRAY_STAINED_GLASS", -0.921875, 0.031250, -0.250000, 0.000f, 0.000f, 0.000f, 0.156250f, 0.218750f, 0.031250f),
            part("LIGHT_GRAY_STAINED_GLASS", -0.921875, 0.031250, -0.093750, 0.000f, 0.000f, 0.000f, 0.156250f, 0.218750f, 0.031250f),
            part("LIGHT_GRAY_STAINED_GLASS", -0.781250, 0.031313, -0.234375, 0.000f, 0.000f, 0.000f, 0.031250f, 0.218750f, 0.156250f),
            part("LIGHT_GRAY_STAINED_GLASS", -0.937500, 0.031313, -0.234375, 0.000f, 0.000f, 0.000f, 0.031250f, 0.218750f, 0.156250f),
            part("SNOW_BLOCK", -0.906250, 0.031250, -0.218750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.156250f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel christmas_plate() {
        return model("christmas_plate", "Plate", FurnitureDefinition.Category.DECORATIONS, "HEAVY_WEIGHTED_PRESSURE_PLATE", 200, 0.900000, 0.375000, List.of(
            part("QUARTZ_BLOCK", -0.312500, 0.031250, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.625000f),
            part("QUARTZ_BLOCK", -0.687500, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.031250f, 0.375000f),
            part("IRON_BLOCK", -0.937500, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.031250f, 0.500000f),
            part("IRON_BLOCK", -0.875000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.018750f, 0.031250f, 0.187500f),
            part("IRON_BLOCK", -0.937500, 0.000000, -0.437500, 0.000f, 0.000f, 0.000f, 0.059375f, 0.031250f, 0.031250f),
            part("IRON_BLOCK", -0.937500, 0.000000, -0.406250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.031250f, 0.031250f),
            part("IRON_BLOCK", -0.125000, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.031250f, 0.375000f),
            part("IRON_BLOCK", -0.053125, 0.000000, -0.546875, 0.000f, 0.000f, 0.000f, 0.015625f, 0.031250f, 0.171875f),
            part("IRON_BLOCK", -0.100000, 0.000000, -0.546875, 0.000f, 0.000f, 0.000f, 0.015625f, 0.031250f, 0.171875f),
            part("IRON_BLOCK", -0.146875, 0.000000, -0.546875, 0.000f, 0.000f, 0.000f, 0.015625f, 0.031250f, 0.171875f),
            part("IRON_BLOCK", -0.146875, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.031250f, 0.015625f),
            part("QUARTZ_BLOCK", -0.812500, 0.031250, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.625000f),
            part("QUARTZ_BLOCK", -0.687500, 0.031250, -0.937500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.031250f, 0.125000f),
            part("QUARTZ_BLOCK", -0.687500, 0.031250, -0.437500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.031250f, 0.125000f),
            part("AIR", -0.500000, -0.093750, -0.406250, 0.000f, 90.000f, 0.000f, 0.375000f, 0.375000f, 0.031250f)
        ));
    }
    private static SmpFurnitureModel christmas_sleigh() {
        return model("christmas_sleigh", "Sleigh", FurnitureDefinition.Category.DECORATIONS, "CHEST_MINECART", 200, 2.625000, 1.468750, List.of(
            part("SKI", -0.312500, 0.078125, -1.625000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.078125f, 0.125000f),
            part("SKI", -0.312500, 0.000000, -1.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.078125f, 2.437500f),
            part("SKI", -0.312500, 0.000000, 0.875000, 0.000f, -20.000f, 0.000f, 0.125000f, 0.078125f, 0.125000f),
            part("SKI", -1.812500, 0.000000, 0.875000, 0.000f, -20.000f, 0.000f, 0.125000f, 0.078125f, 0.125000f),
            part("SKI", -1.812500, 0.078125, -1.625000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.078125f, 0.125000f),
            part("SKI", -1.812500, 0.000000, -1.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.078125f, 2.437500f),
            part("SKI", -0.312500, 0.156250, -1.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.078125f, 0.187500f),
            part("SKI", -1.812500, 0.156250, -1.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.078125f, 0.187500f),
            part("HOLDER", -0.312500, 0.078125, -1.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.109375f, 0.125000f),
            part("HOLDER", -0.312500, 0.078125, 0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.109375f, 0.125000f),
            part("HOLDER", -1.812500, 0.078125, 0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.109375f, 0.125000f),
            part("HOLDER", -1.812500, 0.078125, -1.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.109375f, 0.125000f),
            part("BODY", -0.312500, 0.187500, -1.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.625000f, 0.125000f),
            part("BODY", -1.687500, 0.187500, -1.062500, 0.000f, 0.000f, 0.000f, 1.375000f, 0.562500f, 0.125000f),
            part("BODY", -1.812500, 0.187500, -1.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.625000f, 0.125000f),
            part("BODY", -0.312500, 0.812500, -1.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.109375f, 0.250000f),
            part("BODY", -1.812500, 0.812500, -1.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.109375f, 0.250000f),
            part("BODY", -1.687500, 0.750000, -1.187500, 0.000f, 0.000f, 0.000f, 1.375000f, 0.109375f, 0.250000f),
            part("BODY", -1.812500, 0.187500, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.750000f),
            part("BODY", -0.312500, 0.187500, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.750000f),
            part("BODY", -1.687500, 0.187500, -0.937500, 0.000f, 0.000f, 0.000f, 1.375000f, 0.062500f, 1.625000f),
            part("BODY", -1.812500, 0.187500, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.500000f, 0.312500f),
            part("BODY", -0.312500, 0.187500, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.500000f, 0.312500f),
            part("BODY", -0.312500, 0.187500, 0.125000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.937500f, 0.750000f),
            part("BODY", -1.812500, 0.187500, 0.125000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.937500f, 0.750000f),
            part("BODY", -1.687500, 0.187500, 0.687500, 0.000f, 0.000f, 0.000f, 1.375000f, 0.937500f, 0.125000f),
            part("COMFORT", -0.375000, 1.125000, 0.062500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.875000f),
            part("COMFORT", -0.375000, 0.687500, -0.250000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.375000f),
            part("COMFORT", -1.875000, 0.687500, -0.250000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.375000f),
            part("COMFORT", -1.875000, 1.125000, 0.062500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.875000f),
            part("CHEST", -1.625000, 0.250000, -0.125000, -180.000f, 0.000f, 0.000f, 1.250000f, 1.062500f, 0.812500f),
            part("CHAIR", -1.156250, -0.156250, -0.625000, 90.000f, 90.000f, 0.000f, 0.312500f, 1.250000f, 0.437500f),
            part("BODY", -1.625000, 1.125000, 0.687500, 0.000f, 15.000f, 0.000f, 1.250000f, 0.187500f, 0.125000f),
            part("BODY", -1.687500, 0.765625, -1.281250, 0.000f, -27.500f, 0.000f, 1.375000f, 0.046875f, 0.093750f),
            part("BODY", -1.687500, 0.656250, -1.312500, 0.000f, 0.000f, 0.000f, 1.375000f, 0.109375f, 0.062500f),
            part("BODY", -1.687500, 0.593750, -1.250000, 0.000f, 0.000f, 0.000f, 1.375000f, 0.062500f, 0.125000f),
            part("BLANKET", -1.640625, 0.687500, -0.500000, 0.000f, 0.000f, 0.000f, 1.281250f, 0.015625f, 0.250000f),
            part("BLANKET", -0.375000, 0.562500, -0.500000, 0.000f, 0.000f, 0.000f, 0.015625f, 0.125000f, 0.250000f),
            part("BLANKET", -1.640625, 0.562500, -0.500000, 0.000f, 0.000f, 0.000f, 0.015625f, 0.125000f, 0.250000f),
            part("LINING", -1.825000, 0.250000, 0.187500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.812500f, 0.031250f),
            part("LINING", -1.825000, 0.250000, 0.781250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.812500f, 0.031250f),
            part("LINING", -1.825000, 1.031250, 0.218750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -1.825000, 0.250000, 0.218750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -0.187500, 0.250000, 0.781250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.812500f, 0.031250f),
            part("LINING", -0.187500, 0.250000, 0.218750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -0.187500, 0.250000, 0.187500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.812500f, 0.031250f),
            part("LINING", -0.187500, 1.031250, 0.218750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -0.187500, 0.593750, -0.093750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.125000f),
            part("LINING", -0.187500, 0.250000, 0.031250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.375000f, 0.031250f),
            part("LINING", -0.187500, 0.250000, -0.093750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.125000f),
            part("LINING", -0.187500, 0.250000, -0.125000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.375000f, 0.031250f),
            part("LINING", -1.818750, 0.250000, 0.031250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.375000f, 0.031250f),
            part("LINING", -1.818750, 0.593750, -0.093750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.125000f),
            part("LINING", -1.818750, 0.250000, -0.125000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.375000f, 0.031250f),
            part("LINING", -1.818750, 0.250000, -0.093750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.125000f),
            part("LINING", -1.818750, 0.250000, -0.281250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.187500f, 0.031250f),
            part("LINING", -1.818750, 0.406250, -0.843750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -1.818750, 0.250000, -0.875000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.187500f, 0.031250f),
            part("LINING", -1.818750, 0.250000, -0.843750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -0.187500, 0.250000, -0.875000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.187500f, 0.031250f),
            part("LINING", -0.187500, 0.250000, -0.843750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -0.187500, 0.406250, -0.843750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.031250f, 0.562500f),
            part("LINING", -0.187500, 0.250000, -0.281250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.187500f, 0.031250f),
            part("BODY", -1.687500, 0.615625, -1.306250, 0.000f, 35.000f, 0.000f, 1.375000f, 0.046875f, 0.062500f),
            part("CHAIR", -1.625000, 0.250000, -0.250000, 0.000f, 0.000f, 0.000f, 1.250000f, 0.687500f, 0.125000f),
            part("SIT", -1.000000, 0.703125, -0.312500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel christmas_small_candy() {
        return model("christmas_small_candy", "Small candy", FurnitureDefinition.Category.DECORATIONS, "WHITE_CANDLE", 200, 0.593750, 0.937500, List.of(
            part("WHITE_WOOL", -0.687500, 0.062500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("WHITE_WOOL", -0.812500, 0.125000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.687500, 0.187500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.812500, 0.250000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("WHITE_WOOL", -0.687500, 0.312500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("WHITE_WOOL", -0.812500, 0.375000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.687500, 0.437500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.812500, 0.500000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.812500, 0.062500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.062500f),
            part("WHITE_WOOL", -0.687500, 0.562500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("WHITE_WOOL", -0.812500, 0.625000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.687500, 0.687500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.061875f),
            part("RED_WOOL", -0.812500, 0.750000, -0.061875, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.061875f),
            part("WHITE_WOOL", -0.562500, 0.750000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("WHITE_WOOL", -0.687500, 0.812500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.437500, 0.625000, -0.062500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.062500f, 0.062500f),
            part("WHITE_WOOL", -0.328125, 0.625000, -0.062500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.125000f, 0.062500f),
            part("WHITE_WOOL", -0.437500, 0.687500, -0.062500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.328125, 0.750000, -0.062500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.437500, 0.812500, -0.062500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.125000f, 0.062500f),
            part("RED_WOOL", -0.562500, 0.875000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel christmas_small_gift() {
        return model("christmas_small_gift", "Small gift", FurnitureDefinition.Category.DECORATIONS, "WAXED_EXPOSED_COPPER_BULB", 200, 0.468750, 0.538125, List.of(
            part("YELLOW_SHULKER_BOX", -0.937500, 0.000313, -0.437500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.312500f, 0.375000f),
            part("GREEN_WOOL", -0.968750, 0.312500, -0.468750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.093750f, 0.437500f),
            part("YELLOW_SHULKER_BOX", -0.975000, 0.306875, -0.281250, 0.000f, 0.000f, 0.000f, 0.450000f, 0.106250f, 0.062500f),
            part("YELLOW_SHULKER_BOX", -0.781250, 0.306250, -0.475000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.106250f, 0.450000f),
            part("GREEN_WOOL", -0.943750, 0.000000, -0.281250, 0.000f, 0.000f, 0.000f, 0.387500f, 0.312500f, 0.062500f),
            part("GREEN_WOOL", -0.781250, 0.000063, -0.443750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.387500f),
            part("YELLOW_SHULKER_BOX", -0.693750, 0.350625, -0.275000, 0.000f, 0.000f, 45.000f, 0.187500f, 0.125000f, 0.050000f),
            part("YELLOW_SHULKER_BOX", -0.931250, 0.350625, -0.275000, 0.000f, 0.000f, 45.000f, 0.125000f, 0.187500f, 0.050000f),
            part("YELLOW_SHULKER_BOX", -0.818750, 0.406875, -0.268750, 0.000f, 0.000f, 45.000f, 0.068750f, 0.068750f, 0.037500f)
        ));
    }
    private static SmpFurnitureModel christmas_snow_pyramid() {
        return model("christmas_snow_pyramid", "Snow pyramid", FurnitureDefinition.Category.DECORATIONS, "BONE_MEAL", 200, 0.812500, 0.750000, List.of(
            part("SNOW", -0.593750, 0.000000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.906250, 0.000000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.281250, 0.000000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.781250, 0.000000, -0.843750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.593750, 0.000000, -0.312500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.593750, 0.187500, -0.453125, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.750000, 0.187500, -0.703125, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.859375, 0.000000, -0.343750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.828125, 0.187500, -0.484375, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.718750, 0.375000, -0.578125, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.390625, 0.000000, -0.906250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("SNOW", -0.343750, 0.000000, -0.343750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.406250, 0.187500, -0.343750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.531250, 0.375000, -0.406250, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.656250, 0.562500, -0.453125, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.703125, 0.187438, -0.328125, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("SNOW", -0.781250, 0.375000, -0.390625, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f)
        ));
    }
    private static SmpFurnitureModel christmas_snowman() {
        return model("christmas_snowman", "Snowman", FurnitureDefinition.Category.DECORATIONS, "SNOW_BLOCK", 200, 2.247706, 2.125000, List.of(
            part("BODY", -0.875000, 0.062500, -0.875000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.750000f, 0.750000f),
            part("BODY", -0.812500, 0.875000, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.625000f, 0.625000f),
            part("BODY", -0.750000, 1.562500, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.500000f),
            part("BODY", -0.937500, 0.125000, -0.812500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.625000f, 0.625000f),
            part("BODY", -0.125000, 0.125000, -0.812500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.625000f, 0.625000f),
            part("BODY", -0.812500, 0.125000, -0.937500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.625000f, 0.062500f),
            part("BODY", -0.812500, 0.125000, -0.125000, 0.000f, 0.000f, 0.000f, 0.625000f, 0.625000f, 0.062500f),
            part("BODY", -0.812500, 0.812500, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.062500f, 0.625000f),
            part("BODY", -0.875000, 0.937500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.500000f),
            part("BODY", -0.187500, 0.937500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.500000f),
            part("BODY", -0.750000, 0.937500, -0.875000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.062500f),
            part("BODY", -0.750000, 0.937500, -0.187500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.062500f),
            part("BODY", -0.687500, 1.500000, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.375000f),
            part("BODY", -0.687500, 0.250000, -1.000000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.062500f),
            part("BODY", -0.812500, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.062500f, 0.625000f),
            part("BODY", -0.687500, 0.250000, -0.062500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.062500f),
            part("BODY", -1.000000, 0.250000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.375000f),
            part("BODY", -0.062500, 0.250000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.375000f),
            part("BODY", -0.125000, 1.062500, -0.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 0.250000f),
            part("BODY", -0.625000, 1.062500, -0.937500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.250000f, 0.062500f),
            part("BODY", -0.625000, 1.062500, -0.125000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.250000f, 0.062500f),
            part("BODY", -0.937500, 1.062500, -0.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 0.250000f),
            part("BODY", -0.812500, 1.625000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.375000f),
            part("BODY", -0.250000, 1.625000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.375000f),
            part("BODY", -0.687500, 1.625000, -0.812500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.062500f),
            part("BODY", -0.687500, 1.625000, -0.250000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.062500f),
            part("BODY", -0.687500, 2.062500, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.375000f),
            part("NOSE", -0.562500, 1.671875, -1.000000, 0.000f, 0.000f, -45.000f, 0.125000f, 0.125000f, 0.187500f),
            part("NOSE", -0.531250, 1.703125, -1.125000, 0.000f, 0.000f, -45.000f, 0.062500f, 0.062500f, 0.125000f),
            part("ARM", -1.250000, 1.312500, -0.531250, 0.000f, 0.000f, 50.000f, 0.375000f, 0.062500f, 0.062500f),
            part("ARM", -1.498853, 1.020523, -0.531256, 0.000f, 0.000f, 65.000f, 0.375000f, 0.062500f, 0.062500f),
            part("ARM", -1.263958, 0.626825, -0.832296, 0.000f, -55.000f, 65.000f, 0.062500f, 0.062500f, 0.125000f),
            part("ARM", -1.623853, 1.020523, -0.593750, 0.000f, 0.000f, 65.000f, 0.125000f, 0.062500f, 0.125000f),
            part("ARM", -1.504471, 0.581109, -0.531256, 0.000f, 0.000f, 67.500f, 0.187500f, 0.062500f, 0.062500f),
            part("ARM", -1.441115, 0.324583, -0.593750, 0.000f, 0.000f, 160.000f, 0.187500f, 0.062500f, 0.062500f),
            part("ARM", -1.317725, 0.659568, -0.718750, -0.000f, -25.000f, 65.000f, 0.062500f, 0.062500f, 0.125000f),
            part("ARM", -1.566971, 0.581109, -0.593756, 0.000f, 0.000f, 102.500f, 0.250000f, 0.062500f, 0.062500f),
            part("ARM", -1.580531, 0.391434, -0.531250, 0.000f, 0.000f, 102.500f, 0.187500f, 0.062500f, 0.062500f),
            part("EYE_B", -0.656250, 1.875000, -0.859375, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.046875f),
            part("EYE_B", -0.468750, 1.875000, -0.859375, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.046875f),
            part("EYE", -0.625000, 1.812500, -0.859375, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.046875f),
            part("EYE", -0.437500, 1.812500, -0.859375, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.046875f),
            part("EYE", -0.562500, 1.125000, -0.953125, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.015625f),
            part("EYE", -0.562500, 0.375000, -1.015625, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.015625f),
            part("ARM", -0.125000, 1.312500, -0.531250, 0.000f, 0.000f, -50.000f, 0.375000f, 0.062500f, 0.062500f),
            part("ARM", 0.123853, 1.020523, -0.531256, 0.000f, 0.000f, -65.000f, 0.375000f, 0.062500f, 0.062500f),
            part("ARM", 0.498853, 1.020523, -0.593750, 0.000f, 0.000f, -65.000f, 0.125000f, 0.062500f, 0.125000f),
            part("ARM", 0.255225, 0.659568, -0.718750, 0.000f, -25.000f, -65.000f, 0.062500f, 0.062500f, 0.125000f),
            part("ARM", 0.201458, 0.626825, -0.832296, 0.000f, -55.000f, -65.000f, 0.062500f, 0.062500f, 0.125000f),
            part("ARM", 0.316971, 0.581109, -0.593756, 0.000f, 0.000f, -102.500f, 0.250000f, 0.062500f, 0.062500f),
            part("ARM", 0.253615, 0.324583, -0.593750, 0.000f, 0.000f, -160.000f, 0.187500f, 0.062500f, 0.062500f),
            part("ARM", 0.316971, 0.581109, -0.531256, 0.000f, 0.000f, -67.500f, 0.187500f, 0.062500f, 0.062500f),
            part("ARM", 0.393031, 0.391434, -0.531250, 0.000f, 0.000f, -102.500f, 0.187500f, 0.062500f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel christmas_sock() {
        return model("christmas_sock", "Sock", FurnitureDefinition.Category.DECORATIONS, "RED_DYE", 200, 0.578125, 0.756250, List.of(
            part("RED_WOOL", -0.561730, 0.500000, -0.078125, 0.000f, 0.000f, -17.500f, 0.281250f, 0.375000f, 0.078125f),
            part("RED_WOOL", -0.374230, 0.250000, -0.078125, 0.000f, 0.000f, -17.500f, 0.281250f, 0.250000f, 0.078125f),
            part("RED_WOOL", -0.561730, 0.437500, -0.078125, 0.000f, 0.000f, -17.500f, 0.187500f, 0.062500f, 0.078125f),
            part("WHITE_WOOL", -0.505480, 0.243750, -0.087500, 0.000f, 0.000f, -17.500f, 0.131250f, 0.193750f, 0.087500f),
            part("WHITE_WOOL", -0.567980, 0.312500, -0.087500, 0.000f, 0.000f, -17.500f, 0.062500f, 0.125000f, 0.087500f),
            part("RED_WOOL", -0.092980, 0.312500, -0.078125, 0.000f, 0.000f, -17.500f, 0.062500f, 0.125000f, 0.078125f),
            part("WHITE_WOOL", -0.608605, 0.875000, -0.103125, 0.000f, 0.000f, -17.500f, 0.375000f, 0.125000f, 0.103125f)
        ));
    }
    private static SmpFurnitureModel christmas_table_3x2() {
        return model("christmas_table_3x2", "Table 3x2", FurnitureDefinition.Category.DECORATIONS, "ENCHANTING_TABLE", 200, 3.000000, 1.000000, List.of(
            part("SPRUCE_PLANKS", -2.000000, 0.859375, -0.984375, 0.000f, 0.000f, 0.000f, 3.000000f, 0.125000f, 1.968750f),
            part("RED_WOOL", -1.703125, 0.593750, 0.984375, 0.000f, 0.000f, 0.000f, 2.406250f, 0.390625f, 0.015625f),
            part("SPRUCE_PLANKS", 0.500000, 0.000000, 0.531250, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("SPRUCE_PLANKS", 0.531250, 0.046875, 0.562500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.812500f, 0.187500f),
            part("SPRUCE_PLANKS", 0.500000, 0.000000, -0.718750, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("SPRUCE_PLANKS", 0.531250, 0.046875, -0.687500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.812500f, 0.187500f),
            part("SPRUCE_PLANKS", -1.718750, 0.046875, 0.562500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.812500f, 0.187500f),
            part("SPRUCE_PLANKS", -1.750000, 0.000000, 0.531250, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("SPRUCE_PLANKS", -1.718750, 0.046875, -0.687500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.812500f, 0.187500f),
            part("SPRUCE_PLANKS", -1.750000, 0.000000, -0.718750, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("WHITE_WOOL", -1.890625, 0.593750, -1.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("WHITE_WOOL", 0.703125, 0.593750, -1.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("WHITE_WOOL", -1.890625, 0.593750, 0.984375, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("WHITE_WOOL", 0.703125, 0.593750, 0.984375, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("WHITE_WOOL", -1.890625, 0.984375, -1.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.015625f, 2.000000f),
            part("WHITE_WOOL", 0.703125, 0.984375, -1.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.015625f, 2.000000f),
            part("RED_WOOL", -1.703125, 0.593750, -1.000000, 0.000f, 0.000f, 0.000f, 2.406250f, 0.390625f, 0.015625f),
            part("RED_WOOL", -1.703125, 0.984375, -1.000000, 0.000f, 0.000f, 0.000f, 2.406250f, 0.015625f, 2.000000f)
        ));
    }
    private static SmpFurnitureModel christmas_table_4x3() {
        return model("christmas_table_4x3", "Table 4x3", FurnitureDefinition.Category.DECORATIONS, "ENCHANTING_TABLE", 200, 4.000000, 1.187500, List.of(
            part("SPRUCE_PLANKS", -2.000000, 0.984375, -1.984375, 0.000f, 0.000f, 0.000f, 4.000000f, 0.187500f, 2.968750f),
            part("DRIED_KELP_BLOCK", -1.703125, 0.781250, 0.984375, 0.000f, 0.000f, 0.000f, 3.406250f, 0.390625f, 0.015625f),
            part("SPRUCE_PLANKS", -1.687500, 0.046875, 0.343750, 0.000f, 0.000f, 0.000f, 0.250000f, 0.937500f, 0.250000f),
            part("SPRUCE_PLANKS", -1.734375, 0.000000, 0.296875, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("DRIED_KELP_BLOCK", -1.890625, 0.781250, -2.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("DRIED_KELP_BLOCK", 1.703125, 0.781250, -2.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("DRIED_KELP_BLOCK", -1.890625, 0.781250, 0.984375, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("DRIED_KELP_BLOCK", 1.703125, 0.781250, 0.984375, 0.000f, 0.000f, 0.000f, 0.187500f, 0.390625f, 0.015625f),
            part("DRIED_KELP_BLOCK", -1.890625, 1.171875, -2.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.015625f, 3.000000f),
            part("DRIED_KELP_BLOCK", 1.703125, 1.171875, -2.000000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.015625f, 3.000000f),
            part("DRIED_KELP_BLOCK", -1.703125, 0.781250, -2.000000, 0.000f, 0.000f, 0.000f, 3.406250f, 0.390625f, 0.015625f),
            part("DRIED_KELP_BLOCK", -1.703125, 1.171875, -2.000000, 0.000f, 0.000f, 0.000f, 3.406250f, 0.015625f, 3.000000f),
            part("SPRUCE_PLANKS", -1.687500, 0.046875, -1.593750, 0.000f, 0.000f, 0.000f, 0.250000f, 0.937500f, 0.250000f),
            part("SPRUCE_PLANKS", -1.734375, 0.000000, -1.640625, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("SPRUCE_PLANKS", 1.437500, 0.046875, -1.593750, 0.000f, 0.000f, 0.000f, 0.250000f, 0.937500f, 0.250000f),
            part("SPRUCE_PLANKS", 1.390625, 0.000000, -1.640625, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("SPRUCE_PLANKS", 1.390625, 0.000000, 0.296875, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("SPRUCE_PLANKS", 1.437500, 0.046875, 0.343750, 0.000f, 0.000f, 0.000f, 0.250000f, 0.937500f, 0.250000f)
        ));
    }
    private static SmpFurnitureModel christmas_tree() {
        return model("christmas_tree", "Christmas tree", FurnitureDefinition.Category.DECORATIONS, "SPRUCE_SAPLING", 200, 2.444847, 3.078125, List.of(
            part("OAK_LOG", -0.625000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 2.359375f, 0.250000f),
            part("OAK_LOG", -0.562500, 2.312500, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.234375f, 0.125000f),
            part("WHITE", -1.716787, 0.736585, -0.932530, 0.000f, 0.000f, 35.000f, 0.875000f, 0.250000f, 0.875000f),
            part("WHITE", -0.149841, 0.729349, -0.932530, 0.000f, 0.000f, -35.000f, 0.875000f, 0.250000f, 0.875000f),
            part("WHITE", -0.938833, 0.727712, -1.721621, 0.000f, -35.000f, 0.000f, 0.875000f, 0.250000f, 0.875000f),
            part("WHITE", -0.938833, 0.723373, -0.151775, 0.000f, 35.000f, 0.000f, 0.875000f, 0.250000f, 0.875000f),
            part("WHITE", -1.250000, 0.437500, -1.312500, 0.000f, 0.000f, 0.000f, 1.500000f, 0.187500f, 1.578125f),
            part("WHITE", -1.125000, 0.625000, -1.125000, 0.000f, 0.000f, 0.000f, 1.250000f, 0.125000f, 1.265625f),
            part("WHITE", -1.437500, 0.250000, -1.437500, 0.000f, 0.000f, 0.000f, 1.875000f, 0.187500f, 1.875000f),
            part("WHITE", -1.584819, 1.427697, -0.859375, 0.000f, 0.000f, 45.000f, 0.812500f, 0.250000f, 0.718750f),
            part("WHITE", -0.217155, 1.417299, -0.859375, 0.000f, 0.000f, -45.000f, 0.812500f, 0.250000f, 0.718750f),
            part("WHITE", -0.860708, 1.415212, -1.596621, 0.000f, -45.000f, 0.000f, 0.718750f, 0.250000f, 0.812500f),
            part("WHITE", -0.860708, 1.410636, -0.226927, 0.000f, 45.000f, 0.000f, 0.718750f, 0.250000f, 0.812500f),
            part("WHITE", -0.906250, 0.750000, -0.843750, 0.000f, 0.000f, 0.000f, 0.750000f, 0.750000f, 0.687500f),
            part("WHITE", -1.000000, 1.218750, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 1.015625f),
            part("WHITE", -1.125000, 1.125000, -1.125000, 0.000f, 0.000f, 0.000f, 1.250000f, 0.093750f, 1.265625f),
            part("WHITE", -1.250000, 1.031250, -1.250000, 0.000f, 0.000f, 0.000f, 1.500000f, 0.093750f, 1.500000f),
            part("WHITE", -0.750000, 1.437500, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.750000f, 0.500000f),
            part("WHITE", -0.937500, 1.875000, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.125000f, 0.890625f),
            part("WHITE", -1.000000, 1.781250, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.093750f, 1.015625f),
            part("WHITE", -1.062500, 1.687500, -1.062500, 0.000f, 0.000f, 0.000f, 1.125000f, 0.093750f, 1.125000f),
            part("WHITE", -1.459819, 2.115197, -0.859375, 0.000f, 0.000f, 50.000f, 0.812500f, 0.250000f, 0.718750f),
            part("WHITE", -0.342155, 2.104799, -0.859375, 0.000f, 0.000f, -50.000f, 0.812500f, 0.250000f, 0.718750f),
            part("WHITE", -0.860708, 2.102712, -1.471621, 0.000f, -50.000f, 0.000f, 0.718750f, 0.250000f, 0.812500f),
            part("WHITE", -0.860708, 2.098136, -0.351927, 0.000f, 50.000f, 0.000f, 0.718750f, 0.250000f, 0.812500f),
            part("WHITE", -0.781250, 2.187500, -0.781250, 0.000f, 0.000f, 0.000f, 0.562500f, 0.187500f, 0.562500f),
            part("WHITE", -0.593750, 2.500000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.078125f, 0.187500f),
            part("WHITE", -0.718750, 2.375000, -0.718750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.078125f, 0.437500f),
            part("GOLD_BLOCK", -0.593750, 2.578125, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.187500f),
            part("GOLD_BLOCK", -0.531250, 2.640625, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("GOLD_BLOCK", -0.531250, 2.828125, -0.718750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.437500f),
            part("GOLD_BLOCK", -0.531250, 2.765625, -0.593750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.187500f, 0.187500f),
            part("GOLD_BLOCK", -0.593750, 2.765625, -0.531250, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.062500f),
            part("GOLD_BLOCK", -0.718750, 2.828125, -0.531250, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.062500f),
            part("GOLD_BLOCK", -0.593750, 2.828125, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.187500f),
            part("REDSTONE_BLOCK", -1.140625, 1.875000, -0.437500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("LAPIS_BLOCK", -1.484375, 0.546875, -0.500000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("REDSTONE_BLOCK", -0.437500, 0.562500, 0.281250, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("REDSTONE_BLOCK", 0.000000, 1.812500, -0.406250, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("REDSTONE_BLOCK", -0.781250, 1.125000, -1.390625, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("LAPIS_BLOCK", 0.296875, 0.562500, -0.453125, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("YELLOW_GLAZED_TERRACOTTA", -0.468750, 0.546875, -1.468750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("LAPIS_BLOCK", -1.281250, 0.906250, -1.281250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("YELLOW_GLAZED_TERRACOTTA", -1.093750, 1.562500, -1.093750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("REDSTONE_BLOCK", -0.031250, 1.562500, -1.093750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("YELLOW_GLAZED_TERRACOTTA", 0.156250, 0.906250, -1.281250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("LAPIS_BLOCK", -0.031250, 1.562500, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("REDSTONE_BLOCK", 0.156250, 0.906250, 0.156250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("LAPIS_BLOCK", -0.531250, 1.796875, -1.203125, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("YELLOW_GLAZED_TERRACOTTA", 0.078125, 1.250000, -0.718750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("WHITE", -0.656250, 2.437500, -0.656250, 0.000f, 0.000f, 0.000f, 0.312500f, 0.078125f, 0.312500f),
            part("YELLOW_GLAZED_TERRACOTTA", -0.468750, 1.937500, -0.093750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("REDSTONE_BLOCK", -1.093750, 1.562500, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("YELLOW_GLAZED_TERRACOTTA", -1.281250, 0.906250, 0.156250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("LAPIS_BLOCK", -0.687500, 1.250000, 0.093750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("YELLOW_GLAZED_TERRACOTTA", -1.312500, 1.218750, -0.687500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f)
        ));
    }
    private static SmpFurnitureModel christmas_wreath() {
        return model("christmas_wreath", "Wreath", FurnitureDefinition.Category.DECORATIONS, "AZALEA_LEAVES", 200, 1.046875, 1.000000, List.of(
            part("GREEN_SHULKER_BOX", -1.000000, 0.390625, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.218750f, 0.187500f),
            part("GREEN_SHULKER_BOX", -0.187500, 0.390625, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.218750f, 0.187500f),
            part("GREEN_SHULKER_BOX", -0.312500, 0.140625, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("GREEN_SHULKER_BOX", -0.859375, 0.140625, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("GREEN_SHULKER_BOX", -0.437500, 0.046875, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("GREEN_SHULKER_BOX", -0.734375, 0.046875, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("GREEN_SHULKER_BOX", -0.265625, 0.234375, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("GREEN_SHULKER_BOX", -0.921875, 0.234375, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("SWEET_BERRIES", -0.109375, 0.500000, -0.187500, 0.000f, 0.000f, 0.000f, 0.156250f, 0.156250f, 0.015625f),
            part("SWEET_BERRIES", -0.906250, 0.484375, -0.187500, 0.000f, 0.000f, 0.000f, 0.156250f, 0.156250f, 0.015625f),
            part("SWEET_BERRIES", -0.500000, 0.109375, -0.187500, 0.000f, 0.000f, 0.000f, 0.156250f, 0.156250f, 0.015625f),
            part("GLOW_BERRIES", -0.234375, 0.259375, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.015625f),
            part("GLOW_BERRIES", -0.218750, 0.765625, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.015625f),
            part("GLOW_BERRIES", -0.756250, 0.765625, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.015625f),
            part("GLOW_BERRIES", -0.740625, 0.250000, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.015625f),
            part("GREEN_SHULKER_BOX", -0.593750, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("GREEN_SHULKER_BOX", -0.593750, 0.812500, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("GREEN_SHULKER_BOX", -0.921875, 0.578125, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("GREEN_SHULKER_BOX", -0.265625, 0.578125, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("GREEN_SHULKER_BOX", -0.734375, 0.765625, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("GREEN_SHULKER_BOX", -0.437500, 0.765625, -0.171875, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.156250f),
            part("GREEN_SHULKER_BOX", -0.859375, 0.671875, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("GREEN_SHULKER_BOX", -0.312500, 0.671875, -0.187500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.187500f, 0.187500f),
            part("RED_SHULKER_BOX", -0.563550, 0.908730, -0.161337, -0.000f, -22.500f, 55.000f, 0.125000f, 0.062500f, 0.031250f),
            part("RED_SHULKER_BOX", -0.453125, 0.765625, -0.218750, 0.000f, 0.000f, -55.000f, 0.125000f, 0.187500f, 0.031250f),
            part("RED_SHULKER_BOX", -0.578125, 0.750000, -0.221875, 0.000f, 0.000f, 0.000f, 0.156250f, 0.100000f, 0.031250f),
            part("RED_SHULKER_BOX", -0.506265, 0.529688, -0.224360, -0.000f, 2.500f, 17.500f, 0.125000f, 0.250000f, 0.031250f),
            part("RED_SHULKER_BOX", -0.618429, 0.530659, -0.201350, -0.000f, -7.500f, -17.500f, 0.125000f, 0.250000f, 0.031250f),
            part("RED_SHULKER_BOX", -0.646276, 0.778951, -0.218750, 0.000f, 0.000f, 55.000f, 0.125000f, 0.125000f, 0.031250f)
        ));
    }
    private static SmpFurnitureModel default_bed_large() {
        return model("default_bed_large", "Large Bed", FurnitureDefinition.Category.FURNITURE, "WHITE_BED", 200, 3.000000, 1.597545, List.of(
            part("WOOD", 1.325000, 0.003891, -1.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -1.500000, 0.356250, -1.500000, 0.000f, 0.000f, 0.000f, 3.000000f, 0.337500f, 3.000000f),
            part("WOOD", -1.500000, 0.693750, 1.312500, 0.000f, 0.000f, 0.000f, 3.000000f, 0.531250f, 0.187500f),
            part("WOOD", 1.325000, 0.003891, 1.268750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -1.496477, 0.003891, 1.268750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -1.496477, 0.003891, -1.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", 0.625000, 1.037500, 1.325000, 0.000f, 0.000f, 45.000f, 0.312500f, 0.531250f, 0.156250f),
            part("WOOD", -0.937500, 1.037500, 1.325000, 0.000f, 0.000f, -45.000f, 0.312500f, 0.531250f, 0.156250f),
            part("WOOD", -0.708590, 1.320186, 1.331250, 0.000f, 0.000f, 0.000f, 1.412500f, 0.281250f, 0.143750f),
            part("SHEET", -1.468750, 0.668750, -1.437500, 0.000f, 0.000f, 0.000f, 2.937500f, 0.206250f, 2.443750f),
            part("PILLOW", -1.306250, 0.743750, 0.925000, 0.000f, -22.500f, 0.000f, 1.106250f, 0.206250f, 0.443750f),
            part("SHEET", -1.468750, 0.487500, 0.806250, 0.000f, 0.000f, 0.000f, 2.937500f, 0.406250f, 0.506250f),
            part("PILLOW", 0.193750, 0.743750, 0.925000, 0.000f, -22.500f, 0.000f, 1.106250f, 0.206250f, 0.443750f),
            part("SIT", 0.312500, 0.937500, 0.687500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel default_bed_small() {
        return model("default_bed_small", "Small Bed", FurnitureDefinition.Category.FURNITURE, "RED_BED", 200, 3.000000, 1.597545, List.of(
            part("WOOD", 0.571477, 0.003891, -1.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -0.812500, 0.356250, -1.500000, 0.000f, 0.000f, 0.000f, 1.625000f, 0.337500f, 3.000000f),
            part("WOOD", -0.812500, 0.693750, 1.312500, 0.000f, 0.000f, 0.000f, 1.625000f, 0.531250f, 0.187500f),
            part("WOOD", 0.571477, 0.003891, 1.268750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -0.746477, 0.003891, 1.268750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -0.746477, 0.003891, -1.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", 0.187500, 1.037500, 1.325000, 0.000f, 0.000f, 45.000f, 0.312500f, 0.531250f, 0.156250f),
            part("WOOD", -0.500000, 1.037500, 1.325000, 0.000f, 0.000f, -45.000f, 0.312500f, 0.531250f, 0.156250f),
            part("WOOD", -0.271090, 1.320186, 1.331250, 0.000f, 0.000f, 0.000f, 0.537500f, 0.281250f, 0.143750f),
            part("SHEET", -0.718750, 0.668750, -1.443750, 0.000f, 0.000f, 0.000f, 1.431250f, 0.206250f, 2.443750f),
            part("PILLOW", -0.556250, 0.743750, 0.925000, 0.000f, -22.500f, 0.000f, 1.106250f, 0.206250f, 0.443750f),
            part("SHEET", -0.731250, 0.487500, 0.806250, 0.000f, 0.000f, 0.000f, 1.456250f, 0.406250f, 0.506250f),
            part("SIT", -0.062500, 0.937500, 0.625000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel default_bench() {
        return model("default_bench", "Bench", FurnitureDefinition.Category.FURNITURE, "DARK_OAK_STAIRS", 200, 2.350000, 1.464126, List.of(
            part("FRAME", -1.062500, 0.531250, -0.543750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.150000f, 0.875000f),
            part("FRAME", -1.175000, 0.003891, 0.393750, 0.000f, 0.000f, 0.000f, 0.112500f, 0.962500f, 0.112500f),
            part("FRAME", -1.175000, 0.003891, -0.356250, 0.000f, 0.000f, 0.000f, 0.112500f, 0.962500f, 0.112500f),
            part("FRAME", -1.175000, 0.003891, -0.243750, 0.000f, 0.000f, 0.000f, 0.112500f, 0.150000f, 0.637500f),
            part("FRAME", -1.175000, 0.816391, -0.243750, 0.000f, 0.000f, 0.000f, 0.112500f, 0.150000f, 0.637500f),
            part("FRAME", -1.056250, 0.530517, 0.335407, 0.000f, 22.500f, 0.000f, 0.125000f, 0.937500f, 0.150000f),
            part("FRAME", 0.931250, 0.530517, 0.335407, 0.000f, 22.500f, 0.000f, 0.125000f, 0.937500f, 0.150000f),
            part("FRAME", 0.937500, 0.531250, -0.543750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.150000f, 0.875000f),
            part("FRAME", 1.062500, 0.003891, 0.393750, 0.000f, 0.000f, 0.000f, 0.112500f, 0.962500f, 0.112500f),
            part("FRAME", 1.062500, 0.003891, -0.243750, 0.000f, 0.000f, 0.000f, 0.112500f, 0.150000f, 0.637500f),
            part("FRAME", 1.062500, 0.816391, -0.243750, 0.000f, 0.000f, 0.000f, 0.112500f, 0.150000f, 0.637500f),
            part("FRAME", 1.062500, 0.003891, -0.356250, 0.000f, 0.000f, 0.000f, 0.112500f, 0.962500f, 0.112500f),
            part("FRAME", -0.937500, 0.531250, -0.543750, 0.000f, 0.000f, 0.000f, 1.875000f, 0.150000f, 0.125000f),
            part("FRAME", -0.937500, 0.531250, 0.143750, 0.000f, 0.000f, 0.000f, 1.875000f, 0.150000f, 0.125000f),
            part("FRAME", -0.931250, 1.343017, 0.335407, 0.000f, 22.500f, 0.000f, 1.875000f, 0.125000f, 0.150000f),
            part("COLOR", -1.037500, 0.681250, -0.518750, 0.000f, 0.000f, 0.000f, 2.081250f, 0.050000f, 0.162500f),
            part("COLOR", -1.037500, 0.681250, -0.300000, 0.000f, 0.000f, 0.000f, 2.081250f, 0.050000f, 0.162500f),
            part("COLOR", -1.037500, 0.681250, -0.056250, 0.000f, 0.000f, 0.000f, 2.081250f, 0.050000f, 0.162500f),
            part("COLOR", -1.037500, 0.681250, 0.212500, 0.000f, 22.500f, 0.000f, 2.081250f, 0.162500f, 0.050000f),
            part("COLOR", -1.037500, 0.887500, 0.281250, 0.000f, 22.500f, 0.000f, 2.081250f, 0.162500f, 0.050000f),
            part("COLOR", -1.037500, 1.068750, 0.350000, 0.000f, 22.500f, 0.000f, 2.081250f, 0.162500f, 0.050000f),
            part("COLOR", -1.037500, 1.275000, 0.418750, 0.000f, 22.500f, 0.000f, 2.081250f, 0.162500f, 0.050000f),
            part("SIT", -0.125000, 0.750000, 0.062500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel default_chandelier() {
        return model("default_chandelier", "Lamp", FurnitureDefinition.Category.FURNITURE, "LANTERN", 200, 1.437500, 2.328750, List.of(
            part("LIGHT", -0.133750, -0.328750, -0.078750, 22.500f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("CHAIN", -0.125000, 0.125000, -0.437500, 0.000f, 22.500f, 0.000f, 0.250000f, 0.500000f, 0.250000f),
            part("CHAIN", -0.125000, 0.031250, 0.218750, 0.000f, -22.500f, 0.000f, 0.250000f, 0.500000f, 0.250000f),
            part("CHAIN", 0.156250, 0.000000, -0.093750, 0.000f, 0.000f, 22.500f, 0.250000f, 0.500000f, 0.250000f),
            part("NETHERITE_BLOCK", -0.187500, 0.500000, -0.187500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.375000f),
            part("CHAIN", -0.337500, 0.062500, -0.125000, 0.000f, 0.000f, -22.500f, 0.250000f, 0.500000f, 0.250000f),
            part("NETHERITE_BLOCK", -0.312500, 0.031250, -0.375000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.312500, 0.031250, 0.312500, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.375000, 0.031250, -0.375000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.750000f),
            part("NETHERITE_BLOCK", 0.312500, 0.031250, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.625000f),
            part("CHAIN", -0.250000, 0.562500, -0.250000, 0.000f, 0.000f, 0.000f, 0.500000f, 1.437500f, 0.500000f),
            part("NETHERITE_BLOCK", -0.031250, 0.000000, 0.375000, 0.000f, 45.000f, 0.000f, 0.062500f, 0.187500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.093750, 0.187500, 0.375000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.187500f),
            part("NETHERITE_BLOCK", -0.031250, 0.044194, -0.419194, 0.000f, -45.000f, 0.000f, 0.062500f, 0.187500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.093750, 0.187500, -0.562500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.187500f),
            part("NETHERITE_BLOCK", 0.343750, 0.062500, -0.031250, 0.000f, 0.000f, -45.000f, 0.062500f, 0.187500f, 0.062500f),
            part("NETHERITE_BLOCK", 0.343750, 0.187500, -0.093750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.187500f),
            part("NETHERITE_BLOCK", -0.468750, 0.050000, -0.031250, 0.000f, 0.000f, 45.000f, 0.062500f, 0.187500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.562500, 0.187500, -0.093750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.187500f),
            part("CANDLE", -0.250000, 0.250000, -0.718750, 0.000f, 0.000f, 0.000f, 0.500000f, 0.750000f, 0.500000f),
            part("CANDLE", -0.718750, 0.250000, -0.250000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.750000f, 0.500000f),
            part("CANDLE", -0.250000, 0.250000, 0.218750, 0.000f, 0.000f, 0.000f, 0.500000f, 0.750000f, 0.500000f),
            part("CANDLE", 0.187500, 0.250000, -0.250000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.750000f, 0.500000f)
        ));
    }
    private static SmpFurnitureModel default_chimney() {
        return model("default_chimney", "Kitchen Chimney", FurnitureDefinition.Category.FURNITURE, "ANVIL", 200, 1.037500, 1.131250, List.of(
            part("BLOCK", -1.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.312500f, 1.000000f),
            part("BUTTONS", -0.537500, 0.056250, -1.037500, 0.000f, 0.000f, 0.000f, 0.181250f, 0.075000f, 0.037500f),
            part("BUTTONS", -0.287500, 0.056250, -1.037500, 0.000f, 0.000f, 0.000f, 0.168750f, 0.181250f, 0.037500f),
            part("BLOCK", -1.000000, 0.875000, -0.812500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.250000f, 0.812500f),
            part("BLOCK", -0.812500, 0.312500, -0.625000, 0.000f, 0.000f, 0.000f, 0.631250f, 0.687500f, 0.437500f),
            part("BUTTONS", -0.925000, 0.056250, -1.037500, 0.000f, 0.000f, 0.000f, 0.300000f, 0.075000f, 0.037500f),
            part("INTAKE", -0.937500, -0.006250, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.010000f, 0.875000f)
        ));
    }
    private static SmpFurnitureModel default_decorative_plant() {
        return model("default_decorative_plant", "Small Decorative Plant", FurnitureDefinition.Category.FURNITURE, "OAK_SAPLING", 200, 0.483915, 1.106250, List.of(
            part("POT", -0.187500, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.443750f, 0.375000f),
            part("SAP", -0.025000, 0.437500, -0.050000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.631250f, 0.062500f),
            part("YAPRAK", -0.068750, 0.881250, -0.125000, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", -0.043750, 0.842417, -0.257583, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.137500, 0.710526, -0.119162, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.162500, 0.804276, -0.119162, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", -0.162500, 0.560526, -0.119162, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", -0.137500, 0.466776, -0.119162, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.100000, 0.591776, -0.150412, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.125000, 0.685526, -0.150412, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", -0.100000, 0.629276, 0.119162, 0.000f, -45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.125000, 0.723026, 0.119162, 0.000f, -45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", -0.162500, 0.598026, 0.087912, 0.000f, -45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", -0.137500, 0.504276, 0.087912, 0.000f, -45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.162500, 0.841776, 0.087912, 0.000f, -45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", -0.137500, 0.748026, 0.087912, 0.000f, -45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.043750, 0.879917, 0.226333, 0.000f, -45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.068750, 0.918750, 0.093750, 0.000f, -45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", 0.037500, 0.623026, -0.119162, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", 0.075000, 0.498026, -0.087912, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", 0.100000, 0.404276, -0.087912, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", 0.075000, 0.741776, -0.087912, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", 0.100000, 0.648026, -0.087912, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", 0.006250, 0.779917, -0.226333, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("YAPRAK", -0.018750, 0.818750, -0.093750, 0.000f, 45.000f, 0.000f, 0.156250f, 0.187500f, 0.010000f),
            part("YAPRAK", 0.062500, 0.529276, -0.119162, 0.000f, 45.000f, 0.000f, 0.106250f, 0.093750f, 0.010000f),
            part("TOPRAK", -0.156250, 0.437500, -0.156250, 0.000f, 0.000f, 0.000f, 0.312500f, 0.018750f, 0.312500f)
        ));
    }
    private static SmpFurnitureModel default_door_frame() {
        return model("default_door_frame", "Door Frame", FurnitureDefinition.Category.FURNITURE, "OAK_FENCE", 200, 2.375000, 3.500000, List.of(
            part("WOOD", -0.187500, -0.125000, -0.375000, 0.000f, 0.000f, 90.000f, 0.375000f, 2.625000f, 0.375000f),
            part("WOOD", -1.187500, -1.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.250000f, 2.000000f, 0.250000f),
            part("WOOD", 0.937500, -1.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.250000f, 2.000000f, 0.250000f)
        ));
    }
    private static SmpFurnitureModel default_enchants() {
        return model("default_enchants", "Custom Enchantments Display", FurnitureDefinition.Category.FURNITURE, "ENCHANTING_TABLE", 200, 3.000000, 3.187500, List.of(
            part("BASE", -1.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", 0.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", 0.000000, 0.000000, -2.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", -1.000000, 0.000000, -2.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", -2.000000, 0.000000, -2.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", -2.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", 0.000000, 0.000000, 0.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", -1.000000, 0.000000, 0.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE", -2.000000, 0.000000, 0.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.375000f, 1.000000f),
            part("BASE_BIG", -1.000000, 0.375000, -0.812500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.500000f, 0.687500f),
            part("ANVIL", -1.250000, 0.875000, -1.250000, 0.000f, 0.000f, 0.000f, 1.500000f, 1.362500f, 2.125000f),
            part("BLOCKS", -1.562500, 0.250000, -1.687500, -15.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.375000f),
            part("BLOCKS", -1.187500, 0.375000, -1.875000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.500000f),
            part("BLOCKS", 0.265246, 0.375000, -1.642385, 45.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.375000f),
            part("BLOCKS", -0.250000, 0.375000, -1.812500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.375000f),
            part("ROCKS", -1.562500, 0.187500, 0.250000, 0.000f, 0.000f, 27.500f, 0.750000f, 0.250000f, 0.625000f),
            part("ROCKS", -1.521119, 0.447571, -1.138869, -27.500f, 0.000f, 27.500f, 0.750000f, 0.187500f, 0.625000f),
            part("ROCKS", -0.125000, 0.562500, -0.562500, 0.000f, 0.000f, -27.500f, 0.750000f, 0.250000f, 0.625000f),
            part("BASE_BIG", -1.000000, 0.375000, -0.125000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.500000f, 0.687500f),
            part("ROCKS", -1.708619, 0.385071, -0.951369, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.375000f),
            part("ROCKS", -1.396119, 0.385071, 0.048631, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.375000f),
            part("CARPET", -1.000000, 2.250000, -0.250000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.010000f, 0.625000f),
            part("CARPET", -0.500000, 2.250000, -0.250000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.010000f, 0.625000f),
            part("CARPET", -1.312738, 2.073405, -0.250000, 0.000f, 0.000f, 90.000f, 0.500000f, 0.010000f, 0.625000f),
            part("CARPET", -0.062500, 2.187500, -0.250000, 0.000f, 0.000f, -90.000f, 0.500000f, 0.010000f, 0.625000f),
            part("BLOCKS", 0.000000, 0.750000, -1.750000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.375000f),
            part("SWORD", -1.731013, 0.260071, -0.402483, 90.000f, 0.000f, -180.000f, 0.625000f, 0.625000f, 0.187500f),
            part("CARPET", -1.312738, 1.573405, -0.250000, 0.000f, 0.000f, 90.000f, 0.500000f, 0.010000f, 0.625000f),
            part("CARPET", -0.062500, 1.687500, -0.250000, 0.000f, 0.000f, -90.000f, 0.500000f, 0.010000f, 0.625000f),
            part("GOLD_BLOCK", -0.750000, 2.562500, -0.812500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.125000f, 0.500000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.625000, 2.687500, -0.687500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("LODESTONE", -0.687500, 2.750000, -0.750000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.250000f, 0.375000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.625000, 3.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("GOLD_BLOCK", -0.750000, 3.062500, -0.812500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.125000f, 0.500000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.625000, 2.781250, -0.375000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.187500f, 0.125000f),
            part("STRIPPED_OAK_LOG", -0.562500, 2.500000, 0.062500, 0.000f, 90.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("CHISELED_POLISHED_BLACKSTONE", -0.625000, 2.781250, 0.437500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.187500f, 0.125000f),
            part("YELLOW_CONCRETE", -0.562500, 2.437500, -1.187500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.125000f, 0.125000f),
            part("ORANGE_CONCRETE", -0.562500, 2.687500, -1.000000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.062500f, 0.062500f),
            part("RED_CONCRETE", -0.562500, 2.875000, -1.062500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.125000f, 0.125000f),
            part("RED_CONCRETE", -0.562500, 2.375000, -0.875000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.062500f, 0.062500f),
            part("YELLOW_CONCRETE", -0.562500, 2.312500, -0.687500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.125000f, 0.125000f),
            part("ORANGE_CONCRETE", -0.562500, 2.687500, -1.312500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.125000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel default_fridge() {
        return model("default_fridge", "Kitchen Fridge", FurnitureDefinition.Category.FURNITURE, "IRON_DOOR", 200, 1.056250, 2.812500, List.of(
            part("FRAME", -0.062500, 0.000000, -1.031250, 0.000f, 0.000f, 0.000f, 0.062500f, 2.812500f, 0.031250f),
            part("FRAME", -1.000000, 0.000000, -1.031250, 0.000f, 0.000f, 0.000f, 0.062500f, 2.812500f, 0.031250f),
            part("FRAME", -0.937500, 0.000000, -1.031250, 0.000f, 0.000f, 0.000f, 0.881250f, 0.062500f, 0.031250f),
            part("FRAME", -0.943750, 0.968750, -1.031250, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("HANDLE", -0.881250, 0.368750, -1.037500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.275000f, 0.037500f),
            part("HANDLE", -0.881250, 1.381250, -1.037500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.618750f, 0.037500f),
            part("FRAME", -0.943750, 2.718750, -1.031250, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("HANDLE", -0.506250, 1.693750, -1.037500, 0.000f, 0.000f, 0.000f, 0.387500f, 0.431250f, 0.037500f),
            part("STATS", -0.462500, 1.712500, -1.056250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 0.018750f),
            part("STATS", -0.218750, 1.712500, -1.056250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.281250f, 0.018750f),
            part("STATS", -0.356250, 1.712500, -1.056250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 0.018750f),
            part("BACKGROUND", -0.406250, 1.718750, -1.050000, 0.000f, 0.000f, 0.000f, 0.212500f, 0.137500f, 0.012500f),
            part("BLOCK", -1.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 2.812500f, 1.000000f)
        ));
    }
    private static SmpFurnitureModel default_glass_table() {
        return model("default_glass_table", "Glass Table", FurnitureDefinition.Category.FURNITURE, "CAULDRON", 200, 1.875000, 0.687500, List.of(
            part("FRAME", -1.437500, 0.562500, -0.031250, 0.000f, 0.000f, 0.000f, 1.875000f, 0.125000f, 0.093750f),
            part("GLASS", -1.381250, 0.668750, -1.062500, 0.000f, 0.000f, 0.000f, 1.756250f, 0.010000f, 1.125000f),
            part("FRAME", -1.437500, 0.562500, -1.062500, 0.000f, 0.000f, 0.000f, 1.875000f, 0.125000f, 0.093750f),
            part("FRAME", 0.350000, 0.562500, -0.968750, 0.000f, 0.000f, 0.000f, 0.087500f, 0.125000f, 0.943750f),
            part("FRAME", -1.437500, 0.168750, -1.062500, 0.000f, 0.000f, 0.000f, 1.875000f, 0.125000f, 1.125000f),
            part("FRAME", -1.437500, 0.562500, -0.968750, 0.000f, 0.000f, 0.000f, 0.087500f, 0.125000f, 0.943750f),
            part("LEGS", 0.112500, 0.000000, -0.956250, 0.000f, 0.000f, 0.000f, 0.181250f, 0.606250f, 0.162500f),
            part("LEGS", -1.325000, 0.000000, -0.956250, 0.000f, 0.000f, 0.000f, 0.181250f, 0.606250f, 0.162500f),
            part("LEGS", -1.325000, 0.000000, -0.206250, 0.000f, 0.000f, 0.000f, 0.181250f, 0.606250f, 0.162500f),
            part("LEGS", 0.112500, 0.000000, -0.206250, 0.000f, 0.000f, 0.000f, 0.181250f, 0.606250f, 0.162500f)
        ));
    }
    private static SmpFurnitureModel default_kitchen_cabinet() {
        return model("default_kitchen_cabinet", "Small Kitchen Cabinet", FurnitureDefinition.Category.FURNITURE, "WHITE_SHULKER_BOX", 200, 1.000000, 1.000000, List.of(
            part("FRAME", -0.062500, 0.000000, -0.781250, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.031250f),
            part("FRAME", -1.000000, 0.000000, -0.781250, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.031250f),
            part("FRAME", -0.937500, 0.937500, -0.781250, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.031250f),
            part("FRAME", -0.937500, 0.000000, -0.781250, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.031250f),
            part("FRAME", -0.937500, 0.712500, -0.781250, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.031250f),
            part("HANDLE", -0.631250, 0.368750, -0.787500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.243750f, 0.037500f),
            part("HANDLE", -0.443750, 0.368750, -0.787500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.243750f, 0.037500f),
            part("BLOCK", -1.000000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 1.000000f, 1.000000f, 0.750000f)
        ));
    }
    private static SmpFurnitureModel default_kitchen_cabinet_1() {
        return model("default_kitchen_cabinet_1", "Small Open Kitchen Cabinet", FurnitureDefinition.Category.FURNITURE, "GRAY_SHULKER_BOX", 200, 1.018750, 1.181250, List.of(
            part("BLOCK", -1.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 1.000000f, 1.000000f),
            part("FRAME", -0.131250, 0.050000, -1.012500, 0.000f, 0.000f, 0.000f, 0.068750f, 0.793750f, 0.031250f),
            part("FRAME", -0.943750, 0.050000, -1.012500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.793750f, 0.031250f),
            part("FRAME", -0.881250, 0.050000, -1.012500, 0.000f, 0.000f, 0.000f, 0.750000f, 0.062500f, 0.031250f),
            part("FRAME", -0.943750, 0.843750, -1.012500, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("HANDLE", -0.912500, 0.443750, -1.018750, 0.000f, 0.000f, 0.000f, 0.075000f, 0.275000f, 0.037500f),
            part("BLOCK", -0.125000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 1.000000f, 1.000000f),
            part("BLOCK", -0.875000, 0.000000, -0.125000, 0.000f, 0.000f, 0.000f, 0.750000f, 1.000000f, 0.125000f),
            part("BLOCK", -0.875000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.875000f),
            part("BLOCK", -0.875000, 0.875000, -1.000000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.875000f),
            part("BLOCK", -0.875000, 0.393750, -0.975000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.081250f, 0.850000f),
            part("GLASS", -0.893750, 0.106250, -0.987500, 0.000f, 0.000f, 0.000f, 0.775000f, 0.743750f, 0.010000f),
            part("FOOD1", -0.593750, 0.268750, -0.893750, 0.000f, 0.000f, 0.000f, 0.325000f, 0.337500f, 0.475000f),
            part("FOOD2", -0.743750, 0.656250, -0.893750, 0.000f, 0.000f, 0.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD2", -0.493750, 0.362500, -0.706250, 0.000f, 0.000f, -50.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD2", -0.818750, 0.656250, -0.893750, -180.000f, 0.000f, 0.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD1", -0.718750, 0.268750, -0.893750, -180.000f, 0.000f, 0.000f, 0.325000f, 0.337500f, 0.475000f)
        ));
    }
    private static SmpFurnitureModel default_kitchen_cabinet_2() {
        return model("default_kitchen_cabinet_2", "Large Open Kitchen Cabinet", FurnitureDefinition.Category.FURNITURE, "LIGHT_GRAY_SHULKER_BOX", 200, 1.018750, 2.181250, List.of(
            part("BLOCK", -1.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 2.000000f, 1.000000f),
            part("FRAME", -0.131250, 0.050000, -1.012500, 0.000f, 0.000f, 0.000f, 0.068750f, 1.806250f, 0.031250f),
            part("FRAME", -0.943750, 0.050000, -1.012500, 0.000f, 0.000f, 0.000f, 0.062500f, 1.806250f, 0.031250f),
            part("FRAME", -0.881250, 0.050000, -1.012500, 0.000f, 0.000f, 0.000f, 0.756250f, 0.062500f, 0.031250f),
            part("FRAME", -0.943750, 1.843750, -1.012500, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("HANDLE", -0.912500, 0.943750, -1.018750, 0.000f, 0.000f, 0.000f, 0.075000f, 0.275000f, 0.037500f),
            part("BLOCK", -0.125000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 2.000000f, 1.000000f),
            part("BLOCK", -0.875000, 0.000000, -0.125000, 0.000f, 0.000f, 0.000f, 0.750000f, 2.000000f, 0.125000f),
            part("BLOCK", -0.875000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.875000f),
            part("BLOCK", -0.875000, 1.875000, -1.000000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.875000f),
            part("BLOCK", -0.875000, 1.312500, -0.975000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.081250f, 0.850000f),
            part("BLOCK", -0.875000, 0.625000, -0.975000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.081250f, 0.850000f),
            part("GLASS", -0.893750, 0.106250, -0.987500, 0.000f, 0.000f, 0.000f, 0.775000f, 1.743750f, 0.010000f),
            part("FOOD1", -0.531250, 0.956250, -0.893750, 0.000f, 0.000f, 0.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD2", -0.718750, 0.400000, -0.893750, 0.000f, 0.000f, 0.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD3", -0.743750, 1.656250, -0.893750, 0.000f, 0.000f, 0.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD3", -0.493750, 1.362500, -0.706250, 0.000f, 0.000f, -50.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD2", -0.831250, 0.400000, -0.893750, -180.000f, 0.000f, 0.000f, 0.512500f, 0.525000f, 0.475000f),
            part("FOOD3", -0.818750, 1.656250, -0.893750, -180.000f, 0.000f, 0.000f, 0.512500f, 0.525000f, 0.475000f)
        ));
    }
    private static SmpFurnitureModel default_kitchen_chair() {
        return model("default_kitchen_chair", "Kitchen Chair", FurnitureDefinition.Category.FURNITURE, "BIRCH_STAIRS", 200, 1.000000, 0.808609, List.of(
            part("TOP", -1.000000, 0.568750, -0.875000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.162500f, 0.750000f),
            part("LEGS", -0.937500, 0.500000, -0.812500, 0.000f, 0.000f, 0.000f, 0.881250f, 0.168750f, 0.625000f),
            part("LEGS", -0.191023, -0.058609, -0.350000, 0.000f, 0.000f, 10.000f, 0.125000f, 0.525000f, 0.150000f),
            part("LEGS", -0.191023, -0.058609, -0.793750, 0.000f, 0.000f, 10.000f, 0.125000f, 0.525000f, 0.150000f),
            part("LEGS", -0.933977, -0.058609, -0.793750, 0.000f, 0.000f, -10.000f, 0.125000f, 0.525000f, 0.150000f),
            part("LEGS", -0.933977, -0.058609, -0.350000, 0.000f, 0.000f, -10.000f, 0.125000f, 0.525000f, 0.150000f),
            part("SIT", -0.500000, 0.750000, -0.625000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel default_kitchen_sink() {
        return model("default_kitchen_sink", "Kitchen Sink", FurnitureDefinition.Category.FURNITURE, "CAULDRON", 200, 1.000000, 1.331250, List.of(
            part("BLOCK", -1.000000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.750000f, 0.750000f),
            part("FRAME", -0.062500, 0.000000, -0.781250, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.031250f),
            part("FRAME", -1.000000, 0.000000, -0.781250, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.031250f),
            part("FRAME", -0.937500, 0.937500, -0.781250, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.031250f),
            part("FRAME", -0.937500, 0.000000, -0.781250, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.031250f),
            part("FRAME", -0.937500, 0.712500, -0.781250, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.031250f),
            part("HANDLE", -0.631250, 0.368750, -0.787500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.243750f, 0.037500f),
            part("HANDLE", -0.443750, 0.368750, -0.787500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.243750f, 0.037500f),
            part("BLOCK", -0.187500, 0.750000, -0.750000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.250000f, 0.750000f),
            part("BLOCK", -1.000000, 0.750000, -0.750000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.250000f, 0.750000f),
            part("BLOCK", -0.812500, 0.750000, -0.187500, 0.000f, 0.000f, 0.000f, 0.631250f, 0.250000f, 0.187500f),
            part("BLOCK", -0.812500, 0.750000, -0.750000, 0.000f, 0.000f, 0.000f, 0.631250f, 0.250000f, 0.143750f),
            part("BLOCK", -0.562500, 1.000000, -0.125000, 0.000f, 0.000f, 0.000f, 0.106250f, 0.268750f, 0.081250f),
            part("BLOCK", -0.562500, 1.143750, -0.312500, 0.000f, 0.000f, 0.000f, 0.106250f, 0.125000f, 0.081250f),
            part("BLOCK", -0.562500, 1.250000, -0.312500, 0.000f, 0.000f, 0.000f, 0.106250f, 0.081250f, 0.268750f),
            part("BLOCK", -0.693750, 1.000000, -0.106250, 0.000f, 0.000f, 0.000f, 0.056250f, 0.081250f, 0.037500f),
            part("RED", -0.712500, 1.056250, -0.118750, 0.000f, 0.000f, 0.000f, 0.100000f, 0.068750f, 0.075000f),
            part("BLOCK", -0.381250, 1.000000, -0.106250, 0.000f, 0.000f, 0.000f, 0.056250f, 0.081250f, 0.037500f),
            part("BLUE", -0.400000, 1.056250, -0.118750, 0.000f, 0.000f, 0.000f, 0.100000f, 0.068750f, 0.075000f),
            part("WATER", -0.812500, 0.750000, -0.625000, 0.000f, 0.000f, 0.000f, 0.631250f, 0.187500f, 0.437500f)
        ));
    }
    private static SmpFurnitureModel default_kitchen_table() {
        return model("default_kitchen_table", "Kitchen Table", FurnitureDefinition.Category.FURNITURE, "SCAFFOLDING", 200, 1.875000, 1.000000, List.of(
            part("TOP", -0.937500, 0.812500, -0.687500, 0.000f, 0.000f, 0.000f, 1.875000f, 0.187500f, 1.375000f),
            part("LEGS", -0.250000, 0.000000, -0.437500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.093750f, 0.875000f),
            part("LEGS", -0.000000, 0.081250, -0.431250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.750000f, 0.856250f),
            part("LEGS", -0.125000, 0.081250, -0.425000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.750000f, 0.843750f)
        ));
    }
    private static SmpFurnitureModel default_lamp() {
        return model("default_lamp", "Lamp", FurnitureDefinition.Category.FURNITURE, "LANTERN", 200, 1.767500, 0.626875, List.of(
            part("FRAME", -0.087500, 0.683750, -0.087500, 0.000f, 0.000f, 0.000f, 0.175000f, 0.253750f, 0.175000f),
            part("FRAME", -0.187500, 0.937500, -0.187500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.375000f),
            part("FRAME", -0.700000, 0.771250, -0.043750, 0.000f, 0.000f, 0.000f, 1.400000f, 0.078750f, 0.087500f),
            part("FRAME", 0.621250, 0.640000, -0.043750, 0.000f, 0.000f, 0.000f, 0.078750f, 0.131250f, 0.087500f),
            part("FRAME", 0.603750, 0.456250, -0.087500, -22.500f, 0.000f, 0.000f, 0.113750f, 0.192500f, 0.175000f),
            part("LAMP", 0.691250, 0.421250, -0.078750, -22.500f, 0.000f, 0.000f, 0.192500f, 0.262500f, 0.236250f),
            part("FRAME", -0.717500, 0.456250, -0.087500, 22.500f, 0.000f, 0.000f, 0.113750f, 0.192500f, 0.175000f),
            part("LAMP", -0.883750, 0.421250, -0.078750, 22.500f, 0.000f, 0.000f, 0.192500f, 0.262500f, 0.236250f),
            part("FRAME", -0.700000, 0.640000, -0.043750, 0.000f, 0.000f, 0.000f, 0.078750f, 0.131250f, 0.087500f),
            part("FRAME", 0.183750, 0.640000, -0.043750, 0.000f, 0.000f, 0.000f, 0.078750f, 0.131250f, 0.087500f),
            part("FRAME", 0.153125, 0.548125, -0.087500, 0.000f, 0.000f, 22.500f, 0.192500f, 0.113750f, 0.175000f),
            part("LAMP", 0.153125, 0.373125, -0.105000, 0.000f, 0.000f, 22.500f, 0.262500f, 0.192500f, 0.236250f),
            part("LAMP", -0.415625, 0.373125, -0.105000, 0.000f, 0.000f, -22.500f, 0.262500f, 0.192500f, 0.236250f),
            part("FRAME", -0.345625, 0.548125, -0.087500, 0.000f, 0.000f, -22.500f, 0.192500f, 0.113750f, 0.175000f),
            part("FRAME", -0.262500, 0.640000, -0.043750, 0.000f, 0.000f, 0.000f, 0.078750f, 0.131250f, 0.087500f),
            part("LIGHT", -0.062500, 0.375000, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel default_laptop() {
        return model("default_laptop", "Laptop", FurnitureDefinition.Category.FURNITURE, "POLISHED_BLACKSTONE_PRESSURE_PLATE", 200, 0.875000, 0.662500, List.of(
            part("GOVDE", -0.437500, 0.002500, -0.437500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.056250f, 0.662500f),
            part("GOVDE", -0.437500, -0.011952, 0.297655, 0.000f, 22.500f, 0.000f, 0.875000f, 0.662500f, 0.056250f),
            part("TUS", 0.300000, 0.056250, 0.012500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.300000, 0.056250, -0.112500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.300000, 0.056250, -0.231250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.300000, 0.056250, -0.356250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.175000, 0.056250, -0.356250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.162500, 0.056250, -0.231250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.162500, 0.056250, 0.012500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.162500, 0.056250, -0.112500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.137500, 0.056250, -0.356250, 0.000f, 0.000f, 0.000f, 0.262500f, 0.018750f, 0.081250f),
            part("TUS", 0.025000, 0.056250, -0.231250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.025000, 0.056250, 0.012500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", 0.025000, 0.056250, -0.112500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.375000, 0.056250, -0.356250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.375000, 0.056250, -0.231250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.375000, 0.056250, 0.012500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.375000, 0.056250, -0.112500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.250000, 0.056250, -0.356250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.250000, 0.056250, -0.231250, 0.000f, 0.000f, 0.000f, 0.106250f, 0.018750f, 0.056250f),
            part("TUS", -0.250000, 0.056250, -0.087500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.018750f, 0.056250f),
            part("TUS", -0.250000, 0.056250, 0.012500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.100000, 0.056250, -0.231250, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.100000, 0.056250, -0.112500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.100000, 0.056250, 0.012500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.018750f, 0.081250f),
            part("TUS", -0.250000, 0.056250, -0.143750, 0.000f, 0.000f, 0.000f, 0.106250f, 0.018750f, 0.056250f)
        ));
    }
    private static SmpFurnitureModel default_pizza_box() {
        return model("default_pizza_box", "Pizza Box", FurnitureDefinition.Category.FURNITURE, "SMOOTH_STONE_SLAB", 200, 0.875000, 0.268750, List.of(
            part("BOX", -0.437500, 0.000000, -0.437500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.875000f),
            part("PIZZA", -0.037500, 0.118750, -0.381250, 45.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.537500f),
            part("PIZZA", -0.125888, 0.118750, -0.292862, -45.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.412500f),
            part("PIZZA", -0.355070, 0.118750, -0.027068, 0.000f, 0.000f, 0.000f, 0.762500f, 0.125000f, 0.087500f),
            part("PIZZA", -0.148820, 0.118750, -0.227068, 0.000f, 0.000f, 0.000f, 0.343750f, 0.125000f, 0.200000f),
            part("RED", -0.192570, 0.181250, -0.095818, 0.000f, 0.000f, 0.000f, 0.081250f, 0.087500f, 0.081250f),
            part("RED", 0.038680, 0.181250, -0.214568, 0.000f, 0.000f, 0.000f, 0.081250f, 0.087500f, 0.081250f),
            part("RED", 0.207430, 0.181250, -0.077068, 22.500f, 0.000f, 0.000f, 0.081250f, 0.087500f, 0.081250f),
            part("BLACK", 0.157430, 0.187500, 0.297932, 0.000f, 0.000f, 0.000f, 0.237500f, 0.018750f, 0.081250f),
            part("BLACK", -0.317570, 0.187500, 0.297932, 0.000f, 0.000f, 0.000f, 0.237500f, 0.010000f, 0.081250f),
            part("BLACK", 0.013680, 0.187500, 0.297932, 0.000f, 0.000f, 0.000f, 0.093750f, 0.018750f, 0.081250f),
            part("BLACK", 0.051180, 0.187500, 0.172932, 0.000f, 0.000f, 0.000f, 0.343750f, 0.018750f, 0.081250f)
        ));
    }
    private static SmpFurnitureModel default_sofa() {
        return model("default_sofa", "Small Sofa", FurnitureDefinition.Category.FURNITURE, "BLUE_WOOL", 200, 1.693750, 1.408609, List.of(
            part("CUSHION", -0.562500, 0.356250, -0.500000, 0.000f, 0.000f, 0.000f, 1.250000f, 0.337500f, 1.000000f),
            part("WOOD", 0.446477, 0.003891, -0.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("CUSHION", -0.625000, 0.693750, 0.187500, 0.000f, 0.000f, 0.000f, 1.312500f, 0.718750f, 0.312500f),
            part("CUSHION", 0.437500, 0.468750, -0.625000, 0.000f, 0.000f, 22.500f, 0.437500f, 0.443750f, 1.118750f),
            part("CUSHION", -0.818750, 0.468750, -0.625000, 0.000f, 0.000f, -22.500f, 0.437500f, 0.443750f, 1.118750f),
            part("WOOD", 0.446477, 0.003891, 0.206250, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -0.621477, 0.003891, 0.206250, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -0.621477, 0.003891, -0.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("SIT", 0.062500, 0.687500, 0.000000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel default_sofa_large() {
        return model("default_sofa_large", "Large Sofa", FurnitureDefinition.Category.FURNITURE, "RED_WOOL", 200, 2.693750, 1.408609, List.of(
            part("CUSHION", 0.437500, 0.356250, -0.500000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.337500f, 1.000000f),
            part("WOOD", 1.008977, 0.003891, -0.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("CUSHION", 0.000000, 0.693750, 0.187500, 0.000f, 0.000f, 0.000f, 1.187500f, 0.718750f, 0.312500f),
            part("CUSHION", 0.937500, 0.468750, -0.625000, 0.000f, 0.000f, 22.500f, 0.437500f, 0.443750f, 1.118750f),
            part("CUSHION", -1.318750, 0.468750, -0.625000, 0.000f, 0.000f, -22.500f, 0.437500f, 0.443750f, 1.118750f),
            part("CUSHION", -0.437500, 0.356250, -0.500000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.337500f, 1.000000f),
            part("CUSHION", -1.187500, 0.693750, 0.187500, 0.000f, 0.000f, 0.000f, 1.187500f, 0.718750f, 0.312500f),
            part("WOOD", 1.008977, 0.003891, 0.206250, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -1.183977, 0.003891, 0.206250, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("WOOD", -1.183977, 0.003891, -0.418750, 0.000f, 0.000f, 0.000f, 0.175000f, 0.462500f, 0.175000f),
            part("CUSHION", -1.312500, 0.356250, -0.500000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.337500f, 1.000000f),
            part("SIT", 0.125000, 0.687500, 0.000000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel default_stove() {
        return model("default_stove", "Stove", FurnitureDefinition.Category.FURNITURE, "FURNACE", 200, 1.103125, 1.187500, List.of(
            part("SIDE", -1.000000, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.062500f, 0.875000f),
            part("SIDE", -1.015625, 1.015625, -0.937500, 0.000f, 0.000f, 90.000f, 0.937500f, 0.062500f, 0.875000f),
            part("SIDE", -1.000000, 0.990625, -0.896875, 0.000f, 90.000f, 0.000f, 1.000000f, 0.062500f, 1.000000f),
            part("SIDE", -1.000000, 0.115625, -0.959375, 0.000f, 90.000f, 0.000f, 1.000000f, 0.062500f, 0.312500f),
            part("SIDE", -1.015625, 0.078125, -0.937500, 0.000f, 0.000f, 90.000f, 0.937500f, 0.062500f, 0.875000f),
            part("SIYAHDESEN", -1.000000, 0.115625, -0.646875, 0.000f, 90.000f, 0.000f, 1.000000f, 0.062500f, 0.062500f),
            part("SIYAHDESEN", -1.000000, 0.115625, -0.021875, 0.000f, 90.000f, 0.000f, 1.000000f, 0.062500f, 0.062500f),
            part("SIYAHDESEN", -1.000000, 0.115625, -0.584375, 0.000f, 90.000f, 0.000f, 0.062500f, 0.062500f, 0.562500f),
            part("SIYAHDESEN", -0.062500, 0.115625, -0.584375, 0.000f, 90.000f, 0.000f, 0.062500f, 0.062500f, 0.562500f),
            part("BUTTON", -0.187500, 0.078125, -0.896875, 0.000f, 90.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("BUTTON", -0.375000, 0.078125, -0.896875, 0.000f, 90.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("BUTTON", -0.562500, 0.078125, -0.896875, 0.000f, 90.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("BUTTON", -0.750000, 0.078125, -0.896875, 0.000f, 90.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("BUTTON", -0.937500, 0.078125, -0.896875, 0.000f, 90.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("TOP", -1.000000, 1.000000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.062500f, 1.000000f),
            part("TOP2", -0.475000, 1.000000, -0.437500, 90.000f, 0.000f, 0.000f, 0.437500f, 0.125000f, 0.312500f),
            part("TOP2", -0.475000, 1.000000, -0.875000, 90.000f, 0.000f, 0.000f, 0.437500f, 0.125000f, 0.312500f),
            part("TOP2", -0.850000, 1.000000, -0.875000, 90.000f, 0.000f, 0.000f, 0.437500f, 0.125000f, 0.312500f),
            part("TOP2", -0.850000, 1.000000, -0.437500, 90.000f, 0.000f, 0.000f, 0.437500f, 0.125000f, 0.312500f),
            part("TOP", -1.000000, 1.062500, -0.125000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 0.125000f),
            part("BLACK", -0.937500, 0.787500, -0.918750, 0.000f, -180.000f, 0.000f, 0.875000f, 0.031250f, 0.812500f),
            part("ONKAPI", -0.193750, 0.103125, -0.484375, 0.000f, 90.000f, 0.000f, 0.131250f, 0.075000f, 0.500000f),
            part("ONKAPI", -0.800000, 0.103125, -0.128125, 0.000f, 90.000f, 0.000f, 0.606250f, 0.075000f, 0.143750f),
            part("ONKAPI", -0.962500, 0.103125, -0.621875, 0.000f, 90.000f, 0.000f, 0.937500f, 0.075000f, 0.143750f),
            part("CAM", -0.818750, 0.134375, -0.484375, 0.000f, 90.000f, 0.000f, 0.643750f, 0.010000f, 0.393750f),
            part("BLACK", -0.937500, 1.068750, -0.918750, 0.000f, -180.000f, 0.000f, 0.875000f, 0.062500f, 0.812500f),
            part("ONKAPI", -0.931250, 0.103125, -0.484375, 0.000f, 90.000f, 0.000f, 0.131250f, 0.075000f, 0.500000f),
            part("YEMEK", -0.300000, 0.790625, -0.709375, 0.000f, -90.000f, 0.000f, 0.312500f, 0.312500f, 0.462500f),
            part("YEMEK", -0.550000, 0.790625, -0.709375, 0.000f, -90.000f, 0.000f, 0.312500f, 0.312500f, 0.462500f),
            part("YEMEK", -0.800000, 0.790625, -0.709375, 0.000f, -90.000f, 0.000f, 0.312500f, 0.312500f, 0.462500f)
        ));
    }
    private static SmpFurnitureModel default_tv() {
        return model("default_tv", "TV", FurnitureDefinition.Category.FURNITURE, "PAINTING", 200, 1.750000, 1.125000, List.of(
            part("AYAK", 0.375000, 0.000000, -0.043750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.081250f),
            part("AYAK", -0.500000, 0.000000, -0.043750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.081250f),
            part("GOVDE", -0.875000, 0.187500, -0.062500, 0.000f, 0.000f, 0.000f, 1.750000f, 0.937500f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel default_wardrobe() {
        return model("default_wardrobe", "Large Wardrobe", FurnitureDefinition.Category.FURNITURE, "BROWN_SHULKER_BOX", 200, 2.000000, 2.943750, List.of(
            part("MAIN", -0.500000, 0.125000, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 2.812500f, 0.968750f),
            part("FRAME", 0.437500, 0.125000, -0.968750, 0.000f, 0.000f, 0.000f, 0.062500f, 2.812500f, 0.031250f),
            part("FRAME", -0.500000, 0.125000, -0.968750, 0.000f, 0.000f, 0.000f, 0.062500f, 2.812500f, 0.031250f),
            part("FRAME", -0.437500, 0.125000, -0.968750, 0.000f, 0.000f, 0.000f, 0.881250f, 0.062500f, 0.031250f),
            part("FRAME", -0.443750, 1.031250, -0.968750, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("ACCENT", -0.443750, 0.493750, -0.975000, 0.000f, 0.000f, 0.000f, 0.137500f, 0.337500f, 0.037500f),
            part("ACCENT", -0.456250, 1.556250, -0.975000, 0.000f, 0.000f, 0.000f, 0.150000f, 0.631250f, 0.037500f),
            part("FRAME", -0.443750, 2.843750, -0.968750, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("ACCENT", -0.693750, 0.493750, -0.975000, 0.000f, 0.000f, 0.000f, 0.137500f, 0.337500f, 0.037500f),
            part("MAIN", -1.500000, 0.125000, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 2.812500f, 0.968750f),
            part("FRAME", -1.500000, 0.125000, -0.968750, 0.000f, 0.000f, 0.000f, 0.062500f, 2.812500f, 0.031250f),
            part("FRAME", -0.562500, 0.125000, -0.968750, 0.000f, 0.000f, 0.000f, 0.062500f, 2.812500f, 0.031250f),
            part("FRAME", -1.443750, 0.125000, -0.968750, 0.000f, 0.000f, 0.000f, 0.881250f, 0.062500f, 0.031250f),
            part("FRAME", -1.437500, 1.031250, -0.968750, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("FRAME", -1.437500, 2.843750, -0.968750, 0.000f, 0.000f, 0.000f, 0.881250f, 0.093750f, 0.031250f),
            part("ACCENT", -0.693750, 1.556250, -0.975000, 0.000f, 0.000f, 0.000f, 0.150000f, 0.631250f, 0.037500f),
            part("FRAME", 0.306250, -0.006250, -0.912500, 0.000f, 0.000f, 0.000f, 0.137500f, 0.150000f, 0.100000f),
            part("ACCENT", -1.443750, -0.006250, -0.912500, 0.000f, 0.000f, 0.000f, 0.137500f, 0.275000f, 0.100000f),
            part("ACCENT", -1.443750, -0.006250, -0.100000, 0.000f, 0.000f, 0.000f, 0.137500f, 0.275000f, 0.100000f),
            part("ACCENT", 0.306250, -0.006250, -0.100000, 0.000f, 0.000f, 0.000f, 0.137500f, 0.275000f, 0.100000f)
        ));
    }
    private static SmpFurnitureModel default_washing_machine() {
        return model("default_washing_machine", "Washing Machine", FurnitureDefinition.Category.FURNITURE, "IRON_BLOCK", 200, 1.006250, 1.000000, List.of(
            part("IRON_BLOCK", -0.437500, 0.000000, -0.500000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.937500f),
            part("IRON_BLOCK", -0.437500, 0.937500, -0.500000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.937500f),
            part("IRON_BLOCK", -0.500000, -0.000000, 0.437500, 0.000f, 0.000f, 0.000f, 1.000000f, 1.000000f, 0.062500f),
            part("IRON_BLOCK", -0.500000, -0.000000, -0.500000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.937500f),
            part("IRON_BLOCK", 0.437500, 0.000000, -0.500000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.937500f),
            part("IRON_BLOCK", 0.250000, 0.250000, -0.500000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.500000f, 0.062500f),
            part("IRON_BLOCK", -0.437500, 0.250000, -0.500000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.500000f, 0.062500f),
            part("IRON_BLOCK", -0.437500, 0.062500, -0.500000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.062500f),
            part("IRON_BLOCK", -0.437500, 0.750000, -0.500000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.062500f),
            part("GLASS", -0.250000, 0.250000, -0.500000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.062500f),
            part("RED_WOOL", -0.318750, 0.812500, -0.506250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.010000f),
            part("GREEN_WOOL", -0.218750, 0.812500, -0.506250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.010000f),
            part("LIGHT_BLUE_STAINED_GLASS", -0.437500, 0.062500, -0.375000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.312500f, 0.812500f),
            part("IRON_BLOCK", -0.468750, 0.437500, -0.218750, -45.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel default_window() {
        return model("default_window", "3x3 Window", FurnitureDefinition.Category.FURNITURE, "GLASS", 200, 3.000000, 3.000000, List.of(
            part("SPRUCE_WOOD", -1.500000, -1.000000, -0.125000, 0.000f, 0.000f, 0.000f, 0.062500f, 3.000000f, 0.062500f),
            part("SPRUCE_WOOD", 1.437500, -1.000000, -0.125000, 0.000f, 0.000f, 0.000f, 0.062500f, 3.000000f, 0.062500f),
            part("SPRUCE_WOOD", -0.062500, -1.000000, 0.125000, 0.000f, 0.000f, 0.000f, 0.125000f, 3.000000f, 0.062500f),
            part("SPRUCE_WOOD", 0.750000, -1.000000, 0.125000, 0.000f, 0.000f, 0.000f, 0.062500f, 3.000000f, 0.062500f),
            part("SPRUCE_WOOD", -0.812500, -1.000000, 0.125000, 0.000f, 0.000f, 0.000f, 0.062500f, 3.000000f, 0.062500f),
            part("GLASS", -1.468750, -0.937500, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("GLASS", -1.468750, -0.187500, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("GLASS", -1.468750, 0.562500, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("GLASS", -1.468750, 1.312500, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.625000f, 0.062500f),
            part("SPRUCE_WOOD", -1.468750, 1.937500, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("SPRUCE_WOOD", -1.468750, 1.250000, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("SPRUCE_WOOD", -1.468750, 0.500000, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("SPRUCE_WOOD", -1.468750, -0.250000, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("SPRUCE_WOOD", -1.468750, -1.000000, 0.000000, -22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("SPRUCE_WOOD", 0.781250, -1.000000, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.781250, -0.937500, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", 0.781250, -0.250000, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.781250, -0.187500, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", 0.781250, 0.500000, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.781250, 0.562500, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", 0.781250, 1.250000, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.781250, 1.312500, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.625000f, 0.062500f),
            part("SPRUCE_WOOD", 0.781250, 1.937500, 0.000000, 22.500f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("SPRUCE_WOOD", -0.750000, -1.000000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", -0.750000, -0.937500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", -0.750000, -0.250000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", -0.750000, -0.187500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", -0.750000, 0.500000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", -0.750000, 0.562500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", -0.750000, 1.250000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", -0.750000, 1.312500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.625000f, 0.062500f),
            part("SPRUCE_WOOD", -0.750000, 1.937500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("SPRUCE_WOOD", 0.062500, -1.000000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.062500, -0.937500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", 0.062500, -0.250000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.062500, -0.187500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", 0.062500, 0.500000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.062500, 0.562500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.687500f, 0.062500f),
            part("SPRUCE_WOOD", 0.062500, 1.250000, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f),
            part("GLASS", 0.062500, 1.312500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.625000f, 0.062500f),
            part("SPRUCE_WOOD", 0.062500, 1.937500, 0.125000, 0.000f, 0.000f, 0.000f, 0.687500f, 0.062500f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel default_window_curtains() {
        return model("default_window_curtains", "Window Curtains", FurnitureDefinition.Category.FURNITURE, "TINTED_GLASS", 200, 2.000000, 1.041250, List.of(
            part("FRAME", -1.000000, 1.002500, 0.393750, 0.000f, 0.000f, 0.000f, 2.000000f, 0.078750f, 0.093750f),
            part("CURTAIN", -1.000000, 0.040000, 0.443750, 0.000f, 0.000f, 0.000f, 0.275000f, 0.966250f, 0.043750f),
            part("CURTAIN", 0.456250, 0.196250, 0.431250, 0.000f, 0.000f, 0.000f, 0.281250f, 0.810000f, 0.043750f),
            part("CURTAIN", -0.725000, 0.196250, 0.431250, 0.000f, 0.000f, 0.000f, 0.275000f, 0.810000f, 0.043750f),
            part("CURTAIN", -0.450000, 0.471250, 0.406250, 0.000f, 0.000f, 0.000f, 0.137500f, 0.535000f, 0.043750f),
            part("CURTAIN", 0.318750, 0.471250, 0.406250, 0.000f, 0.000f, 0.000f, 0.137500f, 0.535000f, 0.043750f),
            part("CURTAIN", 0.725000, 0.040000, 0.443750, 0.000f, 0.000f, 0.000f, 0.275000f, 0.966250f, 0.043750f)
        ));
    }
    private static SmpFurnitureModel farm_barrel() {
        return model("farm_barrel", "Barrel", FurnitureDefinition.Category.OUTDOOR, "BARREL", 220, 2.100000, 1.000063, List.of(
            part("SPRUCE_PLANKS", -0.687500, 0.000063, -0.937500, 0.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.078125f),
            part("SPRUCE_PLANKS", -0.687500, 0.000063, -0.125920, 0.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.093750f),
            part("SPRUCE_PLANKS", -1.062500, 0.000000, -0.937500, 45.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.093750f),
            part("SPRUCE_PLANKS", -1.327665, 0.000063, -0.672335, 90.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.093750f),
            part("SPRUCE_PLANKS", -1.327665, 0.000000, -0.297335, 135.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.093750f),
            part("SPRUCE_PLANKS", -0.312500, 0.000000, -0.125920, 45.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.093750f),
            part("SPRUCE_PLANKS", -0.047335, 0.000063, -0.391085, 90.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.093750f),
            part("SPRUCE_PLANKS", -0.047335, 0.000000, -0.766085, 135.000f, 0.000f, 0.000f, 0.375000f, 1.000000f, 0.093750f),
            part("STRIPPED_SPRUCE_WOOD", -0.047335, 0.000188, -1.109835, 90.000f, 0.000f, 0.000f, 0.375000f, 0.937500f, 0.718750f),
            part("STRIPPED_SPRUCE_WOOD", -0.047335, 0.000125, -1.484835, 135.000f, 0.000f, 0.000f, 0.375000f, 0.937500f, 0.718750f),
            part("STRIPPED_SPRUCE_WOOD", -0.687500, 0.000063, -0.859375, 0.000f, 0.000f, 0.000f, 0.375000f, 0.937500f, 0.734375f),
            part("STRIPPED_SPRUCE_WOOD", -1.062500, 0.000000, -0.843750, 45.000f, 0.000f, 0.000f, 0.375000f, 0.937500f, 0.718750f),
            part("NETHERITE_BLOCK", -1.327665, 0.665625, -0.303585, 135.000f, 0.000f, 0.000f, 0.375000f, 0.084375f, 0.918750f),
            part("NETHERITE_BLOCK", -0.687500, 0.656312, -0.944670, 0.000f, 0.000f, 0.000f, 0.375000f, 0.093750f, 0.918750f),
            part("NETHERITE_BLOCK", -1.062500, 0.656250, -0.943750, 45.000f, 0.000f, 0.000f, 0.375000f, 0.093750f, 0.918750f),
            part("NETHERITE_BLOCK", -0.047335, 0.656312, -1.209835, 90.000f, 0.000f, 0.000f, 0.375000f, 0.093750f, 0.918750f),
            part("NETHERITE_BLOCK", -1.327665, 0.290625, -0.303585, 135.000f, 0.000f, 0.000f, 0.375000f, 0.084375f, 0.918750f),
            part("NETHERITE_BLOCK", -0.687500, 0.281313, -0.944670, 0.000f, 0.000f, 0.000f, 0.375000f, 0.093750f, 0.918750f),
            part("NETHERITE_BLOCK", -0.047335, 0.281313, -1.209835, 90.000f, 0.000f, 0.000f, 0.375000f, 0.093750f, 0.918750f),
            part("NETHERITE_BLOCK", -1.062500, 0.281250, -0.943750, 45.000f, 0.000f, 0.000f, 0.375000f, 0.093750f, 0.918750f)
        ));
    }
    private static SmpFurnitureModel farm_bench() {
        return model("farm_bench", "Farm bench", FurnitureDefinition.Category.OUTDOOR, "SPRUCE_STAIRS", 220, 2.000000, 1.375000, List.of(
            part("SPRUCE_LOG", -0.375000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("SPRUCE_LOG", -1.750000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("SPRUCE_LOG", -1.750000, 0.000000, -0.375000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("SPRUCE_PLANKS", -2.000000, 0.625000, -0.937500, 0.000f, 0.000f, 0.000f, 2.000000f, 0.062500f, 0.312500f),
            part("SPRUCE_PLANKS", -2.000000, 0.625000, -0.562500, 0.000f, 0.000f, 0.000f, 2.000000f, 0.062500f, 0.312500f),
            part("SPRUCE_LOG", -0.375000, 0.000000, -0.375000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.562500f, 0.125000f),
            part("SPRUCE_LOG", -1.750625, 0.562500, -0.250000, 0.000f, 12.500f, 0.000f, 0.125000f, 0.812500f, 0.062500f),
            part("SPRUCE_PLANKS", -2.000000, 0.750000, -0.312500, 0.000f, 12.500f, 0.000f, 2.000000f, 0.250000f, 0.062500f),
            part("CHISELED_STONE_BRICKS", -0.328125, 1.171875, -0.318750, 0.000f, 12.500f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("SPRUCE_LOG", -0.375062, 0.562500, -0.250000, 0.000f, 12.500f, 0.000f, 0.125000f, 0.812500f, 0.062500f),
            part("SIT", -0.968750, 0.687500, -0.500000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.062500f),
            part("CHISELED_STONE_BRICKS", -0.328125, 0.687500, -0.828125, 0.000f, 0.000f, 0.000f, 0.031250f, 0.010000f, 0.031250f),
            part("CHISELED_STONE_BRICKS", -0.328125, 0.687500, -0.390625, 0.000f, 0.000f, 0.000f, 0.031250f, 0.010000f, 0.031250f),
            part("CHISELED_STONE_BRICKS", -1.703125, 0.687500, -0.828125, 0.000f, 0.000f, 0.000f, 0.031250f, 0.010000f, 0.031250f),
            part("CHISELED_STONE_BRICKS", -1.703125, 0.687500, -0.390625, 0.000f, 0.000f, 0.000f, 0.031250f, 0.010000f, 0.031250f),
            part("SPRUCE_PLANKS", -2.000000, 1.062500, -0.312500, 0.000f, 12.500f, 0.000f, 2.000000f, 0.250000f, 0.062500f),
            part("CHISELED_STONE_BRICKS", -1.703125, 1.171875, -0.318750, 0.000f, 12.500f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("CHISELED_STONE_BRICKS", -1.703125, 0.828125, -0.318750, 0.000f, 12.500f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("CHISELED_STONE_BRICKS", -0.328125, 0.828125, -0.318750, 0.000f, 12.500f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("SPRUCE_LOG", -0.375000, 0.562500, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.625000f),
            part("SPRUCE_LOG", -1.750000, 0.562500, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.625000f)
        ));
    }
    private static SmpFurnitureModel farm_cart() {
        return model("farm_cart", "Cart", FurnitureDefinition.Category.OUTDOOR, "CHEST_MINECART", 220, 2.628743, 1.432105, List.of(
            part("MUDDY_MANGROVE_ROOTS", -0.312500, 0.000000, -0.500000, 0.000f, 7.500f, 0.000f, 0.125000f, 0.500000f, 0.500000f),
            part("STRIPPED_OAK_WOOD", -0.318750, 0.109375, -0.390625, 0.000f, 7.500f, 0.000f, 0.137500f, 0.281250f, 0.281250f),
            part("OAK_WOOD", -1.687500, 0.312500, -1.562500, 0.000f, 7.500f, 0.000f, 1.375000f, 0.125000f, 1.875000f),
            part("STRIPPED_OAK_WOOD", -0.500000, 0.000000, -1.312500, 0.000f, 0.000f, 10.000f, 0.062500f, 0.625000f, 0.125000f),
            part("OAK_WOOD", -1.562500, 0.328125, -2.125000, 0.000f, 7.500f, 0.000f, 0.125000f, 0.093750f, 0.750000f),
            part("OAK_WOOD", -0.437500, 0.437500, -1.187500, 0.000f, 7.500f, 0.000f, 0.062500f, 0.625000f, 0.125000f),
            part("OAK_WOOD", -0.437500, 0.437500, -0.562500, 0.000f, 7.500f, 0.000f, 0.062500f, 0.625000f, 0.125000f),
            part("OAK_WOOD", -0.437500, 0.437500, 0.062500, 0.000f, 7.500f, 0.000f, 0.062500f, 0.625000f, 0.125000f),
            part("OAK_WOOD", -1.625000, 0.437500, 0.062500, 0.000f, 7.500f, 0.000f, 0.062500f, 0.625000f, 0.125000f),
            part("OAK_WOOD", -1.625000, 0.437500, -0.562500, 0.000f, 7.500f, 0.000f, 0.062500f, 0.625000f, 0.125000f),
            part("OAK_WOOD", -1.625000, 0.437500, -1.187500, 0.000f, 7.500f, 0.000f, 0.062500f, 0.625000f, 0.125000f),
            part("OAK_WOOD", -1.281250, 0.437500, 0.203125, 0.000f, 7.500f, 0.000f, 0.125000f, 0.625000f, 0.062500f),
            part("OAK_WOOD", -0.843750, 0.437500, 0.203125, 0.000f, 7.500f, 0.000f, 0.125000f, 0.625000f, 0.062500f),
            part("STRIPPED_OAK_WOOD", -0.500000, 0.500000, -1.500000, 0.000f, 7.500f, 0.000f, 0.062500f, 0.187500f, 1.703125f),
            part("STRIPPED_OAK_WOOD", -0.500000, 0.781250, -1.500000, 0.000f, 7.500f, 0.000f, 0.062500f, 0.187500f, 1.703125f),
            part("STRIPPED_OAK_WOOD", -1.562500, 0.781250, -1.500000, 0.000f, 7.500f, 0.000f, 0.062500f, 0.187500f, 1.703125f),
            part("STRIPPED_OAK_WOOD", -1.562500, 0.500000, -1.500000, 0.000f, 7.500f, 0.000f, 0.062500f, 0.187500f, 1.703125f),
            part("STRIPPED_OAK_WOOD", -1.500000, 0.781250, 0.140625, 0.000f, 7.500f, 0.000f, 1.000000f, 0.187500f, 0.062500f),
            part("STRIPPED_OAK_WOOD", -1.500000, 0.500000, 0.140625, 0.000f, 7.500f, 0.000f, 1.000000f, 0.187500f, 0.062500f),
            part("OAK_WOOD", -0.562500, 0.328125, -2.125000, 0.000f, 7.500f, 0.000f, 0.125000f, 0.093750f, 0.750000f),
            part("STRIPPED_OAK_WOOD", -1.818750, 0.109375, -0.390625, 0.000f, 7.500f, 0.000f, 0.137500f, 0.281250f, 0.281250f),
            part("MUDDY_MANGROVE_ROOTS", -1.812500, 0.000000, -0.500000, 0.000f, 7.500f, 0.000f, 0.125000f, 0.500000f, 0.500000f),
            part("STRIPPED_OAK_WOOD", -1.562500, 0.000000, -1.312500, 0.000f, 0.000f, -10.000f, 0.062500f, 0.625000f, 0.125000f),
            part("HAY_BLOCK", -1.312500, 0.432105, -0.121257, 0.000f, -82.500f, 0.000f, 0.625000f, 1.000000f, 0.625000f)
        ));
    }
    private static SmpFurnitureModel farm_drink_barrel() {
        return model("farm_drink_barrel", "Drink barrel", FurnitureDefinition.Category.OUTDOOR, "OMINOUS_BOTTLE", 220, 0.953125, 0.875000, List.of(
            part("STRIPPED_SPRUCE_WOOD", -0.187500, 0.000000, -0.968750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("STRIPPED_SPRUCE_WOOD", -0.187500, 0.000000, -0.156250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("STRIPPED_SPRUCE_WOOD", -0.937500, 0.000000, -0.156250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("STRIPPED_SPRUCE_WOOD", -0.937500, 0.000000, -0.968750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("SPRUCE_WOOD", -0.875000, 0.125000, -0.906250, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.828125f),
            part("BARREL", -0.812500, 0.250000, -0.843750, 0.000f, 0.000f, 0.000f, 0.625000f, 0.625000f, 0.703125f),
            part("POLISHED_ANDESITE", -0.546875, 0.484375, -0.906250, 0.000f, 0.000f, 0.000f, 0.093750f, 0.093750f, 0.062500f),
            part("POLISHED_ANDESITE", -0.525000, 0.506250, -0.968750, 0.000f, 0.000f, 0.000f, 0.050000f, 0.050000f, 0.062500f),
            part("POLISHED_ANDESITE", -0.525000, 0.459375, -0.984375, 0.000f, 0.000f, 0.000f, 0.050000f, 0.096875f, 0.015625f)
        ));
    }
    private static SmpFurnitureModel farm_fence() {
        return model("farm_fence", "Fence", FurnitureDefinition.Category.OUTDOOR, "PALE_OAK_FENCE", 220, 2.000000, 1.562500, List.of(
            part("STRIPPED_PALE_OAK_WOOD", -1.093750, 0.000000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 1.562500f, 0.187500f),
            part("STRIPPED_PALE_OAK_WOOD", -2.000000, 0.328125, -0.546875, 0.000f, 0.000f, 0.000f, 2.000000f, 0.140625f, 0.093750f),
            part("STRIPPED_PALE_OAK_WOOD", -2.000000, 0.734375, -0.546875, 0.000f, 0.000f, 0.000f, 2.000000f, 0.140625f, 0.093750f),
            part("STRIPPED_PALE_OAK_WOOD", -2.000000, 1.140625, -0.546875, 0.000f, 0.000f, 0.000f, 2.000000f, 0.140625f, 0.093750f)
        ));
    }
    private static SmpFurnitureModel farm_fence_corner() {
        return model("farm_fence_corner", "Fence corner", FurnitureDefinition.Category.OUTDOOR, "OAK_FENCE", 220, 0.593750, 1.562500, List.of(
            part("STRIPPED_PALE_OAK_WOOD", -0.593750, 0.000000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 1.562500f, 0.187500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.437500, 0.328125, -0.546875, 0.000f, 0.000f, 0.000f, 0.437500f, 0.140625f, 0.093750f),
            part("STRIPPED_PALE_OAK_WOOD", -0.437500, 0.734375, -0.546875, 0.000f, 0.000f, 0.000f, 0.437500f, 0.140625f, 0.093750f),
            part("STRIPPED_PALE_OAK_WOOD", -0.437500, 1.140625, -0.546875, 0.000f, 0.000f, 0.000f, 0.437500f, 0.140625f, 0.093750f),
            part("STRIPPED_PALE_OAK_WOOD", -0.546875, 1.140625, -0.406250, 0.000f, 0.000f, 0.000f, 0.093750f, 0.140625f, 0.406250f),
            part("STRIPPED_PALE_OAK_WOOD", -0.546875, 0.734375, -0.406250, 0.000f, 0.000f, 0.000f, 0.093750f, 0.140625f, 0.406250f),
            part("STRIPPED_PALE_OAK_WOOD", -0.546875, 0.328125, -0.406250, 0.000f, 0.000f, 0.000f, 0.093750f, 0.140625f, 0.406250f)
        ));
    }
    private static SmpFurnitureModel farm_food_trough() {
        return model("farm_food_trough", "Food trough", FurnitureDefinition.Category.OUTDOOR, "HAY_BLOCK", 220, 1.875000, 0.687500, List.of(
            part("BARREL", -1.937500, 0.000625, 0.000000, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.687500f),
            part("BARREL", -0.187500, 0.000000, -0.562500, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.312500f),
            part("SPRUCE_PLANKS", -1.812500, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 1.625000f, 0.312500f, 0.125000f),
            part("SPRUCE_PLANKS", -1.812500, 0.000000, -0.125000, 0.000f, 0.000f, 0.000f, 1.625000f, 0.687500f, 0.125000f),
            part("SPRUCE_PLANKS", -1.937500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.437500f),
            part("SPRUCE_PLANKS", -0.187500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.437500f),
            part("HAY_BLOCK", -1.812500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 1.625000f, 0.250000f, 0.437500f),
            part("BARREL", -1.937500, 0.000000, -0.562500, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.312500f),
            part("BARREL", -0.187500, 0.000625, 0.000000, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.687500f)
        ));
    }
    private static SmpFurnitureModel farm_item_sign() {
        return model("farm_item_sign", "Item sign", FurnitureDefinition.Category.OUTDOOR, "OAK_SIGN", 220, 0.968750, 1.265625, List.of(
            part("OAK_WOOD", -0.546875, 0.000000, -0.531250, 0.000f, 0.000f, 0.000f, 0.093750f, 0.578125f, 0.062500f),
            part("OAK_PLANKS", -0.984375, 0.578125, -0.531250, 0.000f, 0.000f, 0.000f, 0.968750f, 0.500000f, 0.062500f),
            part("AIR", -0.500000, 0.843750, -0.531250, 0.000f, 0.000f, 0.000f, 0.421875f, 0.421875f, 0.015625f)
        ));
    }
    private static SmpFurnitureModel farm_ladder() {
        return model("farm_ladder", "Ladder", FurnitureDefinition.Category.OUTDOOR, "LADDER", 220, 0.715625, 3.162500, List.of(
            part("OAK_PLANKS", -0.250000, 0.000000, -1.009375, 0.000f, 18.450f, 0.000f, 0.106250f, 3.162500f, 0.125000f),
            part("OAK_PLANKS", -0.859375, 0.000000, -1.009375, 0.000f, 18.450f, 0.000f, 0.106250f, 3.162500f, 0.125000f),
            part("OAK_PLANKS", -0.765625, 0.218750, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f),
            part("OAK_PLANKS", -0.765625, 0.600000, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f),
            part("OAK_PLANKS", -0.765625, 0.981250, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f),
            part("OAK_PLANKS", -0.765625, 1.362500, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f),
            part("OAK_PLANKS", -0.765625, 1.743750, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f),
            part("OAK_PLANKS", -0.765625, 2.125000, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f),
            part("OAK_PLANKS", -0.765625, 2.500000, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f),
            part("OAK_PLANKS", -0.765625, 2.875000, -0.993750, 0.000f, 18.450f, 0.000f, 0.515625f, 0.093750f, 0.093750f)
        ));
    }
    private static SmpFurnitureModel farm_lamp() {
        return model("farm_lamp", "Lamp", FurnitureDefinition.Category.OUTDOOR, "LANTERN", 220, 1.145009, 0.593750, List.of(
            part("NETHERITE_BLOCK", -0.671875, 0.000000, -0.671906, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("NETHERITE_BLOCK", -0.625000, 0.062500, -0.625031, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("NETHERITE_BLOCK", -0.562500, 0.125000, -0.562531, 0.000f, 0.000f, 0.000f, 0.125000f, 0.031250f, 0.125000f),
            part("YELLOW_STAINED_GLASS", -0.562500, 0.156250, -0.562531, 0.000f, 0.000f, 0.000f, 0.125000f, 0.281250f, 0.125000f),
            part("YELLOW_STAINED_GLASS", -0.593750, 0.187500, -0.593781, 0.000f, 0.000f, 0.000f, 0.187500f, 0.218750f, 0.187500f),
            part("NETHERITE_BLOCK", -0.562500, 0.437500, -0.562531, 0.000f, 0.000f, 0.000f, 0.125000f, 0.156250f, 0.125000f),
            part("NETHERITE_BLOCK", -0.593750, 0.468750, -0.593781, 0.000f, 0.000f, 0.000f, 0.187500f, 0.093750f, 0.187500f),
            part("NETHERITE_BLOCK", -0.718750, 0.071875, -0.521906, 0.000f, 0.000f, -15.000f, 0.093750f, 0.043750f, 0.043750f),
            part("NETHERITE_BLOCK", -1.072504, 0.095820, -0.521844, 0.000f, 0.000f, -97.500f, 0.359375f, 0.043750f, 0.043750f),
            part("NETHERITE_BLOCK", -0.659610, 0.448827, -0.521906, 0.000f, 0.000f, 16.000f, 0.140625f, 0.043750f, 0.043750f),
            part("NETHERITE_BLOCK", -0.481015, 0.448827, -0.521906, 0.000f, 0.000f, -16.000f, 0.140625f, 0.043750f, 0.043750f),
            part("NETHERITE_BLOCK", -0.375000, 0.071875, -0.521906, 0.000f, 0.000f, 15.000f, 0.093750f, 0.043750f, 0.043750f),
            part("NETHERITE_BLOCK", -0.286871, 0.095820, -0.521844, 0.000f, 0.000f, 97.500f, 0.359375f, 0.043750f, 0.043750f),
            part("WHITE_CANDLE", -0.687500, 0.156250, -0.687531, 0.000f, 0.000f, 0.000f, 0.375000f, 0.218750f, 0.375000f)
        ));
    }
    private static SmpFurnitureModel farm_log_axe() {
        return model("farm_log_axe", "Log axe", FurnitureDefinition.Category.OUTDOOR, "OAK_LOG", 220, 1.156250, 1.125000, List.of(
            part("OAK_LOG", -1.000000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.250000f, 1.000000f),
            part("OAK_LOG", -0.500000, 0.250000, -0.687500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.687500f, 0.375000f),
            part("OAK_LOG", -0.687500, 0.234375, -0.688125, 0.000f, 0.000f, 5.000f, 0.203125f, 0.703125f, 0.375000f),
            part("IRON_AXE", -1.156250, -0.187500, -0.921875, 65.000f, 90.000f, -90.000f, 0.875000f, 0.875000f, 0.531250f)
        ));
    }
    private static SmpFurnitureModel farm_milk_tank() {
        return model("farm_milk_tank", "Milk tank", FurnitureDefinition.Category.OUTDOOR, "BIRCH_WOOD", 220, 0.656250, 0.878125, List.of(
            part("IRON_BLOCK", -0.703125, 0.000000, -0.663250, 0.000f, 0.000f, 0.000f, 0.406250f, 0.562500f, 0.406250f),
            part("IRON_BLOCK", -0.671875, 0.562500, -0.632000, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("IRON_BLOCK", -0.640625, 0.625000, -0.600750, 0.000f, 0.000f, 0.000f, 0.281250f, 0.187500f, 0.281250f),
            part("IRON_BLOCK", -0.671875, 0.812500, -0.632000, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("POLISHED_ANDESITE", -0.625000, 0.875000, -0.585125, 0.000f, 0.000f, 0.000f, 0.250000f, 0.010000f, 0.250000f),
            part("MILK_BUCKET", -0.500000, 0.250000, -0.666375, 0.000f, 0.000f, 0.000f, 0.312500f, 0.312500f, 0.010000f),
            part("IRON_BLOCK", -0.828125, 0.562500, -0.600688, 0.000f, 0.000f, -37.500f, 0.156250f, 0.031250f, 0.031250f),
            part("IRON_BLOCK", -0.828125, 0.562500, -0.350812, 0.000f, 0.000f, -37.500f, 0.156250f, 0.031250f, 0.031250f),
            part("IRON_BLOCK", -0.828125, 0.562500, -0.569438, 0.000f, 0.000f, -37.500f, 0.031250f, 0.031250f, 0.218750f),
            part("IRON_BLOCK", -0.328125, 0.562500, -0.600688, 0.000f, 0.000f, 37.500f, 0.156250f, 0.031250f, 0.031250f),
            part("IRON_BLOCK", -0.203125, 0.562500, -0.569438, 0.000f, 0.000f, 37.500f, 0.031250f, 0.031250f, 0.218750f),
            part("IRON_BLOCK", -0.328125, 0.562500, -0.350812, 0.000f, 0.000f, 37.500f, 0.156250f, 0.031250f, 0.031250f)
        ));
    }
    private static SmpFurnitureModel farm_pitchforks() {
        return model("farm_pitchforks", "Pitchforks", FurnitureDefinition.Category.OUTDOOR, "TRIDENT", 220, 0.437500, 1.625000, List.of(
            part("STRIPPED_OAK_WOOD", -0.546875, 0.500000, -0.515625, 0.000f, 15.200f, 0.000f, 0.093750f, 1.125000f, 0.093750f),
            part("SMOOTH_BASALT", -0.562500, 0.437500, -0.531250, 0.000f, 15.200f, 0.000f, 0.125000f, 0.062500f, 0.125000f),
            part("SMOOTH_BASALT", -0.718750, 0.375000, -0.531250, 0.000f, 15.200f, 0.000f, 0.437500f, 0.062500f, 0.125000f),
            part("SMOOTH_BASALT", -0.359375, 0.000000, -0.500000, 0.000f, 15.200f, 0.000f, 0.062500f, 0.375000f, 0.062500f),
            part("SMOOTH_BASALT", -0.531250, 0.000000, -0.500000, 0.000f, 15.200f, 0.000f, 0.062500f, 0.375000f, 0.062500f),
            part("SMOOTH_BASALT", -0.703125, 0.000000, -0.500000, 0.000f, 15.200f, 0.000f, 0.062500f, 0.375000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel farm_scarecrow() {
        return model("farm_scarecrow", "Scarecrow", FurnitureDefinition.Category.OUTDOOR, "CARVED_PUMPKIN", 220, 1.000000, 2.156250, List.of(
            part("STRIPPED_OAK_WOOD", -0.562500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.875000f, 0.125000f),
            part("OXIDIZED_COPPER_GRATE", -0.750000, 0.937500, -0.656250, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.312500f),
            part("OXIDIZED_COPPER_GRATE", -1.000000, 1.062500, -0.593750, 0.000f, 0.000f, -55.000f, 0.187500f, 0.312500f, 0.187500f),
            part("OXIDIZED_COPPER_GRATE", -0.187500, 1.062500, -0.593750, 0.000f, 0.000f, 55.000f, 0.187500f, 0.312500f, 0.187500f),
            part("CARVED_PUMPKIN", -0.781250, 1.437500, -0.781250, 0.000f, 0.000f, 0.000f, 0.562500f, 0.500000f, 0.562500f),
            part("HAY_BLOCK", -0.906250, 1.906250, -0.906250, 0.000f, 10.000f, 0.000f, 0.812500f, 0.031250f, 0.812500f),
            part("HAY_BLOCK", -0.787500, 1.937500, -0.787500, 0.000f, 10.000f, 0.000f, 0.575000f, 0.218750f, 0.575000f),
            part("HAY_BLOCK", -0.993750, 0.937500, -0.587500, 0.000f, 0.000f, -55.000f, 0.175000f, 0.500000f, 0.175000f),
            part("HAY_BLOCK", -0.181250, 0.937500, -0.587500, 0.000f, 0.000f, 55.000f, 0.175000f, 0.500000f, 0.175000f),
            part("HAY_BLOCK", -0.743750, 0.812500, -0.650000, 0.000f, 0.000f, 0.000f, 0.487500f, 0.625000f, 0.300000f)
        ));
    }
    private static SmpFurnitureModel farm_shelf() {
        return model("farm_shelf", "Shelf", FurnitureDefinition.Category.OUTDOOR, "STRIPPED_OAK_LOG", 220, 1.999375, 0.250000, List.of(
            part("STRIPPED_OAK_LOG", -1.999375, 0.906250, -0.937500, 0.000f, 0.000f, 0.000f, 1.999375f, 0.093750f, 0.937500f),
            part("STRIPPED_OAK_WOOD", -0.437500, 0.906250, -0.600000, 0.000f, 52.500f, 0.000f, 0.125000f, 0.187500f, 0.740625f),
            part("STRIPPED_OAK_WOOD", -1.687500, 0.906250, -0.600000, 0.000f, 52.500f, 0.000f, 0.125000f, 0.187500f, 0.740625f)
        ));
    }
    private static SmpFurnitureModel farm_shovel() {
        return model("farm_shovel", "Shovel", FurnitureDefinition.Category.OUTDOOR, "STONE_SHOVEL", 220, 0.437500, 1.625000, List.of(
            part("STRIPPED_OAK_WOOD", -0.546875, 0.500000, -0.546875, 0.000f, 16.300f, 0.000f, 0.093750f, 1.125000f, 0.093750f),
            part("SMOOTH_BASALT", -0.546875, 0.437500, -0.546875, 0.000f, 16.300f, 0.000f, 0.093750f, 0.062500f, 0.093750f),
            part("SMOOTH_BASALT", -0.718750, 0.187500, -0.546875, 0.000f, 16.300f, 0.000f, 0.437500f, 0.250625f, 0.093750f),
            part("SMOOTH_BASALT", -0.687500, 0.093750, -0.546937, 0.000f, 16.300f, 0.000f, 0.375000f, 0.093750f, 0.094375f),
            part("SMOOTH_BASALT", -0.640625, 0.031250, -0.546875, 0.000f, 16.300f, 0.000f, 0.281250f, 0.062500f, 0.093750f),
            part("SMOOTH_BASALT", -0.578125, 0.000000, -0.546875, 0.000f, 16.300f, 0.000f, 0.156250f, 0.031250f, 0.093750f)
        ));
    }
    private static SmpFurnitureModel farm_tractor() {
        return model("farm_tractor", "Tractor", FurnitureDefinition.Category.OUTDOOR, "MUDDY_MANGROVE_ROOTS", 220, 3.593750, 2.750000, List.of(
            part("NETHERITE_BLOCK", -1.125000, 0.618750, -2.000000, 0.000f, 0.000f, 0.000f, 1.250000f, 0.131250f, 0.250000f),
            part("NETHERITE_BLOCK", -1.000000, 0.625000, -1.875000, 0.000f, 0.000f, 0.000f, 1.000000f, 1.000000f, 0.062500f),
            part("GREEN_SHULKER_BOX", -1.000000, 0.625000, -1.812500, 0.000f, 0.000f, 0.000f, 1.000000f, 1.000000f, 1.375000f),
            part("MUDDY_MANGROVE_ROOTS", -1.375000, 0.000000, -1.625000, 0.000f, 90.000f, 0.000f, 0.375000f, 0.937500f, 0.937500f),
            part("YELLOW_CONCRETE", -1.390625, 0.187500, -1.437500, 0.000f, 0.000f, 0.000f, 0.406250f, 0.562500f, 0.562500f),
            part("MUDDY_MANGROVE_ROOTS", 0.000000, 0.000000, -1.625000, 0.000f, -90.000f, 0.000f, 0.375000f, 0.937500f, 0.937500f),
            part("YELLOW_CONCRETE", -0.015625, 0.187500, -1.437500, 0.000f, 0.000f, 0.000f, 0.406250f, 0.562500f, 0.562500f),
            part("GREEN_SHULKER_BOX", -1.375000, 0.500000, -0.312500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.250000f, 0.375000f),
            part("GREEN_SHULKER_BOX", -1.250000, 0.625000, -0.437500, 0.000f, 0.000f, 0.000f, 0.187500f, 1.937500f, 0.125000f),
            part("GREEN_SHULKER_BOX", -1.250000, 2.562500, -0.500000, 0.000f, 0.000f, 0.000f, 1.500000f, 0.187500f, 1.500000f),
            part("GREEN_SHULKER_BOX", 0.062500, 0.625000, -0.437500, 0.000f, 0.000f, 0.000f, 0.187500f, 1.937500f, 0.125000f),
            part("GREEN_SHULKER_BOX", -1.062500, 0.625000, -0.437500, 0.000f, 0.000f, 0.000f, 1.125000f, 1.062500f, 0.125000f),
            part("GREEN_SHULKER_BOX", -1.250000, 0.625000, 0.750000, 0.000f, 0.000f, 0.000f, 0.187500f, 1.937500f, 0.187500f),
            part("GREEN_SHULKER_BOX", 0.062500, 0.625000, 0.750000, 0.000f, 0.000f, 0.000f, 0.187500f, 1.937500f, 0.187500f),
            part("GREEN_SHULKER_BOX", -1.062500, 0.625000, 0.750000, 0.000f, 0.000f, 0.000f, 1.125000f, 1.062500f, 0.187500f),
            part("NETHERITE_BLOCK", -1.125000, 1.375000, 0.562500, 0.000f, 0.000f, 0.000f, 1.250000f, 0.312500f, 0.187500f),
            part("NETHERITE_BLOCK", -1.125000, 1.000000, 0.187500, 0.000f, 0.000f, 0.000f, 1.250000f, 0.375000f, 0.562500f),
            part("GREEN_SHULKER_BOX", -1.250000, 0.625000, -0.312500, 0.000f, 0.000f, 0.000f, 1.500000f, 0.375000f, 1.062500f),
            part("GREEN_SHULKER_BOX", -1.750000, 1.437500, 0.062500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.125000f, 1.000000f),
            part("MUDDY_MANGROVE_ROOTS", -1.812500, 0.000000, 0.218750, 0.000f, 90.000f, 0.000f, 0.500000f, 1.375000f, 1.375000f),
            part("YELLOW_CONCRETE", -1.828125, 0.343750, 0.562500, 0.000f, 0.000f, 0.000f, 0.531250f, 0.687500f, 0.687500f),
            part("GREEN_SHULKER_BOX", -1.500000, 0.250000, -0.312500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.250000f, 0.375000f),
            part("GREEN_SHULKER_BOX", -1.750000, 0.562500, 0.062500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.875000f, 0.125000f),
            part("GREEN_SHULKER_BOX", 0.250000, 0.562500, 0.062500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.875000f, 0.125000f),
            part("GREEN_SHULKER_BOX", 0.250000, 1.437500, 0.062500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.125000f, 1.000000f),
            part("MUDDY_MANGROVE_ROOTS", 0.312500, 0.000000, 0.218750, 0.000f, -90.000f, 0.000f, 0.500000f, 1.375000f, 1.375000f),
            part("YELLOW_CONCRETE", 0.296875, 0.343750, 0.562500, 0.000f, 0.000f, 0.000f, 0.531250f, 0.687500f, 0.687500f),
            part("GREEN_SHULKER_BOX", -1.187500, 0.625000, 0.937500, 0.000f, 0.000f, 0.000f, 1.375000f, 1.062500f, 0.375000f),
            part("GLASS", -1.062500, 1.687500, -0.437500, 0.000f, 0.000f, 0.000f, 1.125000f, 0.875000f, 0.125000f),
            part("GLASS", -1.062500, 1.687500, 0.750000, 0.000f, 0.000f, 0.000f, 1.125000f, 0.875000f, 0.187500f),
            part("NETHERITE_BLOCK", -1.328125, 0.593750, 0.812500, 0.000f, 0.000f, 0.000f, 1.656250f, 0.187500f, 0.187500f),
            part("GREEN_SHULKER_BOX", 0.125000, 0.875000, 0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.687500f),
            part("GREEN_SHULKER_BOX", -1.250000, 0.875000, 0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.687500f),
            part("GREEN_CONCRETE", -0.156250, 0.875000, -1.875625, 0.000f, 0.000f, 0.000f, 0.062500f, 0.625000f, 0.010000f),
            part("GREEN_CONCRETE", -0.343750, 0.875000, -1.875625, 0.000f, 0.000f, 0.000f, 0.062500f, 0.625000f, 0.010000f),
            part("GREEN_CONCRETE", -0.531250, 0.875000, -1.875625, 0.000f, 0.000f, 0.000f, 0.062500f, 0.625000f, 0.010000f),
            part("GREEN_CONCRETE", -0.906250, 0.875000, -1.875625, 0.000f, 0.000f, 0.000f, 0.062500f, 0.625000f, 0.010000f),
            part("GREEN_CONCRETE", -0.718750, 0.875000, -1.875625, 0.000f, 0.000f, 0.000f, 0.062500f, 0.625000f, 0.010000f),
            part("YELLOW_CONCRETE", -1.018750, 1.406250, -1.887500, 0.000f, 0.000f, 0.000f, 0.268750f, 0.187500f, 0.075000f),
            part("YELLOW_CONCRETE", -0.250000, 1.406250, -1.887500, 0.000f, 0.000f, 0.000f, 0.268750f, 0.187500f, 0.075000f),
            part("IRON_BLOCK", -1.125000, 1.375000, -1.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 1.125000f, 0.125000f),
            part("IRON_BLOCK", -1.171875, 1.812500, -1.109375, 0.000f, 0.000f, 0.000f, 0.218750f, 0.375000f, 0.218750f),
            part("IRON_BLOCK", -1.124938, 2.443750, -1.046875, 0.000f, -25.000f, 0.000f, 0.124875f, 0.196875f, 0.125000f),
            part("NETHERITE_BLOCK", -0.781250, 1.531250, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.203125f),
            part("NETHERITE_BLOCK", -0.906250, 1.750000, -0.125000, 0.000f, 0.000f, 0.000f, 0.312500f, 0.062500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.906250, 1.375000, -0.125000, 0.000f, 0.000f, 0.000f, 0.312500f, 0.062500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.593750, 1.375000, -0.125000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.968750, 1.375000, -0.125000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.775000, 1.375000, -0.125000, 0.000f, -17.500f, 0.000f, 0.050000f, 0.187500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.937500, 1.687500, -0.125000, 16.130f, -6.880f, -35.976f, 0.218750f, 0.062500f, 0.062500f),
            part("NETHERITE_BLOCK", -0.781250, 1.687500, -0.125000, -16.130f, -6.880f, 35.976f, 0.218750f, 0.062500f, 0.062500f),
            part("SIT", -0.750000, 1.375000, 0.375000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel farm_wall_item_sign() {
        return model("farm_wall_item_sign", "Wall item sign", FurnitureDefinition.Category.OUTDOOR, "OAK_HANGING_SIGN", 220, 0.968750, 0.687500, List.of(
            part("OAK_PLANKS", -0.984375, 0.250000, -0.062500, 0.000f, 0.000f, 0.000f, 0.968750f, 0.500000f, 0.062500f),
            part("AIR", -0.500000, 0.515625, -0.062500, 0.000f, 0.000f, 0.000f, 0.421875f, 0.421875f, 0.015625f)
        ));
    }
    private static SmpFurnitureModel farm_water_trough() {
        return model("farm_water_trough", "Water trough", FurnitureDefinition.Category.OUTDOOR, "BLUE_ICE", 220, 1.875000, 0.687500, List.of(
            part("BARREL", -1.937500, 0.000625, 0.000000, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.687500f),
            part("BARREL", -1.937500, 0.000000, -0.562500, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.312500f),
            part("SPRUCE_PLANKS", -1.812500, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 1.625000f, 0.312500f, 0.125000f),
            part("SPRUCE_PLANKS", -1.812500, 0.000000, -0.125000, 0.000f, 0.000f, 0.000f, 1.625000f, 0.687500f, 0.125000f),
            part("SPRUCE_PLANKS", -1.937500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.437500f),
            part("SPRUCE_PLANKS", -0.187500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.437500f),
            part("BLUE_ICE", -1.812500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 1.625000f, 0.250000f, 0.437500f),
            part("BARREL", -0.187500, 0.000000, -0.562500, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.312500f),
            part("BARREL", -0.187500, 0.000625, 0.000000, 0.000f, -90.000f, 0.000f, 0.125000f, 0.125000f, 0.687500f)
        ));
    }
    private static SmpFurnitureModel farm_water_well() {
        return model("farm_water_well", "Water well", FurnitureDefinition.Category.OUTDOOR, "WATER_BUCKET", 220, 0.834375, 1.356250, List.of(
            part("NETHERITE_BLOCK", -0.562500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 1.125000f, 0.125000f),
            part("NETHERITE_BLOCK", -0.546875, 0.875000, -0.812500, 0.000f, 0.000f, 0.000f, 0.093750f, 0.125000f, 0.250000f),
            part("NETHERITE_BLOCK", -0.546875, 0.781250, -0.812500, 0.000f, 0.000f, 0.000f, 0.093750f, 0.093750f, 0.093750f),
            part("NETHERITE_BLOCK", -0.521875, 1.125000, -0.521875, 0.000f, 0.000f, 0.000f, 0.043750f, 0.187500f, 0.043750f),
            part("NETHERITE_BLOCK", -0.521875, 1.312500, -0.521875, 0.000f, 0.000f, 0.000f, 0.043750f, 0.043750f, 0.121875f),
            part("NETHERITE_BLOCK", -0.521937, 1.309911, -0.406250, 0.000f, 45.000f, 0.000f, 0.043750f, 0.043750f, 0.365625f),
            part("NETHERITE_BLOCK", -0.521875, 1.087500, -0.459375, 0.000f, 32.500f, 0.000f, 0.043750f, 0.193750f, 0.043750f),
            part("BLUE_ICE", -0.750000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.312500f, 0.010000f, 0.250000f),
            part("BLUE_ICE", -0.437500, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.312500f, 0.010000f, 0.312500f),
            part("BLUE_ICE", -0.531250, 0.780625, -0.796875, 0.000f, 0.000f, 0.000f, 0.062500f, 0.010000f, 0.062500f),
            part("BLUE_ICE", -0.687500, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.010000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel farm_weather_vane() {
        return model("farm_weather_vane", "Weather vane", FurnitureDefinition.Category.OUTDOOR, "RECOVERY_COMPASS", 220, 0.953125, 1.375000, List.of(
            part("BLACKSTONE", -0.578125, 0.000000, -0.578125, 0.000f, 0.000f, 0.000f, 0.156250f, 0.031250f, 0.156250f),
            part("BLACKSTONE", -0.531250, 0.031250, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 1.281250f, 0.062500f),
            part("BLACKSTONE", -0.537500, 1.312500, -0.537500, 0.000f, 0.000f, 0.000f, 0.075000f, 0.062500f, 0.075000f),
            part("IRON_BLOCK", -0.843750, 1.078125, -0.531875, 0.000f, 0.000f, 0.000f, 0.656250f, 0.062500f, 0.064375f),
            part("IRON_BLOCK", -0.875000, 1.015625, -0.531250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.187500f, 0.062500f),
            part("IRON_BLOCK", -0.906250, 1.031250, -0.531250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.156250f, 0.062500f),
            part("IRON_BLOCK", -0.937500, 1.046875, -0.531250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.125000f, 0.062500f),
            part("IRON_BLOCK", -0.187500, 1.040625, -0.531312, 0.000f, 0.000f, 0.000f, 0.140625f, 0.137500f, 0.062500f),
            part("IRON_BLOCK", -0.968750, 1.062500, -0.531250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.093750f, 0.062500f),
            part("BLACKSTONE", -0.765625, 0.718750, -0.515563, 0.000f, 0.000f, 0.000f, 0.531250f, 0.031250f, 0.031125f),
            part("BLACKSTONE", -0.515625, 0.718750, -0.765563, 0.000f, 0.000f, 0.000f, 0.031250f, 0.031250f, 0.499875f),
            part("IRON_BLOCK", -0.900000, 0.656250, -0.515563, 0.000f, 0.000f, 7.500f, 0.031250f, 0.156250f, 0.031125f),
            part("IRON_BLOCK", -0.796875, 0.656250, -0.515625, 0.000f, 0.000f, -7.500f, 0.031250f, 0.156250f, 0.031125f),
            part("IRON_BLOCK", -0.796875, 0.656250, -0.515563, 0.000f, 0.000f, 45.000f, 0.025000f, 0.078125f, 0.031125f),
            part("IRON_BLOCK", -0.893750, 0.656250, -0.515625, 0.000f, 0.000f, -45.000f, 0.025000f, 0.078125f, 0.031125f),
            part("IRON_BLOCK", -0.203125, 0.656250, -0.515563, 0.000f, 0.000f, 0.000f, 0.031250f, 0.156250f, 0.031125f),
            part("IRON_BLOCK", -0.265625, 0.656250, -0.515563, 0.000f, 0.000f, 0.000f, 0.062500f, 0.031250f, 0.031125f),
            part("IRON_BLOCK", -0.265625, 0.781250, -0.515563, 0.000f, 0.000f, 0.000f, 0.062500f, 0.031250f, 0.031125f),
            part("IRON_BLOCK", -0.515625, 0.656250, -0.796813, 0.000f, 0.000f, 0.000f, 0.031250f, 0.156250f, 0.031125f),
            part("IRON_BLOCK", -0.515625, 0.656250, -0.899938, 0.000f, 0.000f, 0.000f, 0.031250f, 0.156250f, 0.031125f),
            part("IRON_BLOCK", -0.515563, 0.647492, -0.850472, 0.000f, 145.000f, 0.000f, 0.031250f, 0.168750f, 0.031125f),
            part("IRON_BLOCK", -0.515625, 0.718750, -0.203063, 0.000f, 0.000f, 0.000f, 0.031250f, 0.093750f, 0.031125f),
            part("IRON_BLOCK", -0.515625, 0.656250, -0.265563, 0.000f, 0.000f, 0.000f, 0.031250f, 0.062500f, 0.031125f),
            part("IRON_BLOCK", -0.515625, 0.781250, -0.265563, 0.000f, 0.000f, 0.000f, 0.031250f, 0.031250f, 0.062375f),
            part("IRON_BLOCK", -0.515625, 0.656250, -0.234313, 0.000f, 0.000f, 0.000f, 0.031250f, 0.031250f, 0.062375f),
            part("IRON_BLOCK", -1.000000, 1.087500, -0.531250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.043750f, 0.062500f),
            part("IRON_BLOCK", -0.515625, 0.718750, -0.265563, 0.000f, 0.000f, 0.000f, 0.031250f, 0.031250f, 0.062375f),
            part("IRON_BLOCK", -0.234375, 0.718750, -0.515563, 0.000f, 0.000f, 0.000f, 0.031250f, 0.031250f, 0.031125f)
        ));
    }
    private static SmpFurnitureModel farm_windmill() {
        return model("farm_windmill", "Windmill", FurnitureDefinition.Category.OUTDOOR, "WIND_CHARGE", 220, 4.141119, 10.925000, List.of(
            part("STRIPPED_MANGROVE_WOOD", 0.406250, 0.028125, -2.000000, 0.412f, 6.991f, 7.234f, 0.156250f, 8.659375f, 0.156250f),
            part("STRIPPED_MANGROVE_WOOD", 0.390625, 0.040625, 0.375000, -0.412f, -6.991f, 7.234f, 0.156250f, 8.646875f, 0.156250f),
            part("STRIPPED_MANGROVE_WOOD", -1.984375, 0.034375, 0.375000, 0.412f, -6.991f, -7.234f, 0.156250f, 8.653125f, 0.156250f),
            part("STRIPPED_MANGROVE_WOOD", -1.984375, 0.028125, -2.000000, -0.412f, 6.991f, -7.234f, 0.156250f, 8.659375f, 0.156250f),
            part("NETHERITE_BLOCK", -0.875000, 8.250000, -0.859375, 0.000f, 0.000f, 0.000f, 0.312500f, 0.312500f, 0.500000f),
            part("NETHERITE_BLOCK", -0.796875, 8.296875, -1.296875, 0.000f, 0.000f, 0.000f, 0.156250f, 0.156250f, 0.437500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.921875, 9.078125, -1.360000, 0.000f, 0.000f, 0.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.109375, 9.078125, -1.359375, 0.000f, 0.000f, 20.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.296875, 8.953125, -1.360000, 0.000f, 0.000f, 40.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.484375, 8.765625, -1.359375, 0.000f, 0.000f, 60.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.593750, 8.578125, -1.360000, 0.000f, 0.000f, 80.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.593750, 8.390625, -1.359375, 0.000f, 0.000f, 100.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.531250, 8.203125, -1.360000, 0.000f, 0.000f, 120.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.406250, 8.015625, -1.359375, 0.000f, 0.000f, 140.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.218750, 7.890625, -1.360000, 0.000f, 0.000f, 160.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -1.031250, 7.828125, -1.359375, 0.000f, 0.000f, -180.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.859375, 9.078125, -1.359375, 0.000f, 0.000f, -25.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.671875, 8.953125, -1.360000, 0.000f, 0.000f, -45.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.484375, 8.765625, -1.359375, 0.000f, 0.000f, -65.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.375000, 8.578125, -1.360000, 0.000f, 0.000f, -85.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.375000, 8.390625, -1.359375, 0.000f, 0.000f, -105.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.437500, 8.203125, -1.360000, 0.000f, 0.000f, -125.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.562500, 8.015625, -1.359375, 0.000f, 0.000f, -145.000f, 0.406250f, 1.875000f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.750000, 7.890625, -1.360000, 0.000f, 0.000f, -165.000f, 0.406250f, 1.875000f, 0.062500f),
            part("NETHERITE_BLOCK", -1.437500, 8.343750, -1.328125, 0.000f, 0.000f, -45.000f, 1.218750f, 0.062500f, 0.031250f),
            part("NETHERITE_BLOCK", -1.359375, 8.343750, -1.328125, 0.000f, 0.000f, 45.000f, 1.218750f, 0.062500f, 0.031250f),
            part("NETHERITE_BLOCK", -0.750000, 8.375000, -0.484375, 0.000f, 15.000f, 0.000f, 0.062500f, 0.062500f, 0.750000f),
            part("NETHERITE_BLOCK", -0.750000, 8.375000, -0.484375, 0.000f, -15.000f, 0.000f, 0.062500f, 0.062500f, 0.750000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.750000, 8.421875, 0.265625, 0.000f, 0.000f, -20.000f, 0.062500f, 0.468750f, 1.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.750000, 7.953125, 0.265625, 0.000f, 0.000f, 20.000f, 0.062500f, 0.468750f, 1.125000f),
            part("STRIPPED_MANGROVE_WOOD", -1.391585, 4.213881, -1.451671, 1.019f, 6.929f, 0.000f, 1.375000f, 0.187500f, 0.125000f),
            part("STRIPPED_MANGROVE_WOOD", -1.579085, 1.963881, -1.451671, 1.019f, 6.929f, 0.000f, 1.750000f, 0.187500f, 0.125000f),
            part("STRIPPED_MANGROVE_WOOD", -2.580773, 4.223991, -1.468231, -5.125f, 4.779f, 55.224f, 2.562500f, 0.187500f, 0.112500f),
            part("STRIPPED_MANGROVE_WOOD", -1.868963, 3.128430, -1.584346, -6.230f, -3.205f, 125.613f, 2.562500f, 0.187500f, 0.112500f),
            part("STRIPPED_MANGROVE_WOOD", -1.868963, 3.159680, -0.021904, 5.232f, 3.199f, 125.557f, 2.562500f, 0.187500f, 0.112500f),
            part("STRIPPED_MANGROVE_WOOD", -2.612023, 4.270866, -0.113019, 5.125f, -4.779f, 55.224f, 2.593750f, 0.187500f, 0.103125f),
            part("STRIPPED_MANGROVE_WOOD", -1.391585, 4.260756, -0.142079, -1.019f, -6.929f, 0.000f, 1.375000f, 0.187500f, 0.125000f),
            part("STRIPPED_MANGROVE_WOOD", -1.579085, 1.995131, -0.167079, -0.026f, -6.928f, -0.121f, 1.750000f, 0.187500f, 0.150000f),
            part("STRIPPED_MANGROVE_WOOD", 0.390625, 1.984375, -1.609375, -0.412f, 0.000f, 7.234f, 0.156250f, 0.187500f, 1.750000f),
            part("STRIPPED_MANGROVE_WOOD", 0.103125, 4.234375, -1.359375, -0.412f, 0.000f, 7.234f, 0.156250f, 0.187500f, 1.250000f),
            part("STRIPPED_MANGROVE_WOOD", -0.144065, 4.210066, -2.750494, -0.412f, -56.000f, 7.234f, 0.150000f, 0.187500f, 2.687500f),
            part("STRIPPED_MANGROVE_WOOD", 0.137185, 2.022566, -2.625494, -0.412f, 56.000f, 7.234f, 0.150000f, 0.187500f, 2.687500f),
            part("STRIPPED_MANGROVE_WOOD", -1.965625, 1.984375, -1.609375, 0.412f, 0.000f, -7.234f, 0.156250f, 0.187500f, 1.750000f),
            part("STRIPPED_MANGROVE_WOOD", -1.705935, 2.022566, -2.625494, 0.412f, 56.000f, -7.234f, 0.150000f, 0.187500f, 2.687500f),
            part("STRIPPED_MANGROVE_WOOD", -1.424685, 4.210066, -2.750494, 0.412f, -56.000f, -7.234f, 0.150000f, 0.187500f, 2.687500f),
            part("STRIPPED_MANGROVE_WOOD", -1.678125, 4.234375, -1.359375, 0.412f, 0.000f, -7.234f, 0.156250f, 0.187500f, 1.250000f)
        ));
    }
    private static SmpFurnitureModel farm_window() {
        return model("farm_window", "Window", FurnitureDefinition.Category.OUTDOOR, "GLASS", 220, 1.000000, 1.000000, List.of(
            part("STRIPPED_PALE_OAK_WOOD", -1.000000, 0.000000, -1.062500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.109375f, 0.250000f),
            part("STRIPPED_PALE_OAK_WOOD", -1.000000, 0.890625, -1.062500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.109375f, 0.250000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.109375, 0.109375, -1.062500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.781250f, 0.250000f),
            part("STRIPPED_PALE_OAK_WOOD", -1.000000, 0.109375, -1.062500, 0.000f, 0.000f, 0.000f, 0.109375f, 0.781250f, 0.250000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.531250, 0.109375, -0.968750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.781250f, 0.062500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.890625, 0.468750, -0.969375, 0.000f, 0.000f, 0.000f, 0.781250f, 0.062500f, 0.062500f),
            part("GLASS", -0.890625, 0.109375, -0.954375, 0.000f, 0.000f, 0.000f, 0.781250f, 0.781250f, 0.033125f)
        ));
    }
    private static SmpFurnitureModel farm_woods() {
        return model("farm_woods", "Woods", FurnitureDefinition.Category.OUTDOOR, "OAK_WOOD", 220, 1.109375, 2.124375, List.of(
            part("OAK_LOG", -0.221875, -0.374375, -0.656250, -90.000f, 0.000f, -90.000f, 0.249375f, 0.999375f, 0.250000f),
            part("OAK_LOG", -0.625000, -0.374375, -0.625000, -90.000f, 0.000f, -90.000f, 0.250000f, 0.999375f, 0.250000f),
            part("OAK_LOG", -0.937500, -0.373750, -0.681250, -90.000f, 0.000f, -90.000f, 0.249375f, 0.997500f, 0.250000f),
            part("OAK_LOG", -0.265625, -0.031250, -0.625000, 0.000f, -90.000f, 0.000f, 0.234375f, 0.921875f, 0.250000f),
            part("OAK_LOG", -0.609375, -0.087500, -0.656250, 0.000f, -90.000f, 0.000f, 0.250000f, 0.993750f, 0.250000f),
            part("OAK_LOG", -0.921875, -0.078125, -0.671875, 0.000f, -90.000f, 0.000f, 0.234375f, 0.921875f, 0.250000f),
            part("OAK_LOG", -0.078125, 0.812500, -0.500000, 90.000f, 0.000f, 90.000f, 0.250000f, 0.937500f, 0.250000f),
            part("OAK_LOG", -0.617188, 0.273438, -0.812500, 75.000f, 90.000f, 180.000f, 0.250000f, 0.765625f, 0.250000f),
            part("OAK_LOG", -0.265625, 0.531250, -0.656250, 0.000f, -90.000f, 0.000f, 0.234375f, 0.796875f, 0.250000f),
            part("OAK_LOG", -0.609375, 0.468750, -0.656250, 0.000f, -90.000f, 0.000f, 0.250000f, 0.875000f, 0.250000f),
            part("OAK_LOG", -0.921875, 0.484375, -0.656250, 0.000f, -90.000f, 0.000f, 0.234375f, 0.859375f, 0.250000f)
        ));
    }
    private static SmpFurnitureModel halloween_basket() {
        return model("halloween_basket", "basket", FurnitureDefinition.Category.DECORATIONS, "BUCKET", 200, 0.438611, 0.562500, List.of(
            part("BODY", -0.671875, 0.000000, -0.671875, 0.000f, 0.000f, 0.000f, 0.343750f, 0.062500f, 0.343750f),
            part("BODY", -0.671875, 0.000625, -0.703125, 0.000f, 0.000f, 0.000f, 0.343750f, 0.312500f, 0.031250f),
            part("BODY", -0.671875, 0.000625, -0.328125, 0.000f, 0.000f, 0.000f, 0.343750f, 0.312500f, 0.031250f),
            part("BODY", -0.703125, 0.000000, -0.671875, 0.000f, 0.000f, 0.000f, 0.031250f, 0.312500f, 0.343750f),
            part("BODY", -0.328125, 0.000000, -0.671875, 0.000f, 0.000f, 0.000f, 0.031250f, 0.312500f, 0.343750f),
            part("INSIDE", -0.671875, 0.062500, -0.671875, 0.000f, 0.000f, 0.000f, 0.343750f, 0.250000f, 0.343750f),
            part("CANDY1", -0.578125, 0.249938, -0.500000, -42.500f, -90.000f, 90.000f, 0.187500f, 0.031375f, 0.125000f),
            part("SIDE1", -0.500031, 0.313642, -0.389514, 47.500f, 45.000f, 90.000f, 0.031250f, 0.125000f, 0.125000f),
            part("SIDE1", -0.499969, 0.313642, -0.389514, -47.500f, -135.000f, -90.000f, 0.031250f, 0.125000f, 0.125000f),
            part("CANDY4", -0.412500, 0.187500, -0.531250, 0.000f, 0.000f, 0.000f, 0.040625f, 0.187500f, 0.093750f),
            part("CANDY3", -0.640625, 0.125000, -0.453125, 0.000f, 0.000f, 0.000f, 0.078125f, 0.171875f, 0.093750f),
            part("SIDE2", -0.625031, 0.326142, -0.514514, -5.948f, -48.198f, 2.401f, 0.031250f, 0.125000f, 0.125000f),
            part("CANDY2", -0.703125, 0.262437, -0.625000, 3.181f, 174.043f, 92.069f, 0.187500f, 0.031375f, 0.125000f),
            part("SIDE2", -0.624969, 0.326142, -0.514514, 5.948f, -41.802f, -177.599f, 0.031250f, 0.125000f, 0.125000f),
            part("HANDLE", -0.343750, 0.312500, -0.509375, 0.000f, 0.000f, 0.000f, 0.015625f, 0.250000f, 0.015625f),
            part("HANDLE", -0.671875, 0.312500, -0.509375, 0.000f, 0.000f, 0.000f, 0.015625f, 0.250000f, 0.015625f),
            part("HANDLE", -0.656250, 0.546875, -0.509375, 0.000f, 0.000f, 0.000f, 0.312500f, 0.015625f, 0.015625f)
        ));
    }
    private static SmpFurnitureModel halloween_bones() {
        return model("halloween_bones", "bones", FurnitureDefinition.Category.DECORATIONS, "BONE", 200, 0.936118, 0.526539, List.of(
            part("BONE", -0.687500, 0.000000, -0.421875, -27.500f, 0.000f, 0.000f, 0.312500f, 0.063125f, 0.093750f),
            part("BONE", -0.746698, 0.105752, -0.406250, 72.500f, -90.000f, -180.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.746382, -0.121691, -0.406312, -17.500f, -90.000f, 180.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.746368, 0.106066, -0.406312, -72.500f, 90.000f, 0.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.746684, -0.121377, -0.406307, 17.500f, 90.000f, 0.000f, 0.140625f, 0.078125f, 0.062563f),
            part("BONE", -0.215118, 0.106066, -0.359437, 5.000f, 90.000f, 0.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.215434, -0.121377, -0.359432, 85.000f, -90.000f, -180.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.215133, -0.121691, -0.359437, -85.000f, 90.000f, 0.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.215448, 0.105752, -0.359375, -5.000f, -90.000f, -180.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.156250, 0.000000, -0.375000, 50.000f, 0.000f, 0.000f, 0.312500f, 0.063125f, 0.093750f),
            part("BONE", -0.234375, 0.000000, -0.750000, 75.000f, 0.000f, 0.000f, 0.312500f, 0.063125f, 0.093750f),
            part("BONE", -0.293573, 0.105752, -0.734375, -30.000f, -90.000f, 180.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.293258, -0.121691, -0.734437, -60.000f, 90.000f, 0.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.293559, -0.121377, -0.734433, 60.000f, -90.000f, -180.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.293243, 0.106066, -0.734437, 30.000f, 90.000f, 0.000f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.123080, 0.062500, -0.801244, -7.500f, -180.000f, -171.500f, 0.312500f, 0.063125f, 0.093750f),
            part("BONE", -0.415868, 0.280633, -0.769994, -37.500f, 90.000f, 8.500f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.481844, -0.167781, -0.770056, 52.500f, 90.000f, 8.500f, 0.140625f, 0.078125f, 0.062562f),
            part("BONE", -0.194883, -0.012788, -0.770051, -52.500f, -90.000f, -171.500f, 0.140625f, 0.078125f, 0.062563f),
            part("BONE", -0.260859, -0.006315, -0.770056, 37.500f, -90.000f, -171.500f, 0.140625f, 0.078125f, 0.062562f)
        ));
    }
    private static SmpFurnitureModel halloween_bottle() {
        return model("halloween_bottle", "bottle", FurnitureDefinition.Category.DECORATIONS, "EXPERIENCE_BOTTLE", 200, 0.312500, 0.437500, List.of(
            part("GLASS", -0.656250, 0.000000, -0.656250, 0.000f, 0.000f, 0.000f, 0.312500f, 0.375000f, 0.312500f),
            part("LIQUID", -0.650000, 0.006250, -0.650000, 0.000f, 0.000f, 0.000f, 0.300000f, 0.362500f, 0.300000f),
            part("LID", -0.625000, 0.375000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f),
            part("SKULL", -0.587500, 0.134375, -0.587500, 0.000f, 0.000f, 0.000f, 0.175000f, 0.140625f, 0.175000f),
            part("SKULL", -0.539062, 0.099654, -0.605301, 32.500f, 0.000f, 90.000f, 0.062500f, 0.143750f, 0.143750f),
            part("EYE", -0.478125, 0.196875, -0.588125, 0.000f, 0.000f, 0.000f, 0.034375f, 0.046875f, 0.010000f),
            part("EYE", -0.496875, 0.140625, -0.588125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.028125f, 0.010000f),
            part("EYE", -0.556250, 0.196875, -0.588125, 0.000f, 0.000f, 0.000f, 0.034375f, 0.046875f, 0.010000f),
            part("MOUTH", -0.550000, 0.140625, -0.587500, 0.000f, -32.500f, 0.000f, 0.100000f, 0.010000f, 0.087500f),
            part("EYE", -0.515625, 0.140625, -0.588125, 0.000f, 0.000f, 0.000f, 0.015625f, 0.028125f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel halloween_candle() {
        return model("halloween_candle", "candle", FurnitureDefinition.Category.DECORATIONS, "CYAN_CANDLE", 200, 1.343750, 1.203125, List.of(
            part("BODY", -0.625000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.031250f, 0.250000f),
            part("BODY", -0.546875, 0.031250, -0.546875, 0.000f, 0.000f, 0.000f, 0.093750f, 0.531250f, 0.093750f),
            part("DETAIL", -0.562500, 0.218750, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("BODY", -0.812500, 0.250000, -0.531250, 0.000f, 0.000f, -20.000f, 0.250000f, 0.062500f, 0.062500f),
            part("BODY", -0.922423, 0.335505, -0.531875, 0.000f, 0.000f, -90.000f, 0.125000f, 0.062500f, 0.062500f),
            part("BODY", -0.813048, 0.413630, -0.578750, 0.000f, 0.000f, -90.000f, 0.015625f, 0.156250f, 0.156250f),
            part("BODY", -0.546875, 0.515625, -0.578125, 0.000f, 0.000f, -90.000f, 0.015625f, 0.156250f, 0.156250f),
            part("BODY", -0.202577, 0.413630, -0.578750, 0.000f, 0.000f, 90.000f, 0.015625f, 0.156250f, 0.156250f),
            part("BODY", -0.202577, 0.335505, -0.531875, 0.000f, 0.000f, 90.000f, 0.125000f, 0.062500f, 0.062500f),
            part("BODY", -0.437500, 0.250000, -0.531250, 0.000f, 0.000f, 20.000f, 0.250000f, 0.062500f, 0.062500f),
            part("CANDLE", -0.906250, 0.578125, -0.906250, 0.000f, 0.000f, 0.000f, 0.812500f, 0.625000f, 0.812500f),
            part("FIRE", -0.525000, 0.812500, -0.525000, 0.000f, 0.000f, 0.000f, 0.050000f, 0.056250f, 0.050000f),
            part("FIRE", -0.790625, 0.709375, -0.525000, 0.000f, 0.000f, 0.000f, 0.050000f, 0.056250f, 0.050000f),
            part("CANDLE", -1.171875, 0.475000, -0.906250, 0.000f, 0.000f, 0.000f, 0.812500f, 0.625000f, 0.812500f),
            part("CANDLE", -0.640625, 0.475000, -0.906250, 0.000f, 0.000f, 0.000f, 0.812500f, 0.625000f, 0.812500f),
            part("FIRE", -0.259375, 0.709375, -0.525000, 0.000f, 0.000f, 0.000f, 0.050000f, 0.056250f, 0.050000f)
        ));
    }
    private static SmpFurnitureModel halloween_candy() {
        return model("halloween_candy", "candy", FurnitureDefinition.Category.DECORATIONS, "COOKIE", 200, 0.250000, 0.250000, List.of(
            part("BODY", -0.359375, 0.000000, -0.328125, 45.000f, 0.000f, 0.000f, 0.187500f, 0.031313f, 0.125000f),
            part("SIDES", -0.281219, 0.063642, -0.217639, 0.000f, 90.000f, -90.000f, 0.031250f, 0.125000f, 0.125000f),
            part("SIDES", -0.281281, 0.063642, -0.217639, 0.000f, 180.000f, 90.000f, 0.031250f, 0.125000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel halloween_cauldron() {
        return model("halloween_cauldron", "cauldron", FurnitureDefinition.Category.DECORATIONS, "CAULDRON", 200, 0.875000, 1.453125, List.of(
            part("NETHERITE_BLOCK", -0.812500, 0.343750, -0.900000, 0.000f, 0.000f, 0.000f, 0.625000f, 0.125000f, 0.093750f),
            part("NETHERITE_BLOCK", -0.259375, 0.093750, -0.906250, 0.000f, 0.000f, 0.000f, 0.165625f, 0.387500f, 0.146875f),
            part("NETHERITE_BLOCK", -0.290625, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.228125f, 0.106250f, 0.209375f),
            part("NETHERITE_BLOCK", -0.937500, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.228125f, 0.106250f, 0.209375f),
            part("NETHERITE_BLOCK", -0.906250, 0.093750, -0.906250, 0.000f, 0.000f, 0.000f, 0.165625f, 0.387500f, 0.146875f),
            part("NETHERITE_BLOCK", -0.906250, 0.093750, -0.240625, 0.000f, 0.000f, 0.000f, 0.165625f, 0.387500f, 0.146875f),
            part("NETHERITE_BLOCK", -0.937500, 0.000000, -0.271875, 0.000f, 0.000f, 0.000f, 0.228125f, 0.106250f, 0.209375f),
            part("NETHERITE_BLOCK", -0.290625, 0.000000, -0.271875, 0.000f, 0.000f, 0.000f, 0.228125f, 0.106250f, 0.209375f),
            part("NETHERITE_BLOCK", -0.259375, 0.093750, -0.240625, 0.000f, 0.000f, 0.000f, 0.165625f, 0.387500f, 0.146875f),
            part("NETHERITE_BLOCK", -0.812500, 0.343750, -0.193750, 0.000f, 0.000f, 0.000f, 0.625000f, 0.125000f, 0.093750f),
            part("NETHERITE_BLOCK", -0.900000, 0.343750, -0.812500, 0.000f, 0.000f, 0.000f, 0.093750f, 0.125000f, 0.625000f),
            part("NETHERITE_BLOCK", -0.193750, 0.343750, -0.812500, 0.000f, 0.000f, 0.000f, 0.093750f, 0.125000f, 0.625000f),
            part("CAULDRON", -0.837500, 0.343750, -0.837500, 0.000f, 0.000f, 0.000f, 0.675000f, 0.640625f, 0.675000f),
            part("SLIME_BLOCK", -0.818750, 0.931250, -0.801562, 0.000f, 0.000f, 0.000f, 0.637500f, 0.040625f, 0.637500f),
            part("CAULDRON", -0.162500, 0.909375, -0.629687, 0.000f, 0.000f, 0.000f, 0.100000f, 0.075000f, 0.256250f),
            part("CAULDRON", -0.937500, 0.909375, -0.629687, 0.000f, 0.000f, 0.000f, 0.100000f, 0.075000f, 0.256250f),
            part("SOUL_CAMPFIRE", -0.837500, 0.006250, -0.837500, 0.000f, 0.000f, 0.000f, 0.675000f, 0.262500f, 0.675000f),
            part("CRIMSON_FUNGUS", -0.306250, 0.875000, -0.618750, 45.000f, 90.000f, 0.000f, 0.187500f, 0.187500f, 0.062500f),
            part("AMETHYST_SHARD", -0.603125, 0.968750, -0.665625, 0.000f, 90.000f, 0.000f, 0.187500f, 0.187500f, 0.062500f),
            part("BONE", -0.509375, 0.968750, -0.431250, 0.000f, 90.000f, 0.000f, 0.187500f, 0.187500f, 0.062500f),
            part("SLIME_BLOCK", -0.759375, 1.156250, -0.493750, 0.000f, 90.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("SLIME_BLOCK", -0.525000, 1.359375, -0.681250, 0.000f, 90.000f, 0.000f, 0.093750f, 0.093750f, 0.093750f),
            part("SLIME_BLOCK", -0.353125, 1.125000, -0.853125, 0.000f, 90.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel halloween_chair() {
        return model("halloween_chair", "chair", FurnitureDefinition.Category.DECORATIONS, "WARPED_STAIRS", 200, 1.417337, 2.268780, List.of(
            part("BONE_BLOCK", -0.525706, 0.154410, -0.532111, 0.541f, -102.488f, -90.059f, 0.734375f, 0.094375f, 0.093750f),
            part("BONE_BLOCK", -0.062795, 0.225291, -0.541088, -9.182f, 8.518f, 134.315f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -0.049794, 0.086143, -0.541151, 8.408f, 9.283f, 45.684f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -0.493919, -0.393780, -0.545719, -8.408f, -9.283f, -134.316f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -0.480201, 0.606938, -0.545724, 9.182f, -8.518f, -45.685f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -1.208669, 0.154410, -0.532111, -0.541f, -102.488f, 90.059f, 0.734375f, 0.094375f, 0.093750f),
            part("BONE_BLOCK", -1.077830, 0.225291, -0.541088, 9.182f, 8.518f, -134.315f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -1.090831, 0.086143, -0.541151, -8.408f, 9.283f, -45.684f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -0.646706, -0.393780, -0.545719, 8.408f, -9.283f, 134.316f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -0.660424, 0.606938, -0.545724, -9.182f, -8.518f, 45.685f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -1.208669, 0.154410, -0.436639, 0.541f, 102.488f, 90.059f, 0.734375f, 0.094375f, 0.093750f),
            part("BONE_BLOCK", -1.077830, 0.225291, -0.427724, -9.182f, -8.518f, -134.315f, 0.140625f, 0.078125f, 0.093813f),
            part("BONE_BLOCK", -1.090831, 0.086143, -0.427662, 8.408f, -9.283f, -45.684f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -0.646706, -0.393780, -0.423093, -8.408f, 9.283f, 134.316f, 0.140625f, 0.078125f, 0.093813f),
            part("BONE_BLOCK", -0.660424, 0.606938, -0.423088, 9.182f, 8.518f, 45.685f, 0.140625f, 0.078125f, 0.093813f),
            part("BONE_BLOCK", -0.525706, 0.154410, -0.436639, -0.541f, 102.488f, -90.059f, 0.734375f, 0.094375f, 0.093750f),
            part("BONE_BLOCK", -0.062795, 0.225291, -0.427724, 9.182f, -8.518f, 134.315f, 0.140625f, 0.078125f, 0.093813f),
            part("BONE_BLOCK", -0.049794, 0.086143, -0.427662, -8.408f, -9.283f, 45.684f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE_BLOCK", -0.493919, -0.393780, -0.423093, 8.408f, 9.283f, -134.316f, 0.140625f, 0.078125f, 0.093813f),
            part("BONE_BLOCK", -0.480201, 0.606938, -0.423088, -9.182f, 8.518f, -45.685f, 0.140625f, 0.078125f, 0.093813f),
            part("SCULK", -0.921875, 0.500000, -0.812500, 0.000f, 0.000f, 0.000f, 0.843750f, 0.187500f, 0.734375f),
            part("SCULK_SENSOR", -0.890625, 0.500000, -0.203125, 0.000f, 12.500f, 0.000f, 0.781250f, 1.375000f, 0.125000f),
            part("SCULK_CATALYST", -0.953125, 0.937500, -0.812500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.750000f),
            part("SCULK_CATALYST", -0.234375, 0.937500, -0.812500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.750000f),
            part("SIT", -0.500000, 0.687500, -0.437500, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel halloween_cobweb() {
        return model("halloween_cobweb", "cobweb", FurnitureDefinition.Category.DECORATIONS, "COBWEB", 200, 1.000000, 1.468750, List.of(
            part("AIR", -0.968750, 0.453125, -0.568750, 0.000f, -90.000f, 0.000f, 0.953125f, 0.953125f, 1.000000f),
            part("WEB", -0.515625, 0.000000, -0.015650, 0.000f, 0.000f, 0.000f, 0.031250f, 1.000000f, 0.015625f),
            part("WEB", -0.999937, 0.484375, -0.015656, 0.000f, 0.000f, 0.000f, 0.999875f, 0.031250f, 0.015625f),
            part("WEB", -0.515625, -0.062500, -0.015644, 0.000f, 0.000f, -45.000f, 0.031250f, 1.125000f, 0.015625f),
            part("WEB", -0.515625, -0.062500, -0.015638, 0.000f, 0.000f, 45.000f, 0.031250f, 1.125000f, 0.015625f),
            part("WEB", -0.390625, 0.437500, -0.015625, 0.000f, 0.000f, 22.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.390625, 0.437500, -0.015625, 0.000f, 0.000f, -67.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.390625, 0.437500, -0.015625, 0.000f, 0.000f, -157.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.390625, 0.437500, -0.015625, 0.000f, 0.000f, 112.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.375000, 0.437500, -0.015631, 0.000f, 0.000f, 67.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.375000, 0.437500, -0.015631, 0.000f, 0.000f, -22.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.375000, 0.437500, -0.015631, 0.000f, 0.000f, -112.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.375000, 0.437500, -0.015631, 0.000f, 0.000f, 157.500f, 0.031250f, 0.125000f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015625, 0.000f, 0.000f, 112.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015631, 0.000f, 0.000f, 157.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015625, 0.000f, 0.000f, -157.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015631, 0.000f, 0.000f, -112.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015625, 0.000f, 0.000f, -67.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015631, 0.000f, 0.000f, -22.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015625, 0.000f, 0.000f, 22.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.265625, 0.406250, -0.015631, 0.000f, 0.000f, 67.500f, 0.031250f, 0.187500f, 0.015625f),
            part("WEB", -0.125000, 0.343750, -0.015631, 0.000f, 0.000f, 67.500f, 0.031250f, 0.312500f, 0.015625f),
            part("WEB", -0.140625, 0.343750, -0.015625, 0.000f, 0.000f, 112.500f, 0.031250f, 0.312500f, 0.015625f),
            part("WEB", -0.125000, 0.343750, -0.015631, 0.000f, 0.000f, 157.500f, 0.031250f, 0.312500f, 0.015625f),
            part("WEB", -0.140625, 0.343750, -0.015625, 0.000f, 0.000f, -157.500f, 0.031250f, 0.312500f, 0.015625f),
            part("WEB", -0.125000, 0.343750, -0.015631, 0.000f, 0.000f, -112.500f, 0.031250f, 0.312500f, 0.015625f),
            part("WEB", -0.140625, 0.343750, -0.015625, 0.000f, 0.000f, -67.500f, 0.031250f, 0.312500f, 0.015625f),
            part("WEB", -0.125000, 0.343750, -0.015631, 0.000f, 0.000f, -22.500f, 0.031250f, 0.312500f, 0.015625f),
            part("WEB", -0.140625, 0.343750, -0.015625, 0.000f, 0.000f, 22.500f, 0.031250f, 0.312500f, 0.015625f)
        ));
    }
    private static SmpFurnitureModel halloween_coffin() {
        return model("halloween_coffin", "coffin", FurnitureDefinition.Category.DECORATIONS, "STRIPPED_PALE_OAK_WOOD", 200, 1.886852, 0.507000, List.of(
            part("FRAME", -0.071231, 0.000000, -0.937500, 12.500f, 0.000f, 0.000f, 0.125000f, 0.506250f, 1.250000f),
            part("FRAME", -1.050000, 0.000000, -0.937500, -12.500f, 0.000f, 0.000f, 0.118750f, 0.506250f, 1.250000f),
            part("FRAME", -0.071231, 0.000625, 0.324352, -12.500f, 0.000f, 0.000f, 0.125000f, 0.505000f, 0.625000f),
            part("FRAME", -1.050000, 0.000625, 0.324352, 12.500f, 0.000f, 0.000f, 0.125000f, 0.505000f, 0.625000f),
            part("FRAME", -0.728125, 0.000625, -0.900000, 0.000f, 0.000f, 0.000f, 0.456250f, 0.505000f, 0.118750f),
            part("FILL", -0.878125, 0.000063, -0.140625, 0.000f, 0.000f, 0.000f, 0.756250f, 0.493750f, 0.953125f),
            part("FILL", -0.796875, 0.000125, -0.453125, 0.000f, 0.000f, 0.000f, 0.593750f, 0.493750f, 0.312500f),
            part("FRAME", -0.860616, 0.000000, 0.803611, 0.000f, 0.000f, 0.000f, 0.725000f, 0.506250f, 0.125000f),
            part("FILL", -0.734375, 0.000063, -0.781250, 0.000f, 0.000f, 0.000f, 0.468750f, 0.493750f, 0.328125f),
            part("CROSS", -0.515625, 0.500125, -0.140625, 0.000f, 0.000f, 0.000f, 0.062500f, 0.010000f, 0.750000f),
            part("CROSS", -0.703125, 0.500125, 0.296875, 0.000f, 0.000f, 0.000f, 0.437500f, 0.010000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel halloween_coffin_stand() {
        return model("halloween_coffin_stand", "coffin_stand", FurnitureDefinition.Category.DECORATIONS, "PALE_OAK_WOOD", 200, 4.151084, 1.659111, List.of(
            part("FRAME", -0.409339, -0.000125, -0.213449, 0.000f, -90.000f, -12.500f, 0.150000f, 0.457500f, 1.500000f),
            part("FRAME", -0.943806, -0.000125, -0.089434, 0.000f, -90.000f, 12.500f, 0.150000f, 0.457500f, 1.500000f),
            part("FRAME", 0.226824, 0.000625, 1.145501, 0.000f, -90.000f, 12.500f, 0.150000f, 0.456000f, 0.750000f),
            part("FRAME", -1.579969, 0.000625, 1.021486, 0.000f, -90.000f, -12.500f, 0.150000f, 0.456000f, 0.750000f),
            part("FRAME", -0.954375, 0.000625, -0.218750, 0.000f, -90.000f, 0.000f, 0.695000f, 0.456000f, 0.150000f),
            part("FILL", -1.141875, 0.007450, 0.700000, 0.000f, -90.000f, 0.000f, 1.062500f, 0.085000f, 1.143750f),
            part("FILL", -1.029375, 0.007525, 0.325075, 0.000f, -90.000f, 0.000f, 0.837500f, 0.085000f, 0.375000f),
            part("FRAME", -1.120864, -0.000125, 1.833084, 0.000f, -90.000f, 0.000f, 1.025000f, 0.457500f, 0.150000f),
            part("FILL", -0.954375, 0.007450, -0.068750, 0.000f, -90.000f, 0.000f, 0.687500f, 0.085000f, 0.393750f),
            part("CROSS", -0.566875, 1.620386, -1.218861, -75.000f, 90.000f, 180.000f, 0.075000f, 0.010000f, 0.900000f),
            part("CROSS", -0.791875, 1.620386, -0.693861, -75.000f, 90.000f, 180.000f, 0.525000f, 0.010000f, 0.075000f),
            part("FILL", -1.029447, 1.552886, -1.593881, -75.000f, 90.000f, 180.000f, 0.837500f, 0.060000f, 0.375000f),
            part("FILL", -0.954375, 1.552811, -1.987611, -75.000f, 90.000f, 180.000f, 0.687500f, 0.060000f, 0.393750f),
            part("FILL", -1.141875, 1.552811, -1.218861, -75.000f, 90.000f, 180.000f, 1.062500f, 0.060000f, 1.143750f),
            part("FRAME", -0.954375, 1.470986, -2.137611, -75.000f, 90.000f, 180.000f, 0.695000f, 0.156000f, 0.150000f),
            part("FRAME", -1.129164, 1.501486, -2.027935, -70.567f, 128.930f, 139.418f, 0.150000f, 0.157500f, 1.500000f),
            part("FRAME", -1.129164, 1.502236, -1.058065, -70.567f, 51.070f, -139.418f, 0.150000f, 0.156000f, 0.750000f),
            part("FRAME", -1.120864, 1.470236, -0.085778, -75.000f, 90.000f, 180.000f, 1.025000f, 0.157500f, 0.150000f),
            part("FRAME", -0.083534, 1.470986, -0.668000, -70.567f, 128.930f, 139.418f, 0.150000f, 0.156000f, 0.750000f),
            part("FRAME", -0.083534, 1.470236, -2.168000, -70.567f, 51.070f, -139.418f, 0.150000f, 0.157500f, 1.500000f)
        ));
    }
    private static SmpFurnitureModel halloween_cutter() {
        return model("halloween_cutter", "cutter", FurnitureDefinition.Category.DECORATIONS, "STONECUTTER", 200, 1.287625, 2.081250, List.of(
            part("STONECUTTER", -0.875000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.750000f, 0.750000f),
            part("SPRUCE_WOOD", 0.268750, 0.812500, -0.568750, 0.000f, 0.000f, 0.000f, 0.143750f, 1.125000f, 0.137500f),
            part("CHAIN", -0.718750, 1.156250, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.375000f, 0.500000f),
            part("PLAYER_HEAD", -0.843750, 0.781250, -0.875000, -180.000f, 0.000f, 0.000f, 0.750000f, 0.750000f, 0.750000f),
            part("SPRUCE_WOOD", -0.187500, 1.187500, -0.562500, 0.000f, 0.000f, 90.000f, 0.125000f, 0.812500f, 0.125000f),
            part("FIRE_CORAL_FAN", -0.687500, 0.562500, -0.718750, 0.000f, 0.000f, 180.000f, 0.437500f, 0.250000f, 0.437500f),
            part("SPRUCE_WOOD", 0.268813, -0.081250, -0.568750, 0.000f, 30.000f, 0.000f, 0.143750f, 0.893750f, 0.137500f),
            part("SPRUCE_WOOD", 0.268875, -0.081250, -0.568750, 0.000f, -30.000f, 0.000f, 0.143750f, 0.893750f, 0.137500f),
            part("SPRUCE_WOOD", 0.000000, 0.937500, -0.556250, 0.000f, 0.000f, 45.000f, 0.125000f, 0.750000f, 0.112500f),
            part("FIRE_CORAL_FAN", -0.765625, 0.196875, -0.771875, 0.000f, 0.000f, 180.000f, 0.543750f, 0.250000f, 0.543750f)
        ));
    }
    private static SmpFurnitureModel halloween_eye() {
        return model("halloween_eye", "eye", FurnitureDefinition.Category.DECORATIONS, "ENDER_EYE", 200, 1.384375, 0.908125, List.of(
            part("BODY", -0.812500, 0.062500, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.625000f, 0.625000f),
            part("BODY", -0.750000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.750000f, 0.500000f),
            part("BODY", -0.750000, 0.125000, -0.875000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.750000f),
            part("EYE", -0.506250, 0.415625, -0.890625, 0.000f, 0.000f, 0.000f, 0.406250f, 0.406250f, 0.062500f),
            part("BACK", -0.741875, 0.142500, -0.143750, 0.000f, 90.000f, 0.000f, 0.500000f, 0.500000f, 0.375000f),
            part("MIDDLE", -0.718750, 0.296875, -1.153125, 0.000f, -90.000f, 0.000f, 0.406250f, 0.187500f, 0.406250f),
            part("BODY", -0.875000, 0.125000, -0.750000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.500000f, 0.500000f),
            part("SIDE", -0.116875, 0.376875, -0.487500, 0.000f, 90.000f, 90.000f, 0.484375f, 0.531250f, 0.125000f),
            part("SIDE", -0.882500, 0.376875, -0.487500, 0.000f, 90.000f, 90.000f, 0.484375f, 0.531250f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel halloween_fence() {
        return model("halloween_fence", "fence", FurnitureDefinition.Category.DECORATIONS, "TUFF_BRICK_WALL", 200, 1.999937, 1.484375, List.of(
            part("TUFF_BRICKS", -1.125000, 0.515625, -0.656250, 0.000f, 0.000f, 0.000f, 0.250000f, 0.859375f, 0.312500f),
            part("TUFF_BRICKS", -1.999937, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 1.999937f, 0.515625f, 0.250000f),
            part("POLISHED_TUFF", -1.999937, 0.859375, -0.546875, 0.000f, 0.000f, 0.000f, 1.999937f, 0.015625f, 0.093750f),
            part("NETHERITE_BLOCK", -1.187500, 1.375000, -0.718750, 0.000f, 0.000f, 0.000f, 0.375000f, 0.109375f, 0.437500f),
            part("NETHERITE_BLOCK", -1.375000, 0.515625, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.734375f, 0.062500f),
            part("NETHERITE_BLOCK", -1.812500, 0.515625, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.734375f, 0.062500f),
            part("NETHERITE_BLOCK", -0.687500, 0.515625, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.734375f, 0.062500f),
            part("NETHERITE_BLOCK", -0.250000, 0.515625, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.734375f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel halloween_fence_corner() {
        return model("halloween_fence_corner", "fence_corner", FurnitureDefinition.Category.DECORATIONS, "POLISHED_TUFF_WALL", 200, 0.671812, 1.484375, List.of(
            part("TUFF_BRICKS", -0.640625, 0.515625, -0.625000, 0.000f, 0.000f, 0.000f, 0.265625f, 0.859375f, 0.265625f),
            part("TUFF_BRICKS", -0.625000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.624937f, 0.515625f, 0.250000f),
            part("POLISHED_TUFF", -0.500062, 0.859375, -0.546875, 0.000f, 0.000f, 0.000f, 0.500000f, 0.015625f, 0.093750f),
            part("NETHERITE_BLOCK", -0.671875, 1.375000, -0.656250, 0.000f, 0.000f, 0.000f, 0.328125f, 0.109375f, 0.328125f),
            part("NETHERITE_BLOCK", -0.531250, 0.515625, -0.843750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.734375f, 0.062500f),
            part("NETHERITE_BLOCK", -0.218750, 0.515625, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 0.734375f, 0.062500f),
            part("TUFF_BRICKS", -0.625000, 0.000000, -0.999937, 0.000f, 0.000f, 0.000f, 0.250000f, 0.515625f, 0.374937f),
            part("POLISHED_TUFF", -0.546875, 0.859375, -0.999937, 0.000f, 0.000f, 0.000f, 0.093750f, 0.015625f, 0.390619f)
        ));
    }
    private static SmpFurnitureModel halloween_gravestone_v1() {
        return model("halloween_gravestone_v1", "gravestone_v1", FurnitureDefinition.Category.DECORATIONS, "MOSSY_STONE_BRICKS", 200, 0.750000, 0.937500, List.of(
            part("BODY", -0.875000, 0.000000, -0.437500, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.375000f),
            part("BODY", -0.750000, 0.125000, -0.375000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.687500f, 0.250000f),
            part("BODY", -0.656250, 0.812500, -0.343750, 0.000f, 0.000f, 0.000f, 0.312500f, 0.125000f, 0.187500f),
            part("TEXT", -0.359375, 0.375000, -0.381250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.250000f, 0.010000f),
            part("TEXT", -0.515625, 0.375000, -0.381250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.250000f, 0.010000f),
            part("TEXT", -0.593750, 0.375000, -0.381250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.250000f, 0.010000f),
            part("TEXT", -0.406250, 0.593750, -0.381250, 0.000f, 0.000f, 0.000f, 0.046875f, 0.031250f, 0.010000f),
            part("TEXT", -0.406250, 0.484375, -0.381250, 0.000f, 0.000f, 0.000f, 0.046875f, 0.031250f, 0.010000f),
            part("TEXT", -0.437500, 0.484375, -0.381250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.140625f, 0.010000f),
            part("TEXT", -0.409375, 0.368750, -0.381250, 0.000f, 0.000f, -40.000f, 0.031250f, 0.146875f, 0.010000f),
            part("TEXT", -0.640625, 0.484375, -0.381250, 0.000f, 0.000f, 0.000f, 0.046875f, 0.031250f, 0.010000f),
            part("TEXT", -0.671875, 0.484375, -0.381250, 0.000f, 0.000f, 0.000f, 0.031250f, 0.140625f, 0.010000f),
            part("TEXT", -0.640625, 0.593750, -0.381250, 0.000f, 0.000f, 0.000f, 0.046875f, 0.031250f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel halloween_gravestone_v2() {
        return model("halloween_gravestone_v2", "gravestone_v2", FurnitureDefinition.Category.DECORATIONS, "POLISHED_BASALT", 200, 1.046875, 1.103125, List.of(
            part("GROUND", -0.875000, 0.000000, -0.468750, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.437500f),
            part("GROUND", -0.562500, 0.125000, -0.438750, 0.000f, 0.000f, -17.500f, 0.453125f, 0.125000f, 0.375000f),
            part("GROUND", -0.890625, 0.125000, -0.438125, 0.000f, 0.000f, 17.500f, 0.453125f, 0.125000f, 0.375000f),
            part("GROUND", -0.687500, 0.187500, -0.437500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.087500f, 0.375000f),
            part("CROSS", -0.625000, 0.275000, -0.359375, 0.000f, 0.000f, 0.000f, 0.250000f, 0.775000f, 0.218750f),
            part("CROSS", -1.156250, 0.353125, -0.358750, 0.000f, 0.000f, -90.000f, 0.181250f, 0.750000f, 0.217500f)
        ));
    }
    private static SmpFurnitureModel halloween_gravestone_v3() {
        return model("halloween_gravestone_v3", "gravestone_v3", FurnitureDefinition.Category.DECORATIONS, "CHISELED_TUFF_BRICKS", 200, 0.875000, 1.250000, List.of(
            part("BODY", -0.937500, 0.000000, -0.375000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.750000f, 0.250000f),
            part("BODY", -0.875000, 0.812500, -0.062500, 0.000f, -90.000f, 0.000f, 0.750000f, 0.250000f, 0.125000f),
            part("BODY", -0.812500, 0.937500, -0.062500, 0.000f, -90.000f, 0.000f, 0.625000f, 0.250000f, 0.062500f),
            part("BODY", -0.687500, 1.000000, -0.062500, 0.000f, -90.000f, 0.000f, 0.375000f, 0.250000f, 0.062500f),
            part("ICON", -0.500000, 0.468750, -0.387500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.375000f, 0.012500f)
        ));
    }
    private static SmpFurnitureModel halloween_hand() {
        return model("halloween_hand", "hand", FurnitureDefinition.Category.DECORATIONS, "GREEN_CANDLE", 200, 0.500000, 0.857804, List.of(
            part("GROUND", -0.750000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.500000f),
            part("GROUND", -0.687500, 0.000000, -0.687500, 0.000f, -7.500f, -7.500f, 0.375000f, 0.125000f, 0.375000f),
            part("ARM1", -0.625000, 0.062500, -0.593750, 0.000f, 0.000f, 0.000f, 0.246875f, 0.187500f, 0.187500f),
            part("ARM1", -0.609375, 0.181250, -0.578125, 2.154f, -9.768f, 12.316f, 0.218750f, 0.256250f, 0.156250f),
            part("ARM", -0.535025, 0.523460, -0.595999, -1.728f, -9.851f, -9.851f, 0.062500f, 0.234375f, 0.062500f),
            part("ARM", -0.594519, 0.549268, -0.593938, 2.154f, 2.732f, 12.316f, 0.062500f, 0.212500f, 0.062500f),
            part("ARM", -0.676265, 0.525751, -0.602809, 4.209f, -9.079f, 24.666f, 0.062500f, 0.203125f, 0.062500f),
            part("ARM", -0.538609, 0.502514, -0.571653, 4.209f, -9.079f, 24.666f, 0.203125f, 0.062500f, 0.062500f),
            part("ARM", -0.634643, 0.413666, -0.571129, 6.409f, -17.693f, 39.569f, 0.062500f, 0.250000f, 0.062500f),
            part("ARM", -0.593750, 0.375000, -0.562500, 2.154f, -9.768f, 12.316f, 0.187500f, 0.250000f, 0.125000f),
            part("ARM", -0.397984, 0.502514, -0.634153, 4.209f, -9.079f, 24.666f, 0.062500f, 0.062500f, 0.062500f),
            part("ARM", -0.516988, 0.723452, -0.676268, -1.728f, 7.649f, -9.851f, 0.062500f, 0.062500f, 0.118750f),
            part("ARM", -0.623506, 0.717179, -0.585475, 2.154f, -22.268f, 12.316f, 0.062500f, 0.140625f, 0.062500f),
            part("ARM", -0.708900, 0.677253, -0.663517, 4.209f, 45.921f, 24.666f, 0.062500f, 0.062500f, 0.109375f),
            part("ARM", -0.720756, 0.584141, -0.674709, 6.409f, 12.307f, 39.569f, 0.062500f, 0.062500f, 0.109375f)
        ));
    }
    private static SmpFurnitureModel halloween_pumpkin() {
        return model("halloween_pumpkin", "pumpkin", FurnitureDefinition.Category.DECORATIONS, "CARVED_PUMPKIN", 200, 0.763750, 1.000000, List.of(
            part("BODY", -0.812500, 0.062500, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.625000f, 0.625000f),
            part("BODY", -0.750000, 0.125000, -0.875000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.750000f),
            part("BODY", -0.750000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.750000f, 0.500000f),
            part("BODY", -0.875000, 0.125000, -0.750000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.500000f, 0.500000f),
            part("FACE", -0.718750, 0.468750, -0.887500, 0.000f, 0.000f, -40.000f, 0.187500f, 0.062500f, 0.012500f),
            part("FACE", -0.468750, 0.468750, -0.887500, 0.000f, 0.000f, 40.000f, 0.187500f, 0.062500f, 0.012500f),
            part("FACE", -0.723173, 0.261298, -0.888750, 0.000f, 0.000f, -30.000f, 0.150000f, 0.062500f, 0.012500f),
            part("FACE", -0.426827, 0.261298, -0.888125, 0.000f, 0.000f, 30.000f, 0.150000f, 0.062500f, 0.012500f),
            part("FACE", -0.593750, 0.187500, -0.887500, 0.000f, 0.000f, 0.000f, 0.187500f, 0.062500f, 0.012500f),
            part("FACE", -0.475000, 0.156250, -0.888750, 0.000f, 0.000f, 45.000f, 0.062500f, 0.062500f, 0.012500f),
            part("FACE", -0.581250, 0.156250, -0.888125, 0.000f, 0.000f, 45.000f, 0.062500f, 0.062500f, 0.012500f),
            part("TOP", -0.562500, 0.750000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("TOP", -0.531250, 0.812500, -0.531250, 0.000f, 0.000f, 35.000f, 0.062500f, 0.187500f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel halloween_pumpkin_small() {
        return model("halloween_pumpkin_small", "pumpkin_small", FurnitureDefinition.Category.DECORATIONS, "PUMPKIN", 200, 0.381250, 0.437500, List.of(
            part("BODY", -0.406250, 0.031250, -0.404375, 0.000f, 0.000f, 0.000f, 0.312500f, 0.312500f, 0.312500f),
            part("BODY", -0.375000, 0.062500, -0.435625, 0.000f, 0.000f, 0.000f, 0.250000f, 0.250000f, 0.375000f),
            part("BODY", -0.375000, 0.000000, -0.373125, 0.000f, 0.000f, 0.000f, 0.250000f, 0.375000f, 0.250000f),
            part("BODY", -0.437500, 0.062500, -0.373125, 0.000f, 0.000f, 0.000f, 0.375000f, 0.250000f, 0.250000f),
            part("FACE", -0.353125, 0.221875, -0.429375, -11.954f, 12.121f, 44.201f, 0.078125f, 0.078125f, 0.010000f),
            part("FACE", -0.353333, 0.131250, -0.441875, 0.000f, 0.000f, -30.000f, 0.075000f, 0.031250f, 0.010000f),
            part("FACE", -0.221667, 0.130649, -0.441812, 0.000f, 0.000f, 30.000f, 0.075000f, 0.031250f, 0.010000f),
            part("FACE", -0.273878, 0.093750, -0.441875, 0.000f, 0.000f, -30.000f, 0.062500f, 0.031250f, 0.010000f),
            part("TOP", -0.281250, 0.375000, -0.279375, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("FACE", -0.240625, 0.221875, -0.429375, -11.954f, 12.121f, 44.201f, 0.078125f, 0.078125f, 0.010000f),
            part("FACE", -0.296755, 0.125000, -0.441812, 0.000f, 0.000f, 30.000f, 0.062500f, 0.031250f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel halloween_skeleton_v1() {
        return model("halloween_skeleton_v1", "skeleton_v1", FurnitureDefinition.Category.DECORATIONS, "CHAIN", 200, 1.312500, 1.776047, List.of(
            part("DETAIL", -0.125000, 1.937500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.062500f),
            part("DETAIL", -1.000000, 1.937500, -0.062500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.062500f),
            part("CHAIN", -1.156250, 1.562500, -0.156250, 0.000f, 0.000f, 15.000f, 0.437500f, 0.375000f, 0.250000f),
            part("CHAIN", -0.281250, 1.562500, -0.156250, 0.000f, 0.000f, -15.000f, 0.437500f, 0.375000f, 0.250000f),
            part("BONE", -0.191872, 1.129690, -0.062500, 0.000f, 7.500f, -15.000f, 0.062500f, 0.437500f, 0.062500f),
            part("BONE", -0.870628, 1.129690, -0.062500, 0.000f, 7.500f, 15.000f, 0.062500f, 0.437500f, 0.062500f),
            part("DETAIL", -0.876878, 1.442190, -0.068750, 0.000f, 7.500f, 15.000f, 0.075000f, 0.062500f, 0.075000f),
            part("DETAIL", -0.198122, 1.442190, -0.068750, 0.000f, 7.500f, -15.000f, 0.075000f, 0.062500f, 0.075000f),
            part("HEAD", -0.875000, 1.187500, -0.643750, 0.000f, -150.000f, 180.000f, 0.750000f, 0.687500f, 0.500000f),
            part("BONE", -0.531250, 0.750000, -0.125625, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.093750f),
            part("BONE", -0.718750, 0.687500, -0.218750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.218750f),
            part("BONE", -0.718750, 1.000000, -0.093750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.062500f),
            part("BONE", -0.718750, 0.843750, -0.093750, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.062500f),
            part("BONE", -0.328125, 1.000000, -0.218750, 0.000f, 0.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.718750, 1.000000, -0.218750, 0.000f, 0.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.718750, 0.843750, -0.218750, 0.000f, 0.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.328125, 0.843750, -0.218750, 0.000f, 0.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.671875, 0.843750, -0.218750, 0.000f, 0.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -0.671875, 1.000000, -0.218750, 0.000f, 0.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -0.718750, 1.156250, -0.125000, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.093750f),
            part("BONE", -0.421875, 1.000000, -0.218750, 0.000f, 0.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -0.421875, 0.843750, -0.218750, 0.000f, 0.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -0.437500, 0.437500, -0.156875, 0.000f, 0.000f, 20.000f, 0.062500f, 0.250000f, 0.093750f),
            part("BONE", -0.355764, 0.223953, -0.156875, 0.000f, 0.000f, -7.500f, 0.062500f, 0.250000f, 0.093750f),
            part("BONE", -0.706736, 0.223953, -0.156875, 0.000f, 0.000f, 7.500f, 0.062500f, 0.250000f, 0.093750f),
            part("BONE", -0.625000, 0.437500, -0.156875, 0.000f, 0.000f, -20.000f, 0.062500f, 0.250000f, 0.093750f),
            part("BONE", -0.531250, 1.218750, -0.125625, 0.000f, -30.000f, 0.000f, 0.062500f, 0.156250f, 0.056250f)
        ));
    }
    private static SmpFurnitureModel halloween_skeleton_v2() {
        return model("halloween_skeleton_v2", "skeleton_v2", FurnitureDefinition.Category.DECORATIONS, "SKELETON_SKULL", 200, 1.106872, 1.535702, List.of(
            part("BONE", -1.034226, -0.315692, -0.296875, -11.001f, -10.261f, -46.509f, 0.062500f, 0.484375f, 0.062500f),
            part("HEAD", -0.875000, 0.470009, -0.899288, 0.000f, -162.500f, 180.000f, 0.750000f, 0.750000f, 0.750000f),
            part("BONE", -0.531250, 0.000000, -0.313125, 0.000f, -15.000f, 0.000f, 0.062500f, 0.687500f, 0.093750f),
            part("BONE", -0.718750, 0.312500, -0.281250, 0.000f, -15.000f, 0.000f, 0.437500f, 0.062500f, 0.062500f),
            part("BONE", -0.718750, 0.156250, -0.281250, 0.000f, -15.000f, 0.000f, 0.437500f, 0.062500f, 0.062500f),
            part("BONE", -0.328125, 0.312500, -0.406250, 0.000f, -15.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.718750, 0.312500, -0.406250, 0.000f, -15.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.718750, 0.156250, -0.406250, 0.000f, -15.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.328125, 0.156250, -0.406250, 0.000f, -15.000f, 0.000f, 0.046875f, 0.062500f, 0.125000f),
            part("BONE", -0.671875, 0.156250, -0.406250, 0.000f, -15.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -0.671875, 0.312500, -0.406250, 0.000f, -15.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -0.718750, 0.468750, -0.312500, 0.000f, -15.000f, 0.000f, 0.437500f, 0.062500f, 0.093750f),
            part("BONE", -0.421875, 0.312500, -0.406250, 0.000f, -15.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -0.421875, 0.156250, -0.406250, 0.000f, -15.000f, 0.000f, 0.093750f, 0.062500f, 0.046875f),
            part("BONE", -1.092152, 0.162707, -0.862450, -40.691f, 13.729f, -43.327f, 0.062500f, 0.062500f, 0.562500f),
            part("BONE", -0.315810, -0.011895, -0.422903, -5.609f, 41.757f, 37.362f, 0.062500f, 0.484375f, 0.062500f),
            part("BONE", -0.068376, 0.227611, -1.256160, 30.861f, 15.087f, 62.232f, 0.062500f, 0.062500f, 0.562500f)
        ));
    }
    private static SmpFurnitureModel halloween_table() {
        return model("halloween_table", "table", FurnitureDefinition.Category.DECORATIONS, "SCAFFOLDING", 200, 0.999875, 1.000000, List.of(
            part("TOP", -0.999937, 0.812500, -0.999937, 0.000f, 0.000f, 0.000f, 0.999875f, 0.187500f, 0.999875f),
            part("BONE", -0.403838, 0.591908, -0.350464, 0.059f, -12.488f, -89.459f, 0.187500f, 0.094375f, 0.093750f),
            part("BONE", -0.420912, 0.723498, -0.356712, 43.602f, -72.626f, 102.730f, 0.140625f, 0.078125f, 0.093813f),
            part("BONE", -0.416743, 0.584277, -0.356774, -43.718f, -72.592f, 78.328f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE", -0.463039, 0.479947, -0.356769, 43.718f, 72.592f, -101.672f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE", -0.462724, 0.707307, -0.356774, -43.602f, 72.626f, -77.270f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE", -0.400706, 0.210660, -0.344611, 0.541f, -102.488f, -90.059f, 0.250000f, 0.094375f, 0.093750f),
            part("BONE", -0.422170, 0.281541, -0.353588, -9.182f, 8.518f, 134.315f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE", -0.409169, 0.142393, -0.353651, 8.408f, 9.283f, 45.684f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE", -0.510451, 0.004606, -0.353646, -8.408f, -9.283f, -134.316f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE", -0.497441, 0.320344, -0.353651, 9.182f, -8.518f, -45.685f, 0.140625f, 0.078125f, 0.093812f),
            part("BONE", -0.625000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.250000f)
        ));
    }
    private static SmpFurnitureModel halloween_whisk() {
        return model("halloween_whisk", "whisk", FurnitureDefinition.Category.DECORATIONS, "BRUSH", 200, 0.375000, 1.750000, List.of(
            part("WHISK", -0.687500, 0.000000, -0.562500, 0.000f, 10.000f, 0.000f, 0.375000f, 0.375000f, 0.375000f),
            part("WHISK", -0.625000, 0.375000, -0.500000, 0.000f, 10.000f, 0.000f, 0.250000f, 0.312500f, 0.250000f),
            part("STICK", -0.562500, 0.687500, -0.437500, 0.000f, 10.000f, 0.000f, 0.125000f, 1.062500f, 0.125000f),
            part("WHISK", -0.656250, 0.015625, -0.531250, 0.000f, 10.000f, 0.000f, 0.312500f, 0.484375f, 0.312500f),
            part("SIT", -0.500000, 0.812500, -0.500000, 0.000f, 10.000f, 0.000f, 0.010000f, 0.010000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel modern_big_bed() {
        return model("modern_big_bed", "Big Bed", FurnitureDefinition.Category.FURNITURE, "STONE_BRICK_STAIRS", 200, 4.456859, 1.725000, List.of(
            part("WHITE_CONCRETE", -1.824531, 0.312500, -3.326250, 0.000f, 0.000f, 0.000f, 2.736875f, 0.187500f, 4.152500f),
            part("BLACK_CONCRETE", -1.730156, 0.000000, -3.326250, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("BLACK_CONCRETE", -1.730156, 0.000000, 0.637500, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("BLACK_CONCRETE", 0.629219, 0.000000, -3.326250, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("BLACK_CONCRETE", 0.629219, 0.000000, 0.637500, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("WHITE_CONCRETE", -1.824531, 0.312500, -3.439500, 0.000f, 0.000f, 0.000f, 2.736875f, 0.412500f, 0.113250f),
            part("WHITE_CONCRETE", -1.824531, 0.312500, 0.826250, 0.000f, 0.000f, 0.000f, 2.736875f, 1.412500f, 0.188750f),
            part("WHITE_WOOL", -0.456094, 0.687500, 0.071250, 0.000f, 0.000f, 0.000f, 1.321250f, 0.187500f, 0.755000f),
            part("WHITE_WOOL", -1.824531, 0.687500, 0.071250, 0.000f, 0.000f, 0.000f, 1.321250f, 0.187500f, 0.755000f),
            part("WOOL", -1.862281, 0.312500, -3.326250, 0.000f, 0.000f, 0.000f, 2.812375f, 0.437500f, 3.020000f),
            part("WHITE_WOOL", -1.824531, 0.500000, -3.326250, 0.000f, 0.000f, 0.000f, 2.736875f, 0.187500f, 4.152500f),
            part("WHITE_WOOL", -1.918906, 0.312500, -0.306250, 0.000f, 0.000f, 0.000f, 2.925625f, 0.468750f, 0.188750f),
            part("MUSHROOM_STEM", -1.635781, 0.437500, -3.441859, 0.000f, 0.000f, 0.000f, 2.359375f, 0.162500f, 0.010000f),
            part("MUSHROOM_STEM", 0.393281, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -0.007812, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -0.408906, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -1.635781, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -0.833594, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -1.234687, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_computer_chair() {
        return model("modern_computer_chair", "Computer Chair", FurnitureDefinition.Category.FURNITURE, "POLISHED_ANDESITE_STAIRS", 200, 1.075000, 1.687500, List.of(
            part("LIGHT_GRAY_WOOL", -0.921875, 0.500000, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.875000f),
            part("BLACK_CONCRETE", -0.562500, 0.125000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.375000f, 0.125000f),
            part("SIT", -0.484375, 0.625000, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("WHITE_CONCRETE", -0.562500, 0.125000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.312500f),
            part("BLACK_CONCRETE", -0.562500, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.562500, 0.125000, -0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.312500f),
            part("BLACK_CONCRETE", -0.562500, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.812500, 0.125000, -0.562500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.125000f),
            part("WHITE_CONCRETE", -0.437500, 0.125000, -0.562500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.125000f),
            part("BLACK_CONCRETE", -0.312500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("BLACK_CONCRETE", -0.812500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.796875, 0.687500, -0.187500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.421875, 0.687500, -0.187500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.125000f),
            part("LIGHT_GRAY_WOOL", -0.921875, 0.812500, -0.187500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.875000f, 0.125000f),
            part("WHITE_CONCRETE", -0.984375, 0.437500, -0.562500, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.125000f),
            part("WHITE_CONCRETE", -0.984375, 0.500000, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.125000f),
            part("LIGHT_GRAY_WOOL", -1.021875, 1.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.162500f, 0.062500f, 0.750000f),
            part("WHITE_CONCRETE", -0.434375, 0.437500, -0.562500, 0.000f, 0.000f, 0.000f, 0.450000f, 0.062500f, 0.125000f),
            part("LIGHT_GRAY_WOOL", -0.109375, 1.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.162500f, 0.062500f, 0.750000f),
            part("WHITE_CONCRETE", -0.046875, 0.500000, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel modern_computer_desk() {
        return model("modern_computer_desk", "Computer Desk", FurnitureDefinition.Category.FURNITURE, "POLISHED_BLACKSTONE", 200, 3.000000, 1.000000, List.of(
            part("OAK_PLANKS", -2.000000, 0.812500, -1.000000, 0.000f, 0.000f, 0.000f, 3.000000f, 0.187500f, 1.000000f),
            part("GRAY_CONCRETE", 0.687500, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.812500f, 0.125000f),
            part("LIGHT_GRAY_CONCRETE", -1.875000, 0.437500, -0.943750, 0.000f, 0.000f, 0.000f, 0.750000f, 0.375000f, 0.750000f),
            part("GRAY_CONCRETE", 0.687500, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.812500f, 0.125000f),
            part("GRAY_CONCRETE", 0.687500, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.500000f),
            part("GRAY_CONCRETE", -1.875000, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.250000, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.875000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.250000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("LIGHT_GRAY_CONCRETE", -1.875000, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.312500f, 0.606250f),
            part("WHITE_CONCRETE", -1.687500, 0.562500, -1.000000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("WHITE_CONCRETE", -1.687500, 0.250000, -1.000000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.937500, 0.062500, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.750000f, 0.875000f)
        ));
    }
    private static SmpFurnitureModel modern_computer_monitor() {
        return model("modern_computer_monitor", "Computer Monitor", FurnitureDefinition.Category.FURNITURE, "STONE_PRESSURE_PLATE", 200, 1.000000, 0.750000, List.of(
            part("BLACK_CONCRETE", -0.937500, 0.140625, -0.562500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.562500f, 0.062500f),
            part("GRAY_CONCRETE", -0.937500, 0.703125, -0.562500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.937500, 0.078125, -0.562500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.062500, 0.078125, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.687500f, 0.062500f),
            part("GRAY_CONCRETE", -1.000000, 0.078125, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.687500f, 0.062500f),
            part("GRAY_CONCRETE", -0.625000, 0.015625, -0.562500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.062500f),
            part("RED_CONCRETE", -0.125000, 0.078125, -0.562506, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_kitchen_cabinet() {
        return model("modern_kitchen_cabinet", "Kitchen Cabinet", FurnitureDefinition.Category.FURNITURE, "SMOOTH_QUARTZ", 200, 1.000000, 1.125000, List.of(
            part("NETHERITE_BLOCK", -1.000000, 0.812500, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.187500f, 1.000000f),
            part("WHITE_CONCRETE", -0.875000, 0.375000, -0.943750, 0.000f, 0.000f, 0.000f, 0.750000f, 0.187500f, 0.750000f),
            part("LIGHT_GRAY_CONCRETE", -0.937500, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.875000f),
            part("WHITE_CONCRETE", -0.875000, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.250000f, 0.606250f),
            part("WHITE_CONCRETE", -1.000000, 0.062500, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.750000f, 0.937500f),
            part("NETHERITE_BLOCK", -0.681250, 0.225000, -0.984375, 0.000f, 0.000f, 0.000f, 0.365625f, 0.050000f, 0.093750f),
            part("NETHERITE_BLOCK", -0.681250, 0.443750, -0.984375, 0.000f, 0.000f, 0.000f, 0.365625f, 0.050000f, 0.093750f),
            part("WHITE_CONCRETE", -0.875000, 0.562500, -0.959375, 0.000f, 0.000f, 0.000f, 0.750000f, 0.187500f, 0.750000f),
            part("NETHERITE_BLOCK", -0.681250, 0.631250, -0.984375, 0.000f, 0.000f, 0.000f, 0.365625f, 0.050000f, 0.093750f),
            part("NETHERITE_BLOCK", -1.000000, 1.000000, -0.062500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel modern_kitchen_chair() {
        return model("modern_kitchen_chair", "Kitchen Chair", FurnitureDefinition.Category.FURNITURE, "POLISHED_DIORITE_STAIRS", 200, 1.112500, 1.640625, List.of(
            part("WOOL", -0.921875, 0.578125, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.875000f),
            part("SIT", -0.484375, 0.750000, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.875000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("WOOL", -0.796875, 0.765625, -0.182812, 0.000f, 2.500f, 0.000f, 0.625000f, 0.875000f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.250000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.250000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.875000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.731250, 0.218750, -0.475000, 42.500f, 0.000f, 0.000f, 0.875000f, 0.125000f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.937444, 0.218750, -0.555228, -45.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.437444, 0.218750, -0.555228, -45.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.125000f),
            part("WOOL", -0.125000, 0.640625, -0.925000, 0.000f, 0.000f, -7.500f, 0.125000f, 0.312500f, 0.850000f),
            part("WOOL", -0.968750, 0.640625, -0.925000, 0.000f, 0.000f, 7.500f, 0.125000f, 0.312500f, 0.850000f),
            part("WOOL", -0.920567, 0.735665, -0.187500, 0.218f, 2.490f, -4.995f, 0.187500f, 0.875000f, 0.135937f),
            part("WOOL", -0.235683, 0.735665, -0.187500, -0.218f, 2.490f, 4.995f, 0.187500f, 0.875000f, 0.135937f)
        ));
    }
    private static SmpFurnitureModel modern_kitchen_table() {
        return model("modern_kitchen_table", "Kitchen Table", FurnitureDefinition.Category.FURNITURE, "POLISHED_ANDESITE_SLAB", 200, 5.000000, 1.000000, List.of(
            part("QUARTZ_BLOCK", -3.000000, 0.812500, -2.000000, 0.000f, 0.000f, 0.000f, 5.000000f, 0.187500f, 2.000000f),
            part("WHITE_CONCRETE", -2.500000, 0.000000, -1.656250, 0.000f, 0.000f, 0.000f, 4.375000f, 0.812500f, 1.250000f),
            part("QUARTZ_BLOCK", 1.875000, 0.000000, -2.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.812500f, 2.000000f)
        ));
    }
    private static SmpFurnitureModel modern_lamp1() {
        return model("modern_lamp1", "Lamp 1", FurnitureDefinition.Category.FURNITURE, "END_ROD", 200, 0.625000, 1.526758, List.of(
            part("GRAY_CONCRETE", -0.750000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.500000f),
            part("GRAY_CONCRETE", -0.531250, 0.125000, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 1.125000f, 0.062500f),
            part("GRAY_CONCRETE", -0.687500, 0.062500, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.437500f),
            part("GRAY_CONCRETE", -0.529687, 1.210937, -0.410937, 0.000f, -27.500f, 0.000f, 0.059375f, 0.250000f, 0.062500f),
            part("GRAY_CONCRETE", -0.625000, 1.335937, -0.593750, 0.000f, 22.500f, 0.000f, 0.250000f, 0.187500f, 0.187500f),
            part("GRAY_CONCRETE", -0.687500, 1.164062, -0.734375, 0.000f, 22.500f, 0.000f, 0.375000f, 0.187500f, 0.312500f),
            part("GRAY_CONCRETE", -0.750000, 1.129687, -0.875000, 0.000f, 22.500f, 0.000f, 0.062500f, 0.062500f, 0.437500f),
            part("GRAY_CONCRETE", -0.312500, 1.129687, -0.875000, 0.000f, 22.500f, 0.000f, 0.062500f, 0.062500f, 0.437500f),
            part("GRAY_CONCRETE", -0.687500, 1.129687, -0.875000, 0.000f, 22.500f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.687500, 1.129687, -0.500000, 0.000f, 22.500f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("LIGHT", -0.531250, 1.526758, -0.562801, 0.000f, 22.500f, 0.000f, 0.062500f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_lamp2() {
        return model("modern_lamp2", "Lamp 2", FurnitureDefinition.Category.FURNITURE, "SEA_LANTERN", 200, 0.500000, 1.526758, List.of(
            part("GRAY_CONCRETE", -0.531250, 0.062500, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.062500f),
            part("GRAY_CONCRETE", -0.687500, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.062500f, 0.250000f),
            part("GRAY_CONCRETE", -0.375000, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.062500f, 0.250000f),
            part("GRAY_CONCRETE", -0.625000, 0.442187, -0.687500, 0.000f, 0.000f, 0.000f, 0.250000f, 1.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.625000, 0.442187, -0.375000, 0.000f, 0.000f, 0.000f, 0.250000f, 1.062500f, 0.062500f),
            part("LIGHT", -0.531250, 1.526758, -0.562801, 0.000f, 22.500f, 0.000f, 0.062500f, 0.010000f, 0.010000f),
            part("GRAY_CONCRETE", -0.750000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.250000f),
            part("GRAY_CONCRETE", -0.625000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.059375f, 0.500000f),
            part("GRAY_CONCRETE", -0.656250, 1.062500, -0.531250, 0.000f, 0.000f, 0.000f, 0.312500f, 0.062500f, 0.062500f),
            part("WHITE_CONCRETE", -0.625000, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.025000f, 1.062500f, 0.250000f),
            part("WHITE_CONCRETE", -0.406250, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.031250f, 1.062500f, 0.250000f),
            part("WHITE_CONCRETE", -0.603125, 0.442187, -0.406250, 0.000f, 0.000f, 0.000f, 0.200000f, 1.062500f, 0.031250f),
            part("WHITE_CONCRETE", -0.600000, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.193750f, 1.062500f, 0.031250f)
        ));
    }
    private static SmpFurnitureModel modern_models_big_bed() {
        return model("modern_models_big_bed", "Big Bed", FurnitureDefinition.Category.FURNITURE, "STONE_BRICK_STAIRS", 200, 4.456859, 1.725000, List.of(
            part("WHITE_CONCRETE", -1.824531, 0.312500, -3.326250, 0.000f, 0.000f, 0.000f, 2.736875f, 0.187500f, 4.152500f),
            part("BLACK_CONCRETE", -1.730156, 0.000000, -3.326250, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("BLACK_CONCRETE", -1.730156, 0.000000, 0.637500, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("BLACK_CONCRETE", 0.629219, 0.000000, -3.326250, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("BLACK_CONCRETE", 0.629219, 0.000000, 0.637500, 0.000f, 0.000f, 0.000f, 0.188750f, 0.312500f, 0.188750f),
            part("WHITE_CONCRETE", -1.824531, 0.312500, -3.439500, 0.000f, 0.000f, 0.000f, 2.736875f, 0.412500f, 0.113250f),
            part("WHITE_CONCRETE", -1.824531, 0.312500, 0.826250, 0.000f, 0.000f, 0.000f, 2.736875f, 1.412500f, 0.188750f),
            part("WHITE_WOOL", -0.456094, 0.687500, 0.071250, 0.000f, 0.000f, 0.000f, 1.321250f, 0.187500f, 0.755000f),
            part("WHITE_WOOL", -1.824531, 0.687500, 0.071250, 0.000f, 0.000f, 0.000f, 1.321250f, 0.187500f, 0.755000f),
            part("WOOL", -1.862281, 0.312500, -3.326250, 0.000f, 0.000f, 0.000f, 2.812375f, 0.437500f, 3.020000f),
            part("WHITE_WOOL", -1.824531, 0.500000, -3.326250, 0.000f, 0.000f, 0.000f, 2.736875f, 0.187500f, 4.152500f),
            part("WHITE_WOOL", -1.918906, 0.312500, -0.306250, 0.000f, 0.000f, 0.000f, 2.925625f, 0.468750f, 0.188750f),
            part("MUSHROOM_STEM", -1.635781, 0.437500, -3.441859, 0.000f, 0.000f, 0.000f, 2.359375f, 0.162500f, 0.010000f),
            part("MUSHROOM_STEM", 0.393281, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -0.007812, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -0.408906, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -1.635781, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -0.833594, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f),
            part("MUSHROOM_STEM", -1.234687, 1.125000, 0.823891, 0.000f, 0.000f, 0.000f, 0.283125f, 0.412500f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_models_computer_chair() {
        return model("modern_models_computer_chair", "Computer Chair", FurnitureDefinition.Category.FURNITURE, "POLISHED_ANDESITE_STAIRS", 200, 1.075000, 1.687500, List.of(
            part("LIGHT_GRAY_WOOL", -0.921875, 0.500000, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.875000f),
            part("BLACK_CONCRETE", -0.562500, 0.125000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.375000f, 0.125000f),
            part("SIT", -0.484375, 0.625000, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("WHITE_CONCRETE", -0.562500, 0.125000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.312500f),
            part("BLACK_CONCRETE", -0.562500, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.562500, 0.125000, -0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.312500f),
            part("BLACK_CONCRETE", -0.562500, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.812500, 0.125000, -0.562500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.125000f),
            part("WHITE_CONCRETE", -0.437500, 0.125000, -0.562500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.125000f),
            part("BLACK_CONCRETE", -0.312500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("BLACK_CONCRETE", -0.812500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.796875, 0.687500, -0.187500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.125000f),
            part("WHITE_CONCRETE", -0.421875, 0.687500, -0.187500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.125000f),
            part("LIGHT_GRAY_WOOL", -0.921875, 0.812500, -0.187500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.875000f, 0.125000f),
            part("WHITE_CONCRETE", -0.984375, 0.437500, -0.562500, 0.000f, 0.000f, 0.000f, 0.437500f, 0.062500f, 0.125000f),
            part("WHITE_CONCRETE", -0.984375, 0.500000, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.125000f),
            part("LIGHT_GRAY_WOOL", -1.021875, 1.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.162500f, 0.062500f, 0.750000f),
            part("WHITE_CONCRETE", -0.434375, 0.437500, -0.562500, 0.000f, 0.000f, 0.000f, 0.450000f, 0.062500f, 0.125000f),
            part("LIGHT_GRAY_WOOL", -0.109375, 1.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.162500f, 0.062500f, 0.750000f),
            part("WHITE_CONCRETE", -0.046875, 0.500000, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.500000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel modern_models_computer_desk() {
        return model("modern_models_computer_desk", "Computer Desk", FurnitureDefinition.Category.FURNITURE, "POLISHED_BLACKSTONE", 200, 3.000000, 1.000000, List.of(
            part("OAK_PLANKS", -2.000000, 0.812500, -1.000000, 0.000f, 0.000f, 0.000f, 3.000000f, 0.187500f, 1.000000f),
            part("GRAY_CONCRETE", 0.687500, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.812500f, 0.125000f),
            part("LIGHT_GRAY_CONCRETE", -1.875000, 0.437500, -0.943750, 0.000f, 0.000f, 0.000f, 0.750000f, 0.375000f, 0.750000f),
            part("GRAY_CONCRETE", 0.687500, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.812500f, 0.125000f),
            part("GRAY_CONCRETE", 0.687500, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.500000f),
            part("GRAY_CONCRETE", -1.875000, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.250000, 0.000000, -0.812500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.875000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.250000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.062500f),
            part("LIGHT_GRAY_CONCRETE", -1.875000, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.312500f, 0.606250f),
            part("WHITE_CONCRETE", -1.687500, 0.562500, -1.000000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("WHITE_CONCRETE", -1.687500, 0.250000, -1.000000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -1.937500, 0.062500, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.750000f, 0.875000f)
        ));
    }
    private static SmpFurnitureModel modern_models_computer_monitor() {
        return model("modern_models_computer_monitor", "Computer Monitor", FurnitureDefinition.Category.FURNITURE, "STONE_PRESSURE_PLATE", 200, 1.000000, 0.750000, List.of(
            part("BLACK_CONCRETE", -0.937500, 0.140625, -0.562500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.562500f, 0.062500f),
            part("GRAY_CONCRETE", -0.937500, 0.703125, -0.562500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.937500, 0.078125, -0.562500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.062500, 0.078125, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.687500f, 0.062500f),
            part("GRAY_CONCRETE", -1.000000, 0.078125, -0.562500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.687500f, 0.062500f),
            part("GRAY_CONCRETE", -0.625000, 0.015625, -0.562500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.062500f, 0.062500f),
            part("RED_CONCRETE", -0.125000, 0.078125, -0.562506, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_models_kitchen_cabinet() {
        return model("modern_models_kitchen_cabinet", "Kitchen Cabinet", FurnitureDefinition.Category.FURNITURE, "SMOOTH_QUARTZ", 200, 1.000000, 1.125000, List.of(
            part("NETHERITE_BLOCK", -1.000000, 0.812500, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.187500f, 1.000000f),
            part("WHITE_CONCRETE", -0.875000, 0.375000, -0.943750, 0.000f, 0.000f, 0.000f, 0.750000f, 0.187500f, 0.750000f),
            part("LIGHT_GRAY_CONCRETE", -0.937500, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.875000f),
            part("WHITE_CONCRETE", -0.875000, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.250000f, 0.606250f),
            part("WHITE_CONCRETE", -1.000000, 0.062500, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.750000f, 0.937500f),
            part("NETHERITE_BLOCK", -0.681250, 0.225000, -0.984375, 0.000f, 0.000f, 0.000f, 0.365625f, 0.050000f, 0.093750f),
            part("NETHERITE_BLOCK", -0.681250, 0.443750, -0.984375, 0.000f, 0.000f, 0.000f, 0.365625f, 0.050000f, 0.093750f),
            part("WHITE_CONCRETE", -0.875000, 0.562500, -0.959375, 0.000f, 0.000f, 0.000f, 0.750000f, 0.187500f, 0.750000f),
            part("NETHERITE_BLOCK", -0.681250, 0.631250, -0.984375, 0.000f, 0.000f, 0.000f, 0.365625f, 0.050000f, 0.093750f),
            part("NETHERITE_BLOCK", -1.000000, 1.000000, -0.062500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 0.062500f)
        ));
    }
    private static SmpFurnitureModel modern_models_kitchen_chair() {
        return model("modern_models_kitchen_chair", "Kitchen Chair", FurnitureDefinition.Category.FURNITURE, "POLISHED_DIORITE_STAIRS", 200, 1.112500, 1.640625, List.of(
            part("WOOL", -0.921875, 0.578125, -0.937500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.875000f),
            part("SIT", -0.484375, 0.750000, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.875000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("WOOL", -0.796875, 0.765625, -0.182812, 0.000f, 2.500f, 0.000f, 0.625000f, 0.875000f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.250000, 0.000000, -0.250000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.250000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.875000, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.687500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.731250, 0.218750, -0.475000, 42.500f, 0.000f, 0.000f, 0.875000f, 0.125000f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.937444, 0.218750, -0.555228, -45.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.437444, 0.218750, -0.555228, -45.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.125000f),
            part("WOOL", -0.125000, 0.640625, -0.925000, 0.000f, 0.000f, -7.500f, 0.125000f, 0.312500f, 0.850000f),
            part("WOOL", -0.968750, 0.640625, -0.925000, 0.000f, 0.000f, 7.500f, 0.125000f, 0.312500f, 0.850000f),
            part("WOOL", -0.920567, 0.735665, -0.187500, 0.218f, 2.490f, -4.995f, 0.187500f, 0.875000f, 0.135937f),
            part("WOOL", -0.235683, 0.735665, -0.187500, -0.218f, 2.490f, 4.995f, 0.187500f, 0.875000f, 0.135937f)
        ));
    }
    private static SmpFurnitureModel modern_models_kitchen_table() {
        return model("modern_models_kitchen_table", "Kitchen Table", FurnitureDefinition.Category.FURNITURE, "POLISHED_ANDESITE_SLAB", 200, 5.000000, 1.000000, List.of(
            part("QUARTZ_BLOCK", -3.000000, 0.812500, -2.000000, 0.000f, 0.000f, 0.000f, 5.000000f, 0.187500f, 2.000000f),
            part("WHITE_CONCRETE", -2.500000, 0.000000, -1.656250, 0.000f, 0.000f, 0.000f, 4.375000f, 0.812500f, 1.250000f),
            part("QUARTZ_BLOCK", 1.875000, 0.000000, -2.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.812500f, 2.000000f)
        ));
    }
    private static SmpFurnitureModel modern_models_lamp1() {
        return model("modern_models_lamp1", "Lamp 1", FurnitureDefinition.Category.FURNITURE, "END_ROD", 200, 0.625000, 1.526758, List.of(
            part("GRAY_CONCRETE", -0.750000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.500000f),
            part("GRAY_CONCRETE", -0.531250, 0.125000, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 1.125000f, 0.062500f),
            part("GRAY_CONCRETE", -0.687500, 0.062500, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.062500f, 0.437500f),
            part("GRAY_CONCRETE", -0.529687, 1.210937, -0.410937, 0.000f, -27.500f, 0.000f, 0.059375f, 0.250000f, 0.062500f),
            part("GRAY_CONCRETE", -0.625000, 1.335937, -0.593750, 0.000f, 22.500f, 0.000f, 0.250000f, 0.187500f, 0.187500f),
            part("GRAY_CONCRETE", -0.687500, 1.164062, -0.734375, 0.000f, 22.500f, 0.000f, 0.375000f, 0.187500f, 0.312500f),
            part("GRAY_CONCRETE", -0.750000, 1.129687, -0.875000, 0.000f, 22.500f, 0.000f, 0.062500f, 0.062500f, 0.437500f),
            part("GRAY_CONCRETE", -0.312500, 1.129687, -0.875000, 0.000f, 22.500f, 0.000f, 0.062500f, 0.062500f, 0.437500f),
            part("GRAY_CONCRETE", -0.687500, 1.129687, -0.875000, 0.000f, 22.500f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.687500, 1.129687, -0.500000, 0.000f, 22.500f, 0.000f, 0.375000f, 0.062500f, 0.062500f),
            part("LIGHT", -0.531250, 1.526758, -0.562801, 0.000f, 22.500f, 0.000f, 0.062500f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_models_lamp2() {
        return model("modern_models_lamp2", "Lamp 2", FurnitureDefinition.Category.FURNITURE, "SEA_LANTERN", 200, 0.500000, 1.526758, List.of(
            part("GRAY_CONCRETE", -0.531250, 0.062500, -0.531250, 0.000f, 0.000f, 0.000f, 0.062500f, 1.000000f, 0.062500f),
            part("GRAY_CONCRETE", -0.687500, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.062500f, 0.250000f),
            part("GRAY_CONCRETE", -0.375000, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 1.062500f, 0.250000f),
            part("GRAY_CONCRETE", -0.625000, 0.442187, -0.687500, 0.000f, 0.000f, 0.000f, 0.250000f, 1.062500f, 0.062500f),
            part("GRAY_CONCRETE", -0.625000, 0.442187, -0.375000, 0.000f, 0.000f, 0.000f, 0.250000f, 1.062500f, 0.062500f),
            part("LIGHT", -0.531250, 1.526758, -0.562801, 0.000f, 22.500f, 0.000f, 0.062500f, 0.010000f, 0.010000f),
            part("GRAY_CONCRETE", -0.750000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.250000f),
            part("GRAY_CONCRETE", -0.625000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.250000f, 0.059375f, 0.500000f),
            part("GRAY_CONCRETE", -0.656250, 1.062500, -0.531250, 0.000f, 0.000f, 0.000f, 0.312500f, 0.062500f, 0.062500f),
            part("WHITE_CONCRETE", -0.625000, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.025000f, 1.062500f, 0.250000f),
            part("WHITE_CONCRETE", -0.406250, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.031250f, 1.062500f, 0.250000f),
            part("WHITE_CONCRETE", -0.603125, 0.442187, -0.406250, 0.000f, 0.000f, 0.000f, 0.200000f, 1.062500f, 0.031250f),
            part("WHITE_CONCRETE", -0.600000, 0.442187, -0.625000, 0.000f, 0.000f, 0.000f, 0.193750f, 1.062500f, 0.031250f)
        ));
    }
    private static SmpFurnitureModel modern_models_plant() {
        return model("modern_models_plant", "Plant", FurnitureDefinition.Category.FURNITURE, "FERN", 200, 0.500000, 1.974872, List.of(
            part("MUD", -0.750000, 0.500000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.500000f),
            part("MUD", -0.750000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.062500f),
            part("MUD", -0.750000, 0.000000, -0.312500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.062500f),
            part("MUD", -0.312500, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.375000f),
            part("MUD", -0.750000, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.375000f),
            part("MUD", -0.750000, 0.062500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("MUD", -0.312500, 0.062500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("MUD", -0.312500, 0.062500, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("MUD", -0.750000, 0.062500, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("DIRT", -0.687500, 0.515625, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.500000f, 0.375000f),
            part("FLOWERING_AZALEA_LEAVES", -0.687500, 1.015625, -0.640625, 5.750f, 0.000f, 0.000f, 0.375000f, 0.500000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650000, 1.498913, -0.650763, 5.750f, -5.000f, -0.000f, 0.268750f, 0.350000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650000, 1.843549, -0.655435, 5.750f, -5.000f, -0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.525000, 1.843549, -0.655435, 5.750f, -5.000f, -0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.687500, 1.015625, -0.343750, 7.750f, 0.000f, 0.000f, 0.375000f, 0.500000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650000, 1.498587, -0.337473, 7.750f, 5.000f, 0.000f, 0.268750f, 0.350000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.587500, 1.843622, -0.372494, 7.750f, 12.500f, 0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.462500, 1.843622, -0.372494, 7.750f, 12.500f, 0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.687500, 1.015625, -0.609375, -5.000f, 0.000f, 0.000f, 0.010000f, 0.312500f, 0.250000f),
            part("FLOWERING_AZALEA_LEAVES", -0.681536, 1.295416, -0.484375, -4.434f, -2.313f, 27.590f, 0.010000f, 0.312500f, 0.125000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650467, 1.424030, -0.606250, 11.069f, 5.845f, 28.067f, 0.010000f, 0.312500f, 0.218750f),
            part("FLOWERING_AZALEA_LEAVES", -0.609375, 1.015625, -0.606250, 12.500f, 0.000f, 0.000f, 0.010000f, 0.437500f, 0.250000f),
            part("FLOWERING_AZALEA_LEAVES", -0.645906, 1.703501, -0.481250, 11.069f, 5.845f, 28.067f, 0.010000f, 0.187500f, 0.093750f),
            part("FLOWERING_AZALEA_LEAVES", -0.354634, 1.698337, -0.512500, 16.666f, -5.416f, -18.294f, 0.010000f, 0.187500f, 0.093750f),
            part("FLOWERING_AZALEA_LEAVES", -0.327674, 1.296944, -0.506250, -7.151f, 2.267f, -17.642f, 0.010000f, 0.312500f, 0.125000f),
            part("FLOWERING_AZALEA_LEAVES", -0.378874, 1.426952, -0.637500, 15.815f, -7.590f, -26.056f, 0.010000f, 0.312500f, 0.218750f),
            part("FLOWERING_AZALEA_LEAVES", -0.406250, 1.015625, -0.637500, 17.500f, 0.000f, 0.000f, 0.010000f, 0.437500f, 0.250000f),
            part("FLOWERING_AZALEA_LEAVES", -0.328125, 1.015625, -0.631250, -7.500f, 0.000f, 0.000f, 0.010000f, 0.312500f, 0.250000f)
        ));
    }
    private static SmpFurnitureModel modern_models_sofa() {
        return model("modern_models_sofa", "Short Sofa", FurnitureDefinition.Category.FURNITURE, "POLISHED_BASALT", 200, 3.340500, 1.207500, List.of(
            part("WOOL", -1.850938, 0.375000, -1.193750, 0.000f, 0.000f, 0.000f, 2.751000f, 0.195000f, 1.200000f),
            part("STONE", 0.605313, 0.000000, -1.043750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("STONE", -1.850938, 0.000000, -1.043750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("STONE", -1.850938, 0.000000, -0.293750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("STONE", 0.605313, 0.000000, -0.293750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("WOOL", 0.900062, 0.375000, -1.193750, 0.000f, 0.000f, 0.000f, 0.294750f, 0.675000f, 1.200000f),
            part("WOOL", -2.145688, 0.375000, -1.193750, 0.000f, 0.000f, 0.000f, 0.294750f, 0.675000f, 1.200000f),
            part("WOOL", -1.850938, 0.525000, -1.141250, 0.000f, 0.000f, 0.000f, 2.751000f, 0.225000f, 0.997500f),
            part("SIT", -0.475438, 0.750000, -0.616250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("WOOL", -1.850938, 0.562500, -0.143750, 0.000f, 0.000f, 0.000f, 2.751000f, 0.645000f, 0.150000f),
            part("WOOL", 0.163188, 0.750000, -0.447500, 27.500f, 17.500f, -0.000f, 0.786000f, 0.450000f, 0.172500f),
            part("WOOL", -1.850938, 0.750000, -0.503750, -30.823f, 5.550f, 2.208f, 0.786000f, 0.450000f, 0.172500f)
        ));
    }
    private static SmpFurnitureModel modern_models_sofa2() {
        return model("modern_models_sofa2", "Long Sofa", FurnitureDefinition.Category.FURNITURE, "RED_WOOL", 200, 2.921875, 1.068750, List.of(
            part("WOOL", -1.796875, 0.312500, -1.187500, 0.000f, 0.000f, 0.000f, 2.625000f, 0.162500f, 1.187500f),
            part("IRON_BLOCK", 0.640625, 0.000000, -1.062500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("IRON_BLOCK", -1.734375, 0.000000, -1.062500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("IRON_BLOCK", -1.734375, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("IRON_BLOCK", 0.640625, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("WOOL", 0.796875, 0.312500, -1.171875, 0.000f, 0.000f, -5.000f, 0.187500f, 0.625000f, 1.171875f),
            part("WOOL", -1.937500, 0.312500, -1.171875, 0.000f, 0.000f, 7.500f, 0.187500f, 0.625000f, 1.171875f),
            part("WOOL", -0.046875, 0.437500, -1.143750, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.956250f),
            part("SIT", -0.484375, 0.625000, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("WOOL", -0.046875, 0.468750, -0.187500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.600000f, 0.125000f),
            part("IRON_BLOCK", -1.734375, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.812500f),
            part("IRON_BLOCK", 0.640625, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.812500f),
            part("IRON_BLOCK", -1.859375, 0.250000, -1.125000, 0.000f, 0.000f, 0.000f, 2.750000f, 0.062500f, 1.062500f),
            part("WOOL", -0.915625, 0.437500, -1.143750, 0.000f, 0.000f, 0.000f, 0.856250f, 0.187500f, 0.956250f),
            part("WOOL", -1.796875, 0.437500, -1.143750, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.956250f),
            part("WOOL", -1.784375, 0.468750, -0.062500, 0.000f, 0.000f, 0.000f, 2.606250f, 0.600000f, 0.062500f),
            part("WOOL", -1.796875, 0.468750, -0.187500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.600000f, 0.125000f),
            part("WOOL", -0.909375, 0.468750, -0.187500, 0.000f, 0.000f, 0.000f, 0.856250f, 0.600000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel modern_models_table() {
        return model("modern_models_table", "Table", FurnitureDefinition.Category.FURNITURE, "POLISHED_ANDESITE", 200, 2.000000, 0.687500, List.of(
            part("GRAY_CONCRETE", -1.500000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 2.000000f, 0.125000f, 1.000000f),
            part("GRAY_CONCRETE", -1.500000, 0.562500, -1.000000, 0.000f, 0.000f, 0.000f, 2.000000f, 0.125000f, 1.000000f),
            part("GRAY_CONCRETE", 0.375000, 0.125000, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.437500f, 1.000000f),
            part("QUARTZ_BLOCK", -1.062500, 0.125000, -0.625000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.437500f, 0.187500f)
        ));
    }
    private static SmpFurnitureModel modern_models_table_lamp() {
        return model("modern_models_table_lamp", "Table Lamp", FurnitureDefinition.Category.FURNITURE, "SHROOMLIGHT", 200, 0.431250, 1.053463, List.of(
            part("GRAY_CONCRETE", -0.672500, 0.000000, -0.672500, 0.000f, 0.000f, 0.000f, 0.345000f, 0.043125f, 0.345000f),
            part("GRAY_CONCRETE", -0.521563, 0.086250, -0.370625, 0.000f, 0.000f, 0.000f, 0.043125f, 0.526250f, 0.043125f),
            part("GRAY_CONCRETE", -0.629375, 0.043125, -0.629375, 0.000f, 0.000f, 0.000f, 0.258750f, 0.043125f, 0.301875f),
            part("GRAY_CONCRETE", -0.520484, 0.585547, -0.438547, 0.000f, -27.500f, 0.000f, 0.040969f, 0.172500f, 0.043125f),
            part("GRAY_CONCRETE", -0.586250, 0.671797, -0.564688, 0.000f, 22.500f, 0.000f, 0.172500f, 0.129375f, 0.129375f),
            part("GRAY_CONCRETE", -0.629375, 0.553203, -0.661719, 0.000f, 22.500f, 0.000f, 0.258750f, 0.129375f, 0.215625f),
            part("GRAY_CONCRETE", -0.672500, 0.529484, -0.758750, 0.000f, 22.500f, 0.000f, 0.043125f, 0.043125f, 0.301875f),
            part("GRAY_CONCRETE", -0.370625, 0.529484, -0.758750, 0.000f, 22.500f, 0.000f, 0.043125f, 0.043125f, 0.301875f),
            part("GRAY_CONCRETE", -0.629375, 0.529484, -0.758750, 0.000f, 22.500f, 0.000f, 0.258750f, 0.043125f, 0.043125f),
            part("GRAY_CONCRETE", -0.629375, 0.529484, -0.500000, 0.000f, 22.500f, 0.000f, 0.258750f, 0.043125f, 0.043125f),
            part("LIGHT", -0.521563, 1.053463, -0.543333, 0.000f, 22.500f, 0.000f, 0.043125f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_models_tv() {
        return model("modern_models_tv", "TV", FurnitureDefinition.Category.FURNITURE, "BLACK_CONCRETE", 200, 3.146400, 1.704300, List.of(
            part("BLACK_CONCRETE", -1.960575, 0.294975, -0.031138, 0.000f, 0.000f, 0.000f, 2.884200f, 1.442100f, 0.023288f),
            part("GRAY_CONCRETE", -1.960575, 1.737075, -0.031138, 0.000f, 0.000f, 0.000f, 2.884200f, 0.131100f, 0.023288f),
            part("GRAY_CONCRETE", -1.960575, 0.163875, -0.031138, 0.000f, 0.000f, 0.000f, 2.884200f, 0.131100f, 0.023288f),
            part("GRAY_CONCRETE", 0.923625, 0.163875, -0.031138, 0.000f, 0.000f, 0.000f, 0.131100f, 1.704300f, 0.023288f),
            part("GRAY_CONCRETE", -2.091675, 0.163875, -0.031138, 0.000f, 0.000f, 0.000f, 0.131100f, 1.704300f, 0.023288f)
        ));
    }
    private static SmpFurnitureModel modern_models_upper_kitchen_cabinet() {
        return model("modern_models_upper_kitchen_cabinet", "Upper Kitchen Cabinet", FurnitureDefinition.Category.FURNITURE, "SMOOTH_QUARTZ_SLAB", 200, 1.000000, 1.000000, List.of(
            part("WHITE_CONCRETE", -0.500000, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.437500f, 0.843750f, 0.606250f),
            part("WHITE_CONCRETE", -1.000000, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 1.000000f, 0.937500f),
            part("NETHERITE_BLOCK", -0.368750, 0.350000, -0.984375, 0.000f, 0.000f, 0.000f, 0.053125f, 0.362500f, 0.093750f),
            part("WHITE_CONCRETE", -0.937500, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.437500f, 0.843750f, 0.606250f),
            part("NETHERITE_BLOCK", -0.681250, 0.350000, -0.984375, 0.000f, 0.000f, 0.000f, 0.053125f, 0.362500f, 0.093750f)
        ));
    }
    private static SmpFurnitureModel modern_models_wood_panel() {
        return model("modern_models_wood_panel", "Wood Panel", FurnitureDefinition.Category.FURNITURE, "OAK_PLANKS", 200, 2.625000, 1.937500, List.of(
            part("WOOD", -0.187500, 0.062500, -0.125000, 0.000f, 0.000f, 90.000f, 2.000000f, 0.125000f, 0.125000f),
            part("WOOD", -0.500000, 0.062500, -0.125000, 0.000f, 0.000f, 90.000f, 2.000000f, 0.125000f, 0.125000f),
            part("WOOD", -0.812500, 0.062500, -0.125000, 0.000f, 0.000f, 90.000f, 2.000000f, 0.125000f, 0.125000f),
            part("AIR", -0.812500, 0.437500, -0.187500, 0.000f, 0.000f, 0.000f, 0.750000f, 1.562500f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel modern_plant() {
        return model("modern_plant", "Plant", FurnitureDefinition.Category.FURNITURE, "FERN", 200, 0.500000, 1.974872, List.of(
            part("MUD", -0.750000, 0.500000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.500000f, 0.500000f),
            part("MUD", -0.750000, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.062500f),
            part("MUD", -0.750000, 0.000000, -0.312500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.062500f),
            part("MUD", -0.312500, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.375000f),
            part("MUD", -0.750000, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.375000f),
            part("MUD", -0.750000, 0.062500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("MUD", -0.312500, 0.062500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("MUD", -0.312500, 0.062500, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("MUD", -0.750000, 0.062500, -0.312500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.437500f, 0.062500f),
            part("DIRT", -0.687500, 0.515625, -0.687500, 0.000f, 0.000f, 0.000f, 0.375000f, 0.500000f, 0.375000f),
            part("FLOWERING_AZALEA_LEAVES", -0.687500, 1.015625, -0.640625, 5.750f, 0.000f, 0.000f, 0.375000f, 0.500000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650000, 1.498913, -0.650763, 5.750f, -5.000f, -0.000f, 0.268750f, 0.350000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650000, 1.843549, -0.655435, 5.750f, -5.000f, -0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.525000, 1.843549, -0.655435, 5.750f, -5.000f, -0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.687500, 1.015625, -0.343750, 7.750f, 0.000f, 0.000f, 0.375000f, 0.500000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650000, 1.498587, -0.337473, 7.750f, 5.000f, 0.000f, 0.268750f, 0.350000f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.587500, 1.843622, -0.372494, 7.750f, 12.500f, 0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.462500, 1.843622, -0.372494, 7.750f, 12.500f, 0.000f, 0.081250f, 0.131250f, 0.010000f),
            part("FLOWERING_AZALEA_LEAVES", -0.687500, 1.015625, -0.609375, -5.000f, 0.000f, 0.000f, 0.010000f, 0.312500f, 0.250000f),
            part("FLOWERING_AZALEA_LEAVES", -0.681536, 1.295416, -0.484375, -4.434f, -2.313f, 27.590f, 0.010000f, 0.312500f, 0.125000f),
            part("FLOWERING_AZALEA_LEAVES", -0.650467, 1.424030, -0.606250, 11.069f, 5.845f, 28.067f, 0.010000f, 0.312500f, 0.218750f),
            part("FLOWERING_AZALEA_LEAVES", -0.609375, 1.015625, -0.606250, 12.500f, 0.000f, 0.000f, 0.010000f, 0.437500f, 0.250000f),
            part("FLOWERING_AZALEA_LEAVES", -0.645906, 1.703501, -0.481250, 11.069f, 5.845f, 28.067f, 0.010000f, 0.187500f, 0.093750f),
            part("FLOWERING_AZALEA_LEAVES", -0.354634, 1.698337, -0.512500, 16.666f, -5.416f, -18.294f, 0.010000f, 0.187500f, 0.093750f),
            part("FLOWERING_AZALEA_LEAVES", -0.327674, 1.296944, -0.506250, -7.151f, 2.267f, -17.642f, 0.010000f, 0.312500f, 0.125000f),
            part("FLOWERING_AZALEA_LEAVES", -0.378874, 1.426952, -0.637500, 15.815f, -7.590f, -26.056f, 0.010000f, 0.312500f, 0.218750f),
            part("FLOWERING_AZALEA_LEAVES", -0.406250, 1.015625, -0.637500, 17.500f, 0.000f, 0.000f, 0.010000f, 0.437500f, 0.250000f),
            part("FLOWERING_AZALEA_LEAVES", -0.328125, 1.015625, -0.631250, -7.500f, 0.000f, 0.000f, 0.010000f, 0.312500f, 0.250000f)
        ));
    }
    private static SmpFurnitureModel modern_sofa() {
        return model("modern_sofa", "Short Sofa", FurnitureDefinition.Category.FURNITURE, "POLISHED_BASALT", 200, 3.340500, 1.207500, List.of(
            part("WOOL", -1.850938, 0.375000, -1.193750, 0.000f, 0.000f, 0.000f, 2.751000f, 0.195000f, 1.200000f),
            part("STONE", 0.605313, 0.000000, -1.043750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("STONE", -1.850938, 0.000000, -1.043750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("STONE", -1.850938, 0.000000, -0.293750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("STONE", 0.605313, 0.000000, -0.293750, 0.000f, 0.000f, 0.000f, 0.196500f, 0.375000f, 0.150000f),
            part("WOOL", 0.900062, 0.375000, -1.193750, 0.000f, 0.000f, 0.000f, 0.294750f, 0.675000f, 1.200000f),
            part("WOOL", -2.145688, 0.375000, -1.193750, 0.000f, 0.000f, 0.000f, 0.294750f, 0.675000f, 1.200000f),
            part("WOOL", -1.850938, 0.525000, -1.141250, 0.000f, 0.000f, 0.000f, 2.751000f, 0.225000f, 0.997500f),
            part("SIT", -0.475438, 0.750000, -0.616250, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("WOOL", -1.850938, 0.562500, -0.143750, 0.000f, 0.000f, 0.000f, 2.751000f, 0.645000f, 0.150000f),
            part("WOOL", 0.163188, 0.750000, -0.447500, 27.500f, 17.500f, -0.000f, 0.786000f, 0.450000f, 0.172500f),
            part("WOOL", -1.850938, 0.750000, -0.503750, -30.823f, 5.550f, 2.208f, 0.786000f, 0.450000f, 0.172500f)
        ));
    }
    private static SmpFurnitureModel modern_sofa2() {
        return model("modern_sofa2", "Long Sofa", FurnitureDefinition.Category.FURNITURE, "RED_WOOL", 200, 2.921875, 1.068750, List.of(
            part("WOOL", -1.796875, 0.312500, -1.187500, 0.000f, 0.000f, 0.000f, 2.625000f, 0.162500f, 1.187500f),
            part("IRON_BLOCK", 0.640625, 0.000000, -1.062500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("IRON_BLOCK", -1.734375, 0.000000, -1.062500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("IRON_BLOCK", -1.734375, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("IRON_BLOCK", 0.640625, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.062500f, 0.312500f, 0.062500f),
            part("WOOL", 0.796875, 0.312500, -1.171875, 0.000f, 0.000f, -5.000f, 0.187500f, 0.625000f, 1.171875f),
            part("WOOL", -1.937500, 0.312500, -1.171875, 0.000f, 0.000f, 7.500f, 0.187500f, 0.625000f, 1.171875f),
            part("WOOL", -0.046875, 0.437500, -1.143750, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.956250f),
            part("SIT", -0.484375, 0.625000, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("WOOL", -0.046875, 0.468750, -0.187500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.600000f, 0.125000f),
            part("IRON_BLOCK", -1.734375, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.812500f),
            part("IRON_BLOCK", 0.640625, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.812500f),
            part("IRON_BLOCK", -1.859375, 0.250000, -1.125000, 0.000f, 0.000f, 0.000f, 2.750000f, 0.062500f, 1.062500f),
            part("WOOL", -0.915625, 0.437500, -1.143750, 0.000f, 0.000f, 0.000f, 0.856250f, 0.187500f, 0.956250f),
            part("WOOL", -1.796875, 0.437500, -1.143750, 0.000f, 0.000f, 0.000f, 0.875000f, 0.187500f, 0.956250f),
            part("WOOL", -1.784375, 0.468750, -0.062500, 0.000f, 0.000f, 0.000f, 2.606250f, 0.600000f, 0.062500f),
            part("WOOL", -1.796875, 0.468750, -0.187500, 0.000f, 0.000f, 0.000f, 0.875000f, 0.600000f, 0.125000f),
            part("WOOL", -0.909375, 0.468750, -0.187500, 0.000f, 0.000f, 0.000f, 0.856250f, 0.600000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel modern_table() {
        return model("modern_table", "Table", FurnitureDefinition.Category.FURNITURE, "POLISHED_ANDESITE", 200, 2.000000, 0.687500, List.of(
            part("GRAY_CONCRETE", -1.500000, 0.000000, -1.000000, 0.000f, 0.000f, 0.000f, 2.000000f, 0.125000f, 1.000000f),
            part("GRAY_CONCRETE", -1.500000, 0.562500, -1.000000, 0.000f, 0.000f, 0.000f, 2.000000f, 0.125000f, 1.000000f),
            part("GRAY_CONCRETE", 0.375000, 0.125000, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.437500f, 1.000000f),
            part("QUARTZ_BLOCK", -1.062500, 0.125000, -0.625000, 0.000f, 0.000f, 0.000f, 0.187500f, 0.437500f, 0.187500f)
        ));
    }
    private static SmpFurnitureModel modern_table_lamp() {
        return model("modern_table_lamp", "Table Lamp", FurnitureDefinition.Category.FURNITURE, "SHROOMLIGHT", 200, 0.431250, 1.053463, List.of(
            part("GRAY_CONCRETE", -0.672500, 0.000000, -0.672500, 0.000f, 0.000f, 0.000f, 0.345000f, 0.043125f, 0.345000f),
            part("GRAY_CONCRETE", -0.521563, 0.086250, -0.370625, 0.000f, 0.000f, 0.000f, 0.043125f, 0.526250f, 0.043125f),
            part("GRAY_CONCRETE", -0.629375, 0.043125, -0.629375, 0.000f, 0.000f, 0.000f, 0.258750f, 0.043125f, 0.301875f),
            part("GRAY_CONCRETE", -0.520484, 0.585547, -0.438547, 0.000f, -27.500f, 0.000f, 0.040969f, 0.172500f, 0.043125f),
            part("GRAY_CONCRETE", -0.586250, 0.671797, -0.564688, 0.000f, 22.500f, 0.000f, 0.172500f, 0.129375f, 0.129375f),
            part("GRAY_CONCRETE", -0.629375, 0.553203, -0.661719, 0.000f, 22.500f, 0.000f, 0.258750f, 0.129375f, 0.215625f),
            part("GRAY_CONCRETE", -0.672500, 0.529484, -0.758750, 0.000f, 22.500f, 0.000f, 0.043125f, 0.043125f, 0.301875f),
            part("GRAY_CONCRETE", -0.370625, 0.529484, -0.758750, 0.000f, 22.500f, 0.000f, 0.043125f, 0.043125f, 0.301875f),
            part("GRAY_CONCRETE", -0.629375, 0.529484, -0.758750, 0.000f, 22.500f, 0.000f, 0.258750f, 0.043125f, 0.043125f),
            part("GRAY_CONCRETE", -0.629375, 0.529484, -0.500000, 0.000f, 22.500f, 0.000f, 0.258750f, 0.043125f, 0.043125f),
            part("LIGHT", -0.521563, 1.053463, -0.543333, 0.000f, 22.500f, 0.000f, 0.043125f, 0.010000f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel modern_tv() {
        return model("modern_tv", "TV", FurnitureDefinition.Category.FURNITURE, "BLACK_CONCRETE", 200, 3.146400, 1.704300, List.of(
            part("BLACK_CONCRETE", -1.960575, 0.294975, -0.031138, 0.000f, 0.000f, 0.000f, 2.884200f, 1.442100f, 0.023288f),
            part("GRAY_CONCRETE", -1.960575, 1.737075, -0.031138, 0.000f, 0.000f, 0.000f, 2.884200f, 0.131100f, 0.023288f),
            part("GRAY_CONCRETE", -1.960575, 0.163875, -0.031138, 0.000f, 0.000f, 0.000f, 2.884200f, 0.131100f, 0.023288f),
            part("GRAY_CONCRETE", 0.923625, 0.163875, -0.031138, 0.000f, 0.000f, 0.000f, 0.131100f, 1.704300f, 0.023288f),
            part("GRAY_CONCRETE", -2.091675, 0.163875, -0.031138, 0.000f, 0.000f, 0.000f, 0.131100f, 1.704300f, 0.023288f)
        ));
    }
    private static SmpFurnitureModel modern_upper_kitchen_cabinet() {
        return model("modern_upper_kitchen_cabinet", "Upper Kitchen Cabinet", FurnitureDefinition.Category.FURNITURE, "SMOOTH_QUARTZ_SLAB", 200, 1.000000, 1.000000, List.of(
            part("WHITE_CONCRETE", -0.500000, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.437500f, 0.843750f, 0.606250f),
            part("WHITE_CONCRETE", -1.000000, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 1.000000f, 0.937500f),
            part("NETHERITE_BLOCK", -0.368750, 0.350000, -0.984375, 0.000f, 0.000f, 0.000f, 0.053125f, 0.362500f, 0.093750f),
            part("WHITE_CONCRETE", -0.937500, 0.125000, -0.950000, 0.000f, 0.000f, 0.000f, 0.437500f, 0.843750f, 0.606250f),
            part("NETHERITE_BLOCK", -0.681250, 0.350000, -0.984375, 0.000f, 0.000f, 0.000f, 0.053125f, 0.362500f, 0.093750f)
        ));
    }
    private static SmpFurnitureModel modern_wood_panel() {
        return model("modern_wood_panel", "Wood Panel", FurnitureDefinition.Category.FURNITURE, "OAK_PLANKS", 200, 2.625000, 1.937500, List.of(
            part("WOOD", -0.187500, 0.062500, -0.125000, 0.000f, 0.000f, 90.000f, 2.000000f, 0.125000f, 0.125000f),
            part("WOOD", -0.500000, 0.062500, -0.125000, 0.000f, 0.000f, 90.000f, 2.000000f, 0.125000f, 0.125000f),
            part("WOOD", -0.812500, 0.062500, -0.125000, 0.000f, 0.000f, 90.000f, 2.000000f, 0.125000f, 0.125000f),
            part("AIR", -0.812500, 0.437500, -0.187500, 0.000f, 0.000f, 0.000f, 0.750000f, 1.562500f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_bbq_grill() {
        return model("outdoor_bbq_grill", "bbq_grill", FurnitureDefinition.Category.OUTDOOR, "FURNACE", 220, 2.000000, 1.660969, List.of(
            part("IRON_BLOCK", -1.562500, 0.125000, -1.000000, 0.000f, 0.000f, 0.000f, 1.125000f, 0.875000f, 1.000000f),
            part("IRON_BLOCK", -1.500000, 0.000000, -0.926562, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("IRON_BLOCK", -0.625000, 0.000000, -0.926562, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("IRON_BLOCK", -0.625000, 0.000000, -0.176563, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("IRON_BLOCK", -1.500000, 0.000000, -0.176563, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("IRON_BLOCK", -2.000000, 0.875000, -0.989062, 0.000f, 0.000f, 0.000f, 0.437500f, 0.125000f, 0.875000f),
            part("IRON_BLOCK", -0.437500, 0.875000, -0.989062, 0.000f, 0.000f, 0.000f, 0.437500f, 0.125000f, 0.875000f),
            part("IRON_BLOCK", -1.562500, 1.598469, -0.688445, 0.000f, 55.000f, 0.000f, 1.125000f, 0.062500f, 1.000000f),
            part("IRON_BLOCK", -0.500000, 1.348469, -0.688445, 0.000f, 55.000f, 0.000f, 0.062500f, 0.250000f, 1.000000f),
            part("IRON_BLOCK", -1.562500, 1.348469, -0.688445, 0.000f, 55.000f, 0.000f, 0.062500f, 0.250000f, 1.000000f),
            part("IRON_BLOCK", -1.500000, 1.348469, 0.249055, 0.000f, 55.000f, 0.000f, 1.000000f, 0.250000f, 0.062500f),
            part("IRON_BLOCK", -1.500000, 1.348469, -0.688445, 0.000f, 55.000f, 0.000f, 1.000000f, 0.250000f, 0.062500f),
            part("BLACK_CONCRETE", -1.375000, 0.812500, -1.020312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.031250f),
            part("BLACK_CONCRETE", -1.250000, 0.812500, -1.020312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.031250f),
            part("BLACK_CONCRETE", -0.812500, 0.812500, -1.020312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.031250f),
            part("BLACK_CONCRETE", -0.687500, 0.812500, -1.020312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.031250f),
            part("IRON_BLOCK", -1.500000, 0.250000, -1.015625, 0.000f, 0.000f, 0.000f, 1.000000f, 0.500000f, 0.078125f),
            part("BLACK_CONCRETE", -1.343750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f),
            part("BLACK_CONCRETE", -1.218750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f),
            part("BLACK_CONCRETE", -1.093750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f),
            part("BLACK_CONCRETE", -0.968750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f),
            part("BLACK_CONCRETE", -0.843750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f),
            part("BLACK_CONCRETE", -0.718750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f),
            part("BLACK_CONCRETE", -0.593750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f),
            part("IRON_BLOCK", -1.312500, 0.312500, -1.021875, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.084375f),
            part("COOKED_BEEF", -1.156250, 1.000000, -0.525781, 0.000f, 90.000f, 0.000f, 0.343750f, 0.343750f, 0.015625f),
            part("COOKED_BEEF", -0.781250, 1.000000, -0.525781, 0.000f, 90.000f, 0.000f, 0.343750f, 0.343750f, 0.015625f),
            part("BLACK_CONCRETE", -1.468750, 0.937500, -0.895312, 0.000f, 0.000f, 0.000f, 0.062500f, 0.067187f, 0.781250f)
        ));
    }
    private static SmpFurnitureModel outdoor_beach_towel() {
        return model("outdoor_beach_towel", "beach_towel", FurnitureDefinition.Category.OUTDOOR, "BLUE_CARPET", 220, 1.937500, 0.250000, List.of(
            part("SIT", 0.015625, 0.031250, -1.018750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("CONCRETE", -1.000000, 0.000000, -1.937500, 5.000f, 0.000f, 0.000f, 1.937500f, 0.015625f, 1.873125f),
            part("SNOW_BLOCK", -0.187500, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", -0.375000, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", -0.562500, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", -0.750000, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", -0.937500, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", 0.000000, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", 0.187500, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", 0.375000, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", 0.562500, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f),
            part("SNOW_BLOCK", 0.750000, 0.000625, -1.938125, 5.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.875000f)
        ));
    }
    private static SmpFurnitureModel outdoor_beach_umbrella() {
        return model("outdoor_beach_umbrella", "beach_umbrella", FurnitureDefinition.Category.OUTDOOR, "STICK", 220, 3.000000, 3.000000, List.of(
            part("STRIPPED_OAK_WOOD", -0.625000, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.250000f, 0.125000f, 0.375000f),
            part("STRIPPED_OAK_WOOD", -0.750000, 0.000000, -0.625000, 0.000f, 0.000f, 0.000f, 0.500000f, 0.062500f, 0.250000f),
            part("WHITE_WOOL", -0.875000, 2.875000, -0.875000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -0.875000, 2.750000, -1.625000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -0.875000, 2.625000, -2.000000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.375000f),
            part("WHITE_WOOL", -0.125000, 2.750000, -0.875000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -0.875000, 2.750000, -0.125000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -1.625000, 2.750000, -0.875000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -0.875000, 2.625000, 0.625000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.375000f),
            part("WHITE_WOOL", 0.625000, 2.625000, -0.875000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -2.000000, 2.625000, -0.875000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -0.125000, 2.687500, -0.125000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -1.625000, 2.687500, -0.125000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -1.625000, 2.687500, -1.625000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -0.125000, 2.687500, -1.625000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -2.000000, 2.562500, -1.625000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", -1.625000, 2.562500, -2.000000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.375000f),
            part("WHITE_WOOL", -0.125000, 2.562500, -2.000000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.375000f),
            part("WHITE_WOOL", -1.625000, 2.562500, 0.625000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.375000f),
            part("WHITE_WOOL", -0.125000, 2.562500, 0.625000, 0.000f, 0.000f, 0.000f, 0.750000f, 0.125000f, 0.375000f),
            part("WHITE_WOOL", -2.000000, 2.562500, -0.125000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", 0.625000, 2.562500, -0.125000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.750000f),
            part("WHITE_WOOL", 0.625000, 2.562500, -1.625000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.125000f, 0.750000f),
            part("STRIPPED_OAK_WOOD", -0.343750, 2.312500, -0.531250, 0.000f, 0.000f, -30.000f, 0.062500f, 0.625000f, 0.062500f),
            part("STRIPPED_OAK_WOOD", -0.718750, 2.312500, -0.531250, 0.000f, 0.000f, 30.000f, 0.062500f, 0.625000f, 0.062500f),
            part("STRIPPED_OAK_WOOD", -0.531250, 2.312500, -0.343750, 0.000f, 30.000f, 0.000f, 0.062500f, 0.625000f, 0.062500f),
            part("STRIPPED_OAK_WOOD", -0.531250, 2.312500, -0.718750, 0.000f, -30.000f, 0.000f, 0.062500f, 0.625000f, 0.062500f),
            part("STRIPPED_OAK_WOOD", -0.562500, 0.000000, -0.562500, 0.000f, 0.000f, 0.000f, 0.125000f, 2.875000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_bench() {
        return model("outdoor_bench", "bench", FurnitureDefinition.Category.OUTDOOR, "OAK_STAIRS", 220, 1.937500, 1.375000, List.of(
            part("SIT", -1.046875, 0.656250, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("STRIPPED_OAK_WOOD", -1.968750, 0.500000, -0.421875, 0.000f, 0.000f, 0.000f, 1.937500f, 0.125000f, 0.218750f),
            part("OAK_WOOD", -1.906250, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.500000f, 0.187500f),
            part("STRIPPED_OAK_WOOD", -1.968750, 0.500000, -0.893750, 0.000f, 0.000f, 0.000f, 1.937500f, 0.125000f, 0.218750f),
            part("OAK_WOOD", -1.906250, 0.000000, -0.218750, 0.000f, 0.000f, 0.000f, 0.125000f, 1.375000f, 0.187500f),
            part("STRIPPED_OAK_WOOD", -1.968750, 0.500000, -0.656250, 0.000f, 0.000f, 0.000f, 1.937500f, 0.125000f, 0.218750f),
            part("STRIPPED_OAK_WOOD", -1.906250, 0.125000, -0.687500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.468750f),
            part("OAK_WOOD", -0.218750, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.500000f, 0.187500f),
            part("STRIPPED_OAK_WOOD", -0.218750, 0.125000, -0.687500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.468750f),
            part("OAK_WOOD", -0.218750, 0.000000, -0.218750, 0.000f, 0.000f, 0.000f, 0.125000f, 1.375000f, 0.187500f),
            part("STRIPPED_OAK_WOOD", -1.781250, 0.593750, -0.187500, 0.000f, 0.000f, 0.000f, 1.562500f, 0.187500f, 0.125000f),
            part("STRIPPED_OAK_WOOD", -1.781250, 0.843750, -0.187500, 0.000f, 0.000f, 0.000f, 1.562500f, 0.187500f, 0.125000f),
            part("STRIPPED_OAK_WOOD", -1.781250, 1.093750, -0.187500, 0.000f, 0.000f, 0.000f, 1.562500f, 0.187500f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_cabinet() {
        return model("outdoor_cabinet", "cabinet", FurnitureDefinition.Category.OUTDOOR, "IRON_BLOCK", 220, 1.000000, 1.000000, List.of(
            part("IRON_BLOCK", -1.000000, 0.125000, -0.937500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.812500f, 0.937500f),
            part("STRIPPED_OAK_WOOD", -0.809375, 0.406250, -0.950000, 0.000f, 0.000f, 0.000f, 0.625000f, 0.031250f, 0.012500f),
            part("STRIPPED_OAK_WOOD", -0.809375, 0.781250, -0.950000, 0.000f, 0.000f, 0.000f, 0.625000f, 0.031250f, 0.012500f),
            part("STRIPPED_OAK_WOOD", -1.000000, 0.937500, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.062500f, 1.000000f),
            part("STRIPPED_OAK_WOOD", -1.000000, 0.875000, -0.943750, 0.000f, 0.000f, 0.000f, 1.000000f, 0.062500f, 0.010000f),
            part("STRIPPED_OAK_WOOD", -0.937500, 0.562500, -0.943750, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.010000f),
            part("STRIPPED_OAK_WOOD", -0.062500, 0.125000, -0.943750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.750000f, 0.010000f),
            part("STRIPPED_OAK_WOOD", -1.000000, 0.125000, -0.943750, 0.000f, 0.000f, 0.000f, 0.062500f, 0.750000f, 0.010000f),
            part("STRIPPED_OAK_WOOD", -0.937500, 0.125000, -0.943750, 0.000f, 0.000f, 0.000f, 0.875000f, 0.062500f, 0.010000f),
            part("IRON_BLOCK", -0.187500, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("IRON_BLOCK", -0.937500, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("IRON_BLOCK", -0.187500, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("IRON_BLOCK", -0.937500, 0.000000, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_cart() {
        return model("outdoor_cart", "cart", FurnitureDefinition.Category.OUTDOOR, "POLISHED_ANDESITE", 220, 1.703125, 0.750000, List.of(
            part("MUDDY_MANGROVE_ROOTS", -0.563125, 0.000000, -0.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.375000f, 0.125000f),
            part("MUDDY_MANGROVE_ROOTS", -0.561875, 0.125000, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.375000f),
            part("MUDDY_MANGROVE_ROOTS", -0.562500, 0.062500, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.250000f, 0.250000f),
            part("POLISHED_ANDESITE", -0.250000, 0.000000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.250000f, 0.125000f),
            part("POLISHED_ANDESITE", -0.187500, 0.250000, -0.828125, 0.000f, 0.000f, 0.000f, 0.125000f, 0.500000f, 1.062500f),
            part("POLISHED_ANDESITE", -0.937500, 0.250000, -0.828125, 0.000f, 0.000f, 0.000f, 0.125000f, 0.500000f, 1.062500f),
            part("POLISHED_ANDESITE", -0.812500, 0.250000, 0.109375, 0.000f, 0.000f, 0.000f, 0.625000f, 0.500000f, 0.125000f),
            part("POLISHED_ANDESITE", -0.812500, 0.250000, -0.828125, 0.000f, 0.000f, 0.000f, 0.625000f, 0.500000f, 0.125000f),
            part("POLISHED_ANDESITE", -0.875000, 0.000000, -0.031250, 0.000f, 0.000f, 0.000f, 0.125000f, 0.250000f, 0.125000f),
            part("GREEN_TERRACOTTA", -0.312500, 0.625000, 0.234375, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.468750f),
            part("GREEN_TERRACOTTA", -0.812500, 0.625000, 0.234375, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.468750f),
            part("DIRT", -0.875000, 0.500000, -0.765625, 0.000f, 0.000f, 0.000f, 0.750000f, 0.062500f, 0.937500f),
            part("POLISHED_ANDESITE", -0.812500, 0.250000, -0.703125, 0.000f, 0.000f, 0.000f, 0.625000f, 0.062500f, 0.812500f),
            part("GREEN_TERRACOTTA", -0.625000, 0.125000, -0.875000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.125000f),
            part("GREEN_TERRACOTTA", -0.437500, 0.125000, -0.875000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_chair() {
        return model("outdoor_chair", "chair", FurnitureDefinition.Category.OUTDOOR, "PALE_OAK_STAIRS", 220, 0.922549, 1.525000, List.of(
            part("SIT", -0.562500, 0.734375, -0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.010000f, 0.010000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.812500, 0.656250, -0.812500, 0.000f, 0.000f, 0.000f, 0.625000f, 0.046875f, 0.500000f),
            part("PALE_OAK_WOOD", -0.837438, -0.056250, -0.493750, 0.000f, -45.000f, 0.000f, 0.112500f, 0.806250f, 0.112500f),
            part("PALE_OAK_WOOD", -0.875063, 0.562500, -0.875000, 0.000f, 0.000f, 0.000f, 0.750625f, 0.093750f, 0.750000f),
            part("PALE_OAK_WOOD", -0.875000, 0.650000, -0.250000, 0.000f, 5.000f, 0.000f, 0.062500f, 0.818750f, 0.125000f),
            part("PALE_OAK_WOOD", -0.187500, 0.650000, -0.250000, 0.000f, 5.000f, 0.000f, 0.062500f, 0.818750f, 0.125000f),
            part("PALE_OAK_WOOD", -0.837500, -0.000024, -0.758701, 0.000f, -45.000f, 0.000f, 0.112500f, 0.112500f, 0.806250f),
            part("PALE_OAK_WOOD", -0.281312, -0.056250, -0.493750, 0.000f, -45.000f, 0.000f, 0.112500f, 0.806250f, 0.112500f),
            part("PALE_OAK_WOOD", -0.281250, -0.000024, -0.758701, 0.000f, -45.000f, 0.000f, 0.112500f, 0.112500f, 0.806250f),
            part("PALE_OAK_WOOD", -0.812500, 1.343750, -0.250000, 0.000f, 5.000f, 0.000f, 0.625000f, 0.062500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.812500, 1.218750, -0.250000, 0.000f, 5.000f, 0.000f, 0.625000f, 0.062500f, 0.125000f),
            part("PALE_OAK_WOOD", -0.812500, 1.093750, -0.250000, 0.000f, 5.000f, 0.000f, 0.625000f, 0.062500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.812500, 0.968750, -0.250000, 0.000f, 5.000f, 0.000f, 0.625000f, 0.062500f, 0.125000f),
            part("PALE_OAK_WOOD", -0.812500, 0.843750, -0.250000, 0.000f, 5.000f, 0.000f, 0.625000f, 0.062500f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.812500, 0.718750, -0.250000, 0.000f, 5.000f, 0.000f, 0.625000f, 0.062500f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_lawn_mower() {
        return model("outdoor_lawn_mower", "lawn_mower", FurnitureDefinition.Category.OUTDOOR, "SHORT_GRASS", 220, 1.375000, 1.343750, List.of(
            part("BLACK_CONCRETE_POWDER", -0.937500, 0.062500, -0.984375, 0.000f, 0.000f, 0.000f, 0.875000f, 0.250000f, 0.187500f),
            part("BLACK_CONCRETE_POWDER", -0.875000, 0.312500, -0.984375, 0.000f, 0.000f, 0.000f, 0.750000f, 0.062500f, 0.250000f),
            part("RED_SHULKER_BOX", -0.937500, 0.062500, -0.796875, 0.000f, 0.000f, 0.000f, 0.875000f, 0.250000f, 0.812500f),
            part("BLACK_CONCRETE_POWDER", -0.062500, 0.000000, -0.796875, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.125000f),
            part("BLACK_CONCRETE_POWDER", -0.062500, 0.125000, -0.921875, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.375000f),
            part("BLACK_CONCRETE_POWDER", -1.000000, 0.000000, -0.796875, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.125000f),
            part("BLACK_CONCRETE_POWDER", -1.000000, 0.125000, -0.921875, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.375000f),
            part("BLACK_CONCRETE_POWDER", -1.000625, 0.063125, -0.859375, 0.000f, 0.000f, 0.000f, 1.001250f, 0.248750f, 0.250000f),
            part("RED_SHULKER_BOX", -0.750000, 0.125000, -0.609375, 0.000f, 0.000f, 0.000f, 0.500000f, 0.375000f, 0.562500f),
            part("IRON_BLOCK", -0.125000, 0.093750, -0.093750, 0.000f, 20.000f, 0.000f, 0.057812f, 1.250000f, 0.125000f),
            part("BLACK_CONCRETE_POWDER", -0.875000, 0.062500, 0.015625, 0.000f, 0.000f, 0.000f, 0.750000f, 0.375000f, 0.375000f),
            part("IRON_BLOCK", -0.932813, 0.093750, -0.093750, 0.000f, 20.000f, 0.000f, 0.057812f, 1.250000f, 0.125000f),
            part("IRON_BLOCK", -0.875000, 1.218750, -0.062500, 0.000f, 20.000f, 0.000f, 0.750000f, 0.125000f, 0.062500f),
            part("IRON_BLOCK", -0.875000, 0.843750, -0.062500, 0.000f, 20.000f, 0.000f, 0.750000f, 0.062500f, 0.062500f),
            part("BLACK_CONCRETE_POWDER", -0.062500, 0.125000, -0.359375, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.375000f),
            part("BLACK_CONCRETE_POWDER", -0.062500, 0.000000, -0.234375, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.125000f),
            part("BLACK_CONCRETE_POWDER", -1.000625, 0.063125, -0.296875, 0.000f, 0.000f, 0.000f, 1.001250f, 0.248750f, 0.250000f),
            part("BLACK_CONCRETE_POWDER", -1.000000, 0.125000, -0.359375, 0.000f, 0.000f, 0.000f, 0.062500f, 0.125000f, 0.375000f),
            part("BLACK_CONCRETE_POWDER", -1.000000, 0.000000, -0.234375, 0.000f, 0.000f, 0.000f, 0.062500f, 0.375000f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_outdoor_lights() {
        return model("outdoor_outdoor_lights", "outdoor_lights", FurnitureDefinition.Category.OUTDOOR, "TORCH", 220, 0.250000, 0.875000, List.of(
            part("BLACK_CONCRETE", -0.593750, 0.000000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.437500f, 0.187500f),
            part("YELLOW_WOOL", -0.587500, 0.437500, -0.587500, 0.000f, 0.000f, 0.000f, 0.175000f, 0.375000f, 0.175000f),
            part("BLACK_CONCRETE", -0.593750, 0.806250, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.068750f, 0.187500f),
            part("BLACK_CONCRETE", -0.418750, 0.437500, -0.418750, 0.000f, 0.000f, 0.000f, 0.012500f, 0.368750f, 0.012500f),
            part("BLACK_CONCRETE", -0.593750, 0.437500, -0.418750, 0.000f, 0.000f, 0.000f, 0.012500f, 0.368750f, 0.012500f),
            part("BLACK_CONCRETE", -0.593750, 0.437500, -0.593750, 0.000f, 0.000f, 0.000f, 0.012500f, 0.368750f, 0.012500f),
            part("BLACK_CONCRETE", -0.418750, 0.437500, -0.593750, 0.000f, 0.000f, 0.000f, 0.012500f, 0.368750f, 0.012500f)
        ));
    }
    private static SmpFurnitureModel outdoor_plant() {
        return model("outdoor_plant", "plant", FurnitureDefinition.Category.OUTDOOR, "FLOWERING_AZALEA_LEAVES", 220, 1.000000, 0.868750, List.of(
            part("STRIPPED_OAK_WOOD", -0.937500, 0.125000, -0.750000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.312500f, 0.500000f),
            part("STRIPPED_OAK_WOOD", -0.250000, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("STRIPPED_OAK_WOOD", -0.250000, 0.000000, -0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("STRIPPED_OAK_WOOD", -0.875000, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("STRIPPED_OAK_WOOD", -0.875000, 0.000000, -0.437500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.125000f),
            part("YELLOW_TERRACOTTA", -1.000000, 0.437500, -0.812500, 0.000f, 0.000f, 0.000f, 1.000000f, 0.062500f, 0.062500f),
            part("YELLOW_TERRACOTTA", -1.000000, 0.437500, -0.250000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.062500f, 0.062500f),
            part("YELLOW_TERRACOTTA", -0.062500, 0.437500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.500000f),
            part("YELLOW_TERRACOTTA", -1.000000, 0.437500, -0.750000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.062500f, 0.500000f),
            part("FLOWERING_AZALEA_LEAVES", -0.931250, 0.500000, -0.750000, 0.000f, 0.000f, 0.000f, 0.862500f, 0.368750f, 0.500000f),
            part("DIRT", -0.937500, 0.431250, -0.750000, 0.000f, 0.000f, 0.000f, 0.875000f, 0.025000f, 0.500000f),
            part("FLOWERING_AZALEA_LEAVES", -0.868750, 0.562500, -0.687500, 0.000f, 0.000f, 0.000f, 0.737500f, 0.243750f, 0.375000f)
        ));
    }
    private static SmpFurnitureModel outdoor_pool_chair() {
        return model("outdoor_pool_chair", "pool_chair", FurnitureDefinition.Category.OUTDOOR, "PRISMARINE_BRICK_STAIRS", 220, 1.809914, 1.435387, List.of(
            part("SIT", -0.546875, 0.500000, -0.768750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.968750, 0.375000, -0.453125, 0.000f, 0.000f, 0.000f, 0.937500f, 0.125000f, 0.192188f),
            part("PALE_OAK_WOOD", -0.921875, 0.000000, -0.500000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.375000f, 0.187500f),
            part("PALE_OAK_WOOD", -0.203750, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.126250f, 0.375000f, 0.125000f),
            part("PALE_OAK_WOOD", -0.203125, 0.000000, -0.500000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.375000f, 0.187500f),
            part("PALE_OAK_WOOD", -0.156250, 0.687500, -0.968750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.779687f),
            part("PALE_OAK_WOOD", -0.156250, 0.500000, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.125000f),
            part("PALE_OAK_WOOD", -0.968750, 0.687500, -0.968750, 0.000f, 0.000f, 0.000f, 0.125000f, 0.062500f, 0.779687f),
            part("PALE_OAK_WOOD", -0.968750, 0.500000, -0.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.187500f, 0.125000f),
            part("PALE_OAK_WOOD", -0.921875, 0.000000, -1.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.375000f, 0.187500f),
            part("PALE_OAK_WOOD", -0.203125, 0.000000, -1.937500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.375000f, 0.187500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.203125, 0.250000, -1.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 1.250000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.203125, 0.062500, -1.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 1.250000f),
            part("PALE_OAK_WOOD", -0.187500, 0.560387, -0.302586, 0.000f, 22.500f, 0.000f, 0.125000f, 0.875000f, 0.125000f),
            part("PALE_OAK_WOOD", -0.937500, 0.560387, -0.302586, 0.000f, 22.500f, 0.000f, 0.125000f, 0.875000f, 0.125000f),
            part("PALE_OAK_WOOD", -0.922500, 0.000000, -0.937500, 0.000f, 0.000f, 0.000f, 0.126250f, 0.375000f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.921875, 0.062500, -1.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 1.250000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.921875, 0.250000, -1.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 1.250000f),
            part("GLAZED_TERRACOTTA", -0.781250, 0.500000, -1.675000, 0.000f, 0.000f, 0.000f, 0.562500f, 0.012500f, 1.254687f),
            part("GLAZED_TERRACOTTA", -0.781250, 0.560387, -0.315086, 0.000f, 22.500f, 0.000f, 0.562500f, 0.640625f, 0.012500f),
            part("STRIPPED_PALE_OAK_WOOD", -0.968750, 0.375000, -1.253125, 0.000f, 0.000f, 0.000f, 0.937500f, 0.125000f, 0.254688f),
            part("STRIPPED_PALE_OAK_WOOD", -0.968750, 0.375000, -0.718750, 0.000f, 0.000f, 0.000f, 0.937500f, 0.125000f, 0.254688f),
            part("STRIPPED_PALE_OAK_WOOD", -0.968750, 0.375000, -0.987500, 0.000f, 0.000f, 0.000f, 0.937500f, 0.125000f, 0.254687f),
            part("STRIPPED_PALE_OAK_WOOD", -0.968750, 0.375000, -1.518750, 0.000f, 0.000f, 0.000f, 0.937500f, 0.125000f, 0.254688f),
            part("STRIPPED_PALE_OAK_WOOD", -0.968750, 0.375000, -1.987500, 0.000f, 0.000f, 0.000f, 0.937500f, 0.125000f, 0.192187f),
            part("STRIPPED_PALE_OAK_WOOD", -0.968750, 0.375000, -1.784375, 0.000f, 0.000f, 0.000f, 0.937500f, 0.125000f, 0.254687f),
            part("STRIPPED_PALE_OAK_WOOD", -0.812500, 0.622887, -0.302586, 0.000f, 22.500f, 0.000f, 0.625000f, 0.203125f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.812500, 0.872887, -0.302586, 0.000f, 22.500f, 0.000f, 0.625000f, 0.203125f, 0.125000f),
            part("STRIPPED_PALE_OAK_WOOD", -0.812500, 1.122887, -0.302586, 0.000f, 22.500f, 0.000f, 0.625000f, 0.203125f, 0.125000f)
        ));
    }
    private static SmpFurnitureModel outdoor_swing() {
        return model("outdoor_swing", "swing", FurnitureDefinition.Category.OUTDOOR, "CHAIN", 220, 2.937500, 2.887500, List.of(
            part("SIT", -0.484375, 0.687500, -0.518750, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("STRIPPED_OAK_WOOD", -1.937500, 0.065625, 0.781250, 0.000f, -27.500f, 0.000f, 0.187500f, 2.887500f, 0.187500f),
            part("DEEPSLATE_TILES", -1.750000, 2.500000, -0.562500, 0.000f, 0.000f, 0.000f, 2.500000f, 0.125000f, 0.125000f),
            part("STRIPPED_OAK_WOOD", -1.928125, 0.065625, -1.968750, 0.000f, 27.500f, 0.000f, 0.178125f, 2.887500f, 0.187500f),
            part("STRIPPED_OAK_WOOD", 0.750000, 0.065625, -1.968750, 0.000f, 27.500f, 0.000f, 0.173437f, 2.887500f, 0.187500f),
            part("STRIPPED_OAK_WOOD", 0.750000, 0.065625, 0.781250, 0.000f, -27.500f, 0.000f, 0.187500f, 2.887500f, 0.187500f),
            part("CYAN_CONCRETE", -1.000000, 0.562500, -0.750000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 0.500000f),
            part("CHAIN", -1.109375, 0.687500, -0.734375, 0.000f, 0.000f, 0.000f, 0.468750f, 1.812500f, 0.468750f),
            part("CHAIN", -0.359375, 0.687500, -0.734375, 0.000f, 0.000f, 0.000f, 0.468750f, 1.812500f, 0.468750f)
        ));
    }
    private static SmpFurnitureModel outdoor_table_bench() {
        return model("outdoor_table_bench", "table_bench", FurnitureDefinition.Category.OUTDOOR, "JUNGLE_STAIRS", 220, 2.875000, 0.562500, List.of(
            part("SIT", -0.484375, 0.500000, -0.550000, 0.000f, 0.000f, 0.000f, 0.010000f, 0.010000f, 0.010000f),
            part("STRIPPED_OAK_WOOD", -1.937500, 0.437500, -0.484375, 0.000f, 0.000f, 0.000f, 2.875000f, 0.125000f, 0.271875f),
            part("STRIPPED_OAK_WOOD", 0.687500, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.437500f, 0.500000f),
            part("STRIPPED_OAK_WOOD", -1.812500, 0.000000, -0.750000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.437500f, 0.500000f),
            part("STRIPPED_OAK_WOOD", -1.937500, 0.437500, -0.781250, 0.000f, 0.000f, 0.000f, 2.875000f, 0.125000f, 0.265625f)
        ));
    }
    private static SmpFurnitureModel outdoor_table_big() {
        return model("outdoor_table_big", "table_big", FurnitureDefinition.Category.OUTDOOR, "STRIPPED_JUNGLE_WOOD", 220, 2.875000, 1.000000, List.of(
            part("STRIPPED_OAK_WOOD", -1.937500, 0.875000, -2.000000, 0.000f, 0.000f, 0.000f, 2.875000f, 0.125000f, 0.614062f),
            part("STRIPPED_OAK_WOOD", -1.750000, 0.000000, -0.375000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.875000f, 0.250000f),
            part("STRIPPED_OAK_WOOD", -1.750000, 0.000000, -1.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.875000f, 0.250000f),
            part("STRIPPED_OAK_WOOD", 0.625000, 0.000000, -0.375000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.875000f, 0.250000f),
            part("STRIPPED_OAK_WOOD", 0.625000, 0.000000, -1.875000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.875000f, 0.250000f),
            part("STRIPPED_OAK_WOOD", -1.687500, 0.187500, -1.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 1.250000f),
            part("STRIPPED_OAK_WOOD", 0.625000, 0.625000, -1.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 1.250000f),
            part("STRIPPED_OAK_WOOD", 0.625000, 0.187500, -1.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 1.250000f),
            part("STRIPPED_OAK_WOOD", -1.687500, 0.625000, -1.625000, 0.000f, 0.000f, 0.000f, 0.062500f, 0.250000f, 1.250000f),
            part("STRIPPED_OAK_WOOD", -1.937500, 0.875000, -0.676562, 0.000f, 0.000f, 0.000f, 2.875000f, 0.125000f, 0.676562f),
            part("STRIPPED_OAK_WOOD", -1.937500, 0.875000, -1.357813, 0.000f, 0.000f, 0.000f, 2.875000f, 0.125000f, 0.646875f)
        ));
    }
    private static SmpFurnitureModel outdoor_table_small() {
        return model("outdoor_table_small", "table_small", FurnitureDefinition.Category.OUTDOOR, "STRIPPED_PALE_OAK_WOOD", 220, 1.000000, 1.000000, List.of(
            part("STRIPPED_PALE_OAK_WOOD", -1.000000, 0.875000, -1.000000, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 0.318750f),
            part("PALE_OAK_WOOD", -0.250000, 0.859375, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.000000f),
            part("PALE_OAK_WOOD", -0.875000, 0.859375, -1.000000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.015625f, 1.000000f),
            part("STRIPPED_PALE_OAK_WOOD", -1.000000, 0.875000, -0.318750, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 0.318750f),
            part("STRIPPED_PALE_OAK_WOOD", -1.000000, 0.875000, -0.659375, 0.000f, 0.000f, 0.000f, 1.000000f, 0.125000f, 0.318750f),
            part("PALE_OAK_WOOD", -0.593750, 0.125000, -0.593750, 0.000f, 0.000f, 0.000f, 0.187500f, 0.750000f, 0.187500f),
            part("PALE_OAK_WOOD", -0.656250, 0.062500, -0.656250, 0.000f, 0.000f, 0.000f, 0.312500f, 0.062500f, 0.312500f),
            part("PALE_OAK_WOOD", -0.781250, 0.000000, -0.781250, 0.000f, 0.000f, 0.000f, 0.562500f, 0.062500f, 0.562500f)
        ));
    }
    private static SmpFurnitureModel outdoor_watering_can() {
        return model("outdoor_watering_can", "watering_can", FurnitureDefinition.Category.OUTDOOR, "LIME_CONCRETE", 220, 1.173884, 0.687500, List.of(
            part("LIME_SHULKER_BOX", -0.750000, 0.000000, -0.687500, 0.000f, 0.000f, 0.000f, 0.500000f, 0.375000f, 0.625000f),
            part("COAL_BLOCK", -0.687500, 0.375000, -0.500000, 0.000f, 0.000f, 0.000f, 0.375000f, 0.010000f, 0.312500f),
            part("LIME_SHULKER_BOX", -0.562500, 0.375000, -0.187500, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("LIME_SHULKER_BOX", -0.562500, 0.375000, -0.625000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.312500f, 0.125000f),
            part("LIME_SHULKER_BOX", -0.562500, 0.562500, -0.500000, 0.000f, 0.000f, 0.000f, 0.125000f, 0.125000f, 0.312500f),
            part("LIME_SHULKER_BOX", -0.562500, 0.171875, -1.171875, 0.000f, 50.000f, 0.000f, 0.125000f, 0.125000f, 0.375000f),
            part("COAL_BLOCK", -0.609375, 0.125000, -1.234375, 0.000f, 50.000f, 0.000f, 0.218750f, 0.218750f, 0.062500f),
            part("BLUE_ICE", -0.453125, 0.283644, -1.236384, 0.000f, 50.000f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("BLUE_ICE", -0.453125, 0.158644, -1.236384, 0.000f, 50.000f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("BLUE_ICE", -0.578125, 0.158644, -1.236384, 0.000f, 50.000f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("BLUE_ICE", -0.578125, 0.283644, -1.236384, 0.000f, 50.000f, 0.000f, 0.031250f, 0.031250f, 0.010000f),
            part("BLUE_ICE", -0.515625, 0.221144, -1.236384, 0.000f, 50.000f, 0.000f, 0.031250f, 0.031250f, 0.010000f)
        ));
    }
    private static SmpFurnitureModel model(String id, String name, FurnitureDefinition.Category category, String preview, double price, double width, double height, List<SmpModelPart> parts) {
        return new SmpFurnitureModel(id, name, category, material(preview), price, parts, width, height, SmpFurnitureModel.PlacementType.FLOOR, SmpFurnitureModel.InteractionType.NONE);
    }
    private static SmpModelPart part(String material, double x, double y, double z, float yaw, float pitch, float roll, float scaleX, float scaleY, float scaleZ) {
        Material value = material(material);
        float scale = Math.max(scaleX, Math.max(scaleY, scaleZ));
        return new SmpModelPart(value, value.isBlock() ? SmpModelPart.EntityType.BLOCK_DISPLAY : SmpModelPart.EntityType.ITEM_DISPLAY, EquipmentSlot.HEAD, x, y, z, yaw, pitch, roll, scale, scaleX, scaleY, scaleZ, true, false, true, false, false, false);
    }
    private static Material material(String name) {
        Material value = Material.matchMaterial(name);
        return value == null || value.isAir() ? Material.OAK_PLANKS : value;
    }
}