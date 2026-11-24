package club.abelix.api.base.util.scoreboard.scroller;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakepixelDefaultScroller implements Scroller {

    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

    private int currentPosition = 0;

    protected FakepixelDefaultScroller(List<String> lines) {
        this.lines.addAll(lines);
    }

    public FakepixelDefaultScroller(@NonNull String name) {
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

    private List<String> getAnimation(String s) {
        String name = s.toUpperCase();
        List<String> animation = new ArrayList<>();

        String beginName = "";

        StringBuilder prefixBuilder = new StringBuilder("§f§l").append(beginName)
                .append("§6§l").append(name.charAt(0)).append("§e§l").append(name.substring(1));

        animation.add(prefixBuilder.toString());
        beginName += name.charAt(0);

        for (int i = 1; i < name.length(); i++) {
            prefixBuilder.setLength(0);
            prefixBuilder.append("§f§l").append(beginName).append("§6§l").append(name.charAt(i))
                    .append("§e§l").append(name.substring(i + 1));

            animation.add(prefixBuilder.toString());

            beginName += name.charAt(i);
        }

        for (int i = 0; i < 2; i++) {
            animation.add("§e§l" + name);
            animation.add("§f§l" + name);
            animation.add("§e§l" + name);
            animation.add("§f§l" + name);
        }

        for (int i = 0; i < 20; i++) {
            animation.add("§e§l" + name);
        }

        return animation;
    }
}

