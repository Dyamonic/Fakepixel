package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class AuctionHouseDefaults {
    private AuctionHouseDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("auctionhouse.enabled", true);
        root.set("auctionhouse.max-listings", 8);
        root.set("auctionhouse.notifications.buyer-bought", "&6Auction House &8> &aYou bought &f%item% &afor &e%price% coins &afrom &f%seller%&a.");
        root.set("auctionhouse.notifications.seller-sold", "&6Auction House &8> &aYour &f%item% &asold to &f%buyer% &afor &e%price% coins&a.");
    }
}
