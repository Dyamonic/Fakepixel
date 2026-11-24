package club.abelix.api.base.util.scoreboard.scroller;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnimationScroller implements Scroller {

    private final String name;
    private final List<String> displayAnimation = Collections.synchronizedList(new ArrayList<>());
    private int d = 0;
    private int t = 0;
    private String currentDisplay;

    public AnimationScroller(@NonNull String name) {
        displayAnimation.addAll(getAnimation(name));

        this.name = name;
    }

    public static List<String> getAnimation(String displayName) {
        List<String> animation = new ArrayList<>();

        String displayLine = displayName + "  ";
        char[] displayInfoArray = displayLine.toCharArray();
        char[] displayWorkArray = new char[displayInfoArray.length];
        int slotTextSee = 0;

        for (char sym : displayInfoArray) {
            int slot = displayInfoArray.length - 1;

            for (int g = 0; g < getSizeCharArray(displayWorkArray); g++) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < displayInfoArray.length; i++) {
                    if (displayWorkArray[i] == 0) {
                        if (i == slot) {
                            if (i == slotTextSee) {
                                displayWorkArray[slotTextSee] = sym;
                                line.append(sym);

                                slotTextSee++;
                            } else {
                                line.append("§d§l").append(sym);
                                slot -= 1;
                            }
                        } else {
                            line.append(" ");
                        }
                    } else {
                        line.append(displayWorkArray[i]);
                    }
                }

                animation.add(" §8§l» §5§l" + line);
            }
        }

        return animation;
    }

    private static int getSizeCharArray(char[] array) {
        int i = 0;
        for (char arr : array) {
            if (arr == 0) {
                i++;
            }
        }

        return i;
    }

    @Override
    public String getNext(boolean recursively) {
        try {
            if (d == displayAnimation.size()) {
                if (t == 120) {
                    t = 0;
                    d = 0;
                }

                currentDisplay = " §8§l» §f§l" + name + "  ";
                t++;
            } else {
                currentDisplay = displayAnimation.get(d);
                ++d;
            }
        } catch (Exception ignored) {
        }

        return currentDisplay;
    }

    @Override
    public boolean hasNext() {
        return d != displayAnimation.size();
    }
}