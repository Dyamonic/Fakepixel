package me.outerspace.thepit.registry.definition;

import me.outerspace.thepit.registry.RegistryNode;
import java.util.List;

public record ShopItemDefinition(String id, String displayName, String material, int amount, short data,
                                 int coinPrice, int goldPrice, int slot, List<String> enchants,
                                 List<String> commands, RegistryNode values) {
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getMaterial() { return material; }
    public int getAmount() { return amount; }
    public short getData() { return data; }
    public int getCoinPrice() { return coinPrice; }
    public int getGoldPrice() { return goldPrice; }
    public int getSlot() { return slot; }
    public List<String> getEnchants() { return enchants; }
    public List<String> getCommands() { return commands; }
    public RegistryNode getValues() { return values; }
}
