package me.outerspace.thepit.model;

public enum Period {
    LIFETIME("lifetime"), MONTHLY("monthly"), WEEKLY("weekly"), DAILY("daily");
    private final String path;
    Period(String path) { this.path = path; }
    public String path() { return path; }
    public static Period from(String s) {
        for (Period p : values()) if (p.name().equalsIgnoreCase(s) || p.path.equalsIgnoreCase(s)) return p;
        return null;
    }
}
