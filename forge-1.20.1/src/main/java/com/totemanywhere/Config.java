package com.totemanywhere;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SEARCH_CONTAINERS;
    public static final ForgeConfigSpec.BooleanValue SEARCH_ENDER_CHEST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SEARCH_CONTAINERS = b.comment("Buscar tótems dentro de shulkers, bundles, mochilas, etc.")
                .define("searchContainers", true);
        SEARCH_ENDER_CHEST = b.comment("Buscar tótems también en el ender chest del jugador.")
                .define("searchEnderChest", false);
        SPEC = b.build();
    }
}
