package club.abelix.core.app.api.event;

/**
 * Dummy class which all callable event must extend.
 */
public abstract class Event {

    /**
     * Method called after this event has been dispatched to all handlers.
     */
    public void postCall() {

    }

}
