package club.abelix.api.base.util.scoreboard.scroller;


import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultScroller implements Scroller {

    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

    private int currentPosition = 0;

    protected DefaultScroller(List<String> lines) {
        this.lines.addAll(lines);
    }

    public DefaultScroller(@NonNull String name) {
        lines.addAll(getAnimation(name));
    }

    @Override
    public String getNext(boolean recursively) {
        ++currentPosition;

        if (recursively && currentPosition >= lines.size()) currentPosition = 0;
        if (!hasNext()) throw new IllegalStateException("Scrolling has end.");

        return lines.get(currentPosition);
    }

    @Override
    public boolean hasNext() {
        return currentPosition <= lines.size();
    }

    public List<String> getAnimation(String name) {
        List<String> animation = new ArrayList<>();
        int position;

        for (position = 0; position < name.toCharArray().length; ++position) {
            StringBuilder builder = new StringBuilder(name);

            char c = builder.charAt(position);
            builder.deleteCharAt(position);
            builder.insert(position, "§b§l" + c + "§f§l");
            animation.add("§f§l" + builder);
        }

        for (position = 0; position < 15; ++position) {
            animation.add("§f§l" + name);
        }

        animation.add("§b§l" + name);
        animation.add("§f§l" + name);
        animation.add("§b§l" + name);
        animation.add("§f§l" + name);
        animation.add("§b§l" + name);
        animation.add("§f§l" + name);

        for (position = 0; position < 500; ++position) {
            animation.add("§b§l" + name);
        }

        return animation;
    }
}


