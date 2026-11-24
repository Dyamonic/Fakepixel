package club.abelix.core.app.api.event;

/**
 * Events that implement this indicate that they may be cancelled and thus
 * prevented from happening.
 */
public interface Cancellable {

    /**
     * Get whether or not this event is cancelled.
     *
     * @return the cancelled enums of this event
     */
    boolean isCancelled();

    /**
     * Sets the cancelled enums of this event.
     *
     * @param cancel the enums to set
     */
    void setCancelled(boolean cancel);

}
