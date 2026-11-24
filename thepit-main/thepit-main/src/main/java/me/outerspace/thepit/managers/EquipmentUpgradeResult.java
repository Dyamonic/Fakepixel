package me.outerspace.thepit.managers;

public enum EquipmentUpgradeResult {
    EQUIPPED,
    REPLACED_STANDARD_ITEM,
    PLACED_IN_INVENTORY,
    ALREADY_HAS_ITEM,
    PROTECTED_ITEM_EQUIPPED,
    NO_SAFE_INVENTORY_SPACE,
    INVALID_ITEM,
    FAILED
}
