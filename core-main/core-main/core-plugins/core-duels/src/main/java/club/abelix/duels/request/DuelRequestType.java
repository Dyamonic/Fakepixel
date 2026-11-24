package club.abelix.duels.request;

import club.abelix.commons.util.request.RequestType;

/**
 * @author artem
 */
public class DuelRequestType extends RequestType {

    public static final DuelRequestType INSTANCE = new DuelRequestType();

    private DuelRequestType() {
        super("duel", "/duel accept %s", "/duel reject %s", true);
    }
}
