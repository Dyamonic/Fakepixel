package club.abelix.commons.util.database;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * @author WhileInside
 */
public class IntArrayTimeSerializer {

    private final ToIntFunction<String> toIndex;
    private final IntFunction<String> fromIndex;

    public IntArrayTimeSerializer(ToIntFunction<String> toIndex, IntFunction<String> fromIndex) {
        this.toIndex = toIndex;
        this.fromIndex = fromIndex;
    }

    private int now() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public String serialize(int[] in) {
        StringBuilder builder = new StringBuilder();
        int now = now();

        for (int i = 0; i < in.length; i++) {
            int expire = in[i];

            if (expire < now) {
                continue; // не стоит заносить сюда истёкшие значения
            }

            builder.append(fromIndex.apply(i)).append('=').append(expire).append(';');
        }

        return builder.length() == 0 ? null : builder.toString();
    }

    public void deserialize(String text, int[] out) {
        StringBuilder objectNameBuilder = new StringBuilder();
        StringBuilder expireBuilder = new StringBuilder();

        boolean part = false;

        for (char character : text.toCharArray()) {
            if (!part) {
                if (character == '=') {
                    part = true;
                    continue;
                }

                objectNameBuilder.append(character);
            } else {
                if (character == ';') {
                    String objectName = objectNameBuilder.toString();
                    int expire = Integer.parseInt(expireBuilder.toString());

                    out[toIndex.applyAsInt(objectName)] = expire;

                    objectNameBuilder.delete(0, objectNameBuilder.length());
                    expireBuilder.delete(0, expireBuilder.length());

                    part = false;
                    continue;
                }

                expireBuilder.append(character);
            }
        }
    }

}
