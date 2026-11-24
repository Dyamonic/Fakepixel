package me.outerspace.thepit.registry;

import java.util.Map;
import java.util.Optional;

public final class MessageRegistry extends RegistryNode {
    public MessageRegistry(Map<String, Object> values) { super(values); }
    public Optional<String> getMessage(String id) { return Optional.ofNullable(getString("messages." + id)); }
    public String message(String id, String fallback) { return getString("messages." + id, fallback); }
    public String prefix() { return message("prefix", "&bThe Pit &8» &7"); }
}
