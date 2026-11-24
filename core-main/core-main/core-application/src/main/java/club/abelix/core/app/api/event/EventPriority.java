package club.abelix.core.app.api.event;

/**
 * Importance of the {@link org.greenrobot.eventbus.Subscribe}. When executing an Event, the handlers
 * are called in order of their Priority.
 */
public class EventPriority {

    public static final byte LOWEST = -64;
    public static final byte LOW = -32;
    public static final byte NORMAL = 0;
    public static final byte HIGH = 32;
    public static final byte HIGHEST = 64;

}
