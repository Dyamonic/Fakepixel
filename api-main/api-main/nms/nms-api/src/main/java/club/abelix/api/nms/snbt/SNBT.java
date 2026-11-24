package club.abelix.api.nms.snbt;

import club.abelix.api.nms.nbt.tag.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author WhileInside
 */
public class SNBT {

    public static Tag deserialize(String snbt) {
        try {
            return SNBTParser.parse(snbt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serialize(Tag tag) {
        return tag.toString();
    }

    private static class StringPointer {
        private final String value;
        private int index;

        public StringPointer(String value) {
            this.value = value;
        }

        public String parseSimpleString() {
            int oldIndex = index;
            while (hasNext() && isSimpleChar(currentChar())) {
                index++;
            }
            return value.substring(oldIndex, index);
        }

        public String parseQuotedString() throws ParseException {
            int oldIndex = ++index; //ignore beginning quotes
            StringBuilder sb = null;
            boolean escape = false;
            while (hasNext()) {
                char c = next();
                if (escape) {
                    if (c != '\\' && c != '"') {
                        throw parseException("invalid escape of '" + c + "'");
                    }
                    escape = false;
                } else {
                    if (c == '\\') { //escape
                        escape = true;
                        if (sb != null) {
                            continue;
                        }
                        sb = new StringBuilder(value.substring(oldIndex, index - 1));
                        continue;
                    }
                    if (c == '"') {
                        return sb == null ? value.substring(oldIndex, index - 1) : sb.toString();
                    }
                }
                if (sb != null) {
                    sb.append(c);
                }
            }
            throw parseException("missing end quote");
        }

        public boolean nextArrayElement() {
            skipWhitespace();
            if (hasNext() && currentChar() == ',') {
                index++;
                skipWhitespace();
                return true;
            }
            return false;
        }

        public void expectChar(char c) throws ParseException {
            skipWhitespace();
            boolean hasNext = hasNext();
            if (hasNext && currentChar() == c) {
                index++;
                return;
            }
            throw parseException("expected '" + c + "' but got " + (hasNext ? "'" + currentChar() + "'" : "EOF"));
        }

        public void skipWhitespace() {
            while (hasNext() && Character.isWhitespace(currentChar())) {
                index++;
            }
        }

        public boolean hasNext() {
            return index < value.length();
        }

        public boolean hasCharsLeft(int num) {
            return this.index + num < value.length();
        }

        public char currentChar() {
            return value.charAt(index);
        }

        public char next() {
            return value.charAt(index++);
        }

        public void skip(int offset) {
            index += offset;
        }

        public char lookAhead(int offset) {
            return value.charAt(index + offset);
        }

        private static boolean isSimpleChar(char c) {
            return c >= 'a' && c <= 'z'
                    || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '-'
                    || c == '+'
                    || c == '.'
                    || c == '_';
        }

        public ParseException parseException(String msg) {
            return new ParseException(msg, index);
        }
    }

    private static class SNBTParser {

        private static final Pattern
                FLOAT_LITERAL_PATTERN = Pattern.compile("^[-+]?(?:\\d+\\.?|\\d*\\.\\d+)(?:e[-+]?\\d+)?f$", Pattern.CASE_INSENSITIVE),
                DOUBLE_LITERAL_PATTERN = Pattern.compile("^[-+]?(?:\\d+\\.?|\\d*\\.\\d+)(?:e[-+]?\\d+)?d$", Pattern.CASE_INSENSITIVE),
                DOUBLE_LITERAL_NO_SUFFIX_PATTERN = Pattern.compile("^[-+]?(?:\\d+\\.|\\d*\\.\\d+)(?:e[-+]?\\d+)?$", Pattern.CASE_INSENSITIVE),
                BYTE_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+b$", Pattern.CASE_INSENSITIVE),
                SHORT_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+s$", Pattern.CASE_INSENSITIVE),
                INT_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+$", Pattern.CASE_INSENSITIVE),
                LONG_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+l$", Pattern.CASE_INSENSITIVE),
                NUMBER_PATTERN = Pattern.compile("^[-+]?\\d+$");

        private final StringPointer ptr;

        private SNBTParser(String string) {
            this.ptr = new StringPointer(string);
        }

        public static Tag parse(String string, int maxDepth) throws ParseException {
            SNBTParser parser = new SNBTParser(string);
            Tag tag = parser.parseAnything(maxDepth);
            parser.ptr.skipWhitespace();
            if (parser.ptr.hasNext()) {
                throw parser.ptr.parseException("invalid characters after end of snbt");
            }
            return tag;
        }

        public static Tag parse(String string) throws ParseException {
            return parse(string, 512);
        }

        private Tag parseAnything(int maxDepth) throws ParseException {
            ptr.skipWhitespace();
            switch (ptr.currentChar()) {
                case '{':
                    return parseCompoundTag(maxDepth);
                case '[':
                    if (ptr.hasCharsLeft(2) && ptr.lookAhead(1) != '"' && ptr.lookAhead(2) == ';') {
                        ptr.expectChar('[');
                        char arrayType = ptr.next();
                        ptr.expectChar(';');
                        ptr.skipWhitespace();
                        switch (arrayType) {
                            case 'B':
                                return parseByteArrayTag();
                            case 'I':
                                return parseIntArrayTag();
                            case 'L':
                                return parseLongArrayTag();
                        }
                        throw new ParseException("invalid array type '" + arrayType + "'", ptr.index);
                    }

                    return parseListTag(maxDepth);
            }
            return parseStringOrLiteral();
        }

        private Tag parseStringOrLiteral() throws ParseException {
            ptr.skipWhitespace();
            if (ptr.currentChar() == '"') {
                return new StringTag(ptr.parseQuotedString());
            }
            String s = ptr.parseSimpleString();
            if (s.isEmpty()) {
                throw new ParseException("expected non empty value", ptr.index);
            }
            if (FLOAT_LITERAL_PATTERN.matcher(s).matches()) {
                return new FloatTag(Float.parseFloat(s.substring(0, s.length() - 1)));
            } else if (BYTE_LITERAL_PATTERN.matcher(s).matches()) {
                try {
                    return new ByteTag(Byte.parseByte(s.substring(0, s.length() - 1)));
                } catch (NumberFormatException ex) {
                    throw ptr.parseException("byte not in range: \"" + s.substring(0, s.length() - 1) + "\"");
                }
            } else if (SHORT_LITERAL_PATTERN.matcher(s).matches()) {
                try {
                    return new ShortTag(Short.parseShort(s.substring(0, s.length() - 1)));
                } catch (NumberFormatException ex) {
                    throw ptr.parseException("short not in range: \"" + s.substring(0, s.length() - 1) + "\"");
                }
            } else if (LONG_LITERAL_PATTERN.matcher(s).matches()) {
                try {
                    return new LongTag(Long.parseLong(s.substring(0, s.length() - 1)));
                } catch (NumberFormatException ex) {
                    throw ptr.parseException("long not in range: \"" + s.substring(0, s.length() - 1) + "\"");
                }
            } else if (INT_LITERAL_PATTERN.matcher(s).matches()) {
                try {
                    return new IntTag(Integer.parseInt(s));
                } catch (NumberFormatException ex) {
                    throw ptr.parseException("int not in range: \"" + s.substring(0, s.length() - 1) + "\"");
                }
            } else if (DOUBLE_LITERAL_PATTERN.matcher(s).matches()) {
                return new DoubleTag(Double.parseDouble(s.substring(0, s.length() - 1)));
            } else if (DOUBLE_LITERAL_NO_SUFFIX_PATTERN.matcher(s).matches()) {
                return new DoubleTag(Double.parseDouble(s));
            } else if ("true".equalsIgnoreCase(s)) {
                return new ByteTag((byte) 1);
            } else if ("false".equalsIgnoreCase(s)) {
                return new ByteTag((byte) 0);
            }
            return new StringTag(s);
        }

        private CompoundTag parseCompoundTag(int maxDepth) throws ParseException {
            ptr.expectChar('{');

            CompoundTag compoundTag = new CompoundTag();

            ptr.skipWhitespace();
            while (ptr.hasNext() && ptr.currentChar() != '}') {
                ptr.skipWhitespace();
                String key = ptr.currentChar() == '"' ? ptr.parseQuotedString() : ptr.parseSimpleString();
                if (key.isEmpty()) {
                    throw new ParseException("empty keys are not allowed", ptr.index);
                }
                ptr.expectChar(':');

                compoundTag.setTag(key, parseAnything(decrementMaxDepth(maxDepth)));

                if (!ptr.nextArrayElement()) {
                    break;
                }
            }
            ptr.expectChar('}');
            return compoundTag;
        }

        private ListTag parseListTag(int maxDepth) throws ParseException {
            ptr.expectChar('[');
            ptr.skipWhitespace();
            ListTag list = new ListTag();
            while (ptr.currentChar() != ']') {
                Tag element = parseAnything(decrementMaxDepth(maxDepth));
                try {
                    list.addTag(element);
                } catch (IllegalArgumentException ex) {
                    throw ptr.parseException(ex.getMessage());
                }
                if (!ptr.nextArrayElement()) {
                    break;
                }
            }
            ptr.expectChar(']');
            return list;
        }

        private ByteArrayTag parseByteArrayTag() throws ParseException {
            List<Byte> byteList = new ArrayList<>();
            while (ptr.currentChar() != ']') {
                String s = ptr.parseSimpleString();
                ptr.skipWhitespace();
                if (NUMBER_PATTERN.matcher(s).matches()) {
                    try {
                        byteList.add(Byte.parseByte(s));
                    } catch (NumberFormatException ex) {
                        throw ptr.parseException("byte not in range: \"" + s + "\"");
                    }
                } else {
                    throw ptr.parseException("invalid byte in ByteArrayTag: \"" + s + "\"");
                }
                if (!ptr.nextArrayElement()) {
                    break;
                }
            }
            ptr.expectChar(']');
            byte[] bytes = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                bytes[i] = byteList.get(i);
            }
            return new ByteArrayTag(bytes);
        }

        private IntArrayTag parseIntArrayTag() throws ParseException {
            List<Integer> intList = new ArrayList<>();
            while (ptr.currentChar() != ']') {
                String s = ptr.parseSimpleString();
                ptr.skipWhitespace();
                if (NUMBER_PATTERN.matcher(s).matches()) {
                    try {
                        intList.add(Integer.parseInt(s));
                    } catch (NumberFormatException ex) {
                        throw ptr.parseException("int not in range: \"" + s + "\"");
                    }
                } else {
                    throw ptr.parseException("invalid int in IntArrayTag: \"" + s + "\"");
                }
                if (!ptr.nextArrayElement()) {
                    break;
                }
            }
            ptr.expectChar(']');
            return new IntArrayTag(intList.stream().mapToInt(i -> i).toArray());
        }

        private LongArrayTag parseLongArrayTag() throws ParseException {
            List<Long> longList = new ArrayList<>();
            while (ptr.currentChar() != ']') {
                String s = ptr.parseSimpleString();
                ptr.skipWhitespace();
                if (NUMBER_PATTERN.matcher(s).matches()) {
                    try {
                        longList.add(Long.parseLong(s));
                    } catch (NumberFormatException ex) {
                        throw ptr.parseException("long not in range: \"" + s + "\"");
                    }
                } else {
                    throw ptr.parseException("invalid long in LongArrayTag: \"" + s + "\"");
                }
                if (!ptr.nextArrayElement()) {
                    break;
                }
            }
            ptr.expectChar(']');
            return new LongArrayTag(longList.stream().mapToLong(l -> l).toArray());
        }

        private int decrementMaxDepth(int maxDepth) {
            if (maxDepth < 0) {
                throw new IllegalArgumentException("negative maximum depth is not allowed");
            } else if (maxDepth == 0) {
                throw new IllegalStateException("reached maximum depth of NBT structure");
            }
            return --maxDepth;
        }
    }
}
