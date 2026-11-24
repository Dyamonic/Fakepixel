package club.abelix.api.base.util.scoreboard.scroller;

/**
 * @author WhileInside
 */
public interface Scroller {

    String getNext(boolean recursively);

    boolean hasNext();
}
