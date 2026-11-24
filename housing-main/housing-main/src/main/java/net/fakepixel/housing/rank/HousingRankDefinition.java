package net.fakepixel.housing.rank;

import java.util.Collections;
import java.util.List;

public final class HousingRankDefinition {
    public enum Category { ADMIN_STAFF, STAFF, PLAYER }
    private final String id;
    private final Category category;
    private final List<String> permissions;
    public HousingRankDefinition(String id, Category category, List<String> permissions) {
        this.id = id; this.category = category;
        this.permissions = Collections.unmodifiableList(permissions);
    }
    public String getId() { return id; }
    public Category getCategory() { return category; }
    public List<String> getPermissions() { return permissions; }
}
