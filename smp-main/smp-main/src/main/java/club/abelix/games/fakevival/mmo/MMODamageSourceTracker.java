package club.abelix.games.fakevival.mmo;

public final class MMODamageSourceTracker {
    private static final ThreadLocal<MMODamageSourceType> CURRENT = new ThreadLocal<>();

    private MMODamageSourceTracker() {
    }

    public static void runAs(MMODamageSourceType sourceType, Runnable runnable) {
        MMODamageSourceType previous = CURRENT.get();
        CURRENT.set(sourceType);
        try {
            runnable.run();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static MMODamageSourceType current() {
        MMODamageSourceType sourceType = CURRENT.get();
        return sourceType == null ? MMODamageSourceType.MELEE : sourceType;
    }
}
