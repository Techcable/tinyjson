/**
 * A tiny single-file JSON library for java.
 *
 * Should be fairly fast. 
 *
 * See also: mjson, simple-json, gson, jackson
 * Repository: https://github.com/Techcable/tinyjson
 * 
 * Copyright 2022 Techcable
 * Dual-licensed under MIT/CC0
 */
package net.techcable.tinyjson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * A tiny json parser.
 */
public final class TinyJson {
    private TinyJson() {}

    /**
     * Parse the specified string as json
     *
     * @param text the text to parse
       @throws JsonException if an error occurs parsing json
     * @return the json value
     */
    public static TinyJson.JsonValue parseString(String text) {
        try {
            Parser parser = new Parser(new StringReader(text));
            JsonValue value = parser.parseValue();
            parser.expectFinished();
            return value;
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }
    public static class Serializer {
        private final StringBuilder builder = new StringBuilder();
        private Serializer() {}

        public static Serializer simple() {
            return new Serializer();
        }

        public Serializer serialize(boolean b) {
            builder.append(b);
            return this;
        }
        public Serializer serialize(int i) {
            builder.append(i);
            return this;
        }
        public Serializer serialize(double d) {
            builder.append(d);
            return this;
        }
        public Serializer serialize(String s) {
            builder.append('"');
            for (int i = 0; i < s.length(); i++) {
                escapeCharInto(s.charAt(i));
            }
            builder.append('"');
            return this;
        }
        private void escapeCharInto(char c) {
            if (c >= 32 && c < 127) {
                // Printable ASCII
                builder.append(c);
            }
            builder.append('\\');
            switch (c) {
                case '\\':
                case '"':
                    builder.append(c);
                    break;
                case '\t':
                    builder.append('t');
                    break;
                case '\n':
                    builder.append('n');
                case '\r':
                    builder.append('r');
                    break;
                default:
                    builder.append(Integer.toHexString(c));
                    break;
            }
        }

        public Serializer serialize(JsonSerializable ser) {
            if (ser == null) {
                this.builder.append((Object) null);
            } else {
                ser.serializeJson(this);
            }
            return this;
        }

        public <T extends JsonSerializable> Serializer serializeObject(Map<String, T> entries) {
            boolean first = true;
            builder.append('{');
            for (Map.Entry<String, T> entry : entries.entrySet()) {
                if (!first) builder.append(',');
                serialize(entry.getKey());
                builder.append(':');
                serialize(entry.getValue());
                first = false;
            }
            builder.append('}');
            return this;
        }
        public <T extends JsonSerializable> Serializer serializeArray(List<T> entries) {
            boolean first = true;
            builder.append('[');
            for (T entry : entries) {
                if (!first) builder.append(',');
                this.serialize(entry);
                first = false;
            }
            builder.append(']');
            return this;
        }
        /* package */ void serializePrimitive0(JsonPrimitive prim) {
            Object value = prim.getValueAsObject();
            if (value instanceof String) {
                this.serialize((String) value);
            } else {
                this.builder.append(Objects.toString(value));
            }
        }

        @Override
        public String toString() {
            return this.builder.toString();
        }
    }

    public static interface JsonSerializable {
        public void serializeJson(TinyJson.Serializer serializer);

        public static String toJsonString(JsonSerializable value) {
            if (value == null) {
                return "null";
            } else {
                TinyJson.Serializer ser = TinyJson.Serializer.simple();
                value.serializeJson(ser);
                return value.toString();
            }
        }
    }

    public abstract static class JsonValue implements JsonSerializable {
        /**
         * Serialize this value as a json string.
         *
         * @return the json string representation
         */
        @Override
        public String toString() {
            TinyJson.Serializer serializer = TinyJson.Serializer.simple();
            this.serializeJson(serializer);
            return serializer.toString();
        }
        @Override
        public abstract int hashCode();
        @Override
        public abstract boolean equals(Object other);
    }
    public static final class JsonPrimitive extends JsonValue {
        private final Object value;
        private JsonPrimitive(Object value) {
            this.value = value;
        }

        /**
         * Get the value of this primitive as an untyped object
         */
        public Object getValueAsObject() {
            return this.value;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.value);
        }

        @Override
        public String toString() {
            if (this.value instanceof String) {
                return super.toString(); // Use serializer
            } else {
                return Objects.toString(value);
            }
        }

        @Override
        public void serializeJson(TinyJson.Serializer ser) {
            // NOTE: This is an implementation detail
            ser.serializePrimitive0(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof JsonPrimitive) {
                Object otherValue = ((JsonPrimitive) obj).value;
                return this.value == otherValue ||
                    Objects.equals(this.value, otherValue);
            } else {
                return false;
            }
        }

        public static final JsonPrimitive NULL = new JsonPrimitive(null);
        private static final JsonPrimitive TRUE = new JsonPrimitive(true);
        private static final JsonPrimitive FALSE = new JsonPrimitive(false);
        public static JsonPrimitive of(String s) {
            return new JsonPrimitive(Objects.requireNonNull(s));
        }
        public static JsonPrimitive of(int i) {
            return new JsonPrimitive(i);
        }
        public static JsonPrimitive of(double d) {
            return new JsonPrimitive(d);
        }
        public static JsonPrimitive of(boolean b) {
            return b ? JsonPrimitive.TRUE : JsonPrimitive.FALSE;
        }
    }

    public static final class JsonObject extends JsonValue {
        private final Map<String, JsonValue> entries;
        private JsonObject(Map<String, JsonValue> map) {
            this.entries = Objects.requireNonNull(map);
        }

        private static final JsonObject EMPTY = viewOf(Collections.emptyMap());

        /**
         * Return an immutable empty map
         *
         * @see Collections#emptyMap()
         * @return an empty map that is immutable
         */
        public static JsonObject empty() {
            return EMPTY;
        }

        @Override
        public void serializeJson(TinyJson.Serializer ser) {
            ser.serializeObject(this.entries);
        }

        @Override
        public int hashCode() {
            return this.entries.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof JsonObject) {
                return this.entries.equals(((JsonObject) obj).entries);
            } else {
                return false;
            }
        }

        public static JsonObject viewOf(Map<String, JsonValue> value) {
            return new JsonObject(value);
        }
        public static JsonObject copyOf(Map<String, JsonValue> value) {
            JsonObject obj = mutableEmpty();
            obj.entries.putAll(value);
            return obj;
        }

        /**
         * Return a mutable empty map.
         *
         * The resulting map preserves insertion order.
         *
         * @see HashMap()
         * @see LinkedHashMap()
         * @return a mutable and
         */
        public static JsonObject mutableEmpty() {
            return new JsonObject(new LinkedHashMap<>());
        }
    }


    public static final class JsonArray extends JsonValue {
        private final List<JsonValue> elements;
        private JsonArray(List<JsonValue> elements) {
            this.elements = Objects.requireNonNull(elements);
        }

        private static final JsonArray EMPTY = viewOf(Collections.emptyList());

        @Override
        public void serializeJson(TinyJson.Serializer ser) {
            ser.serializeArray(this.elements);
        }

        /**
         * Return an immutable empty array
         *
         * @see Collections#emptyMap()
         * @return an empty map that is immutable
         */
        public static JsonArray empty() {
            return EMPTY;
        }

        @Override
        public int hashCode() {
            return this.elements.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof JsonArray) {
                return this.elements.equals(((JsonArray) obj).elements);
            } else {
                return false;
            }
        }

        public static JsonArray viewOf(List<JsonValue> value) {
            return new JsonArray(value);
        }
        public static JsonArray copyOf(List<JsonValue> value) {
            JsonArray arr = mutableEmpty();
            arr.elements.addAll(value);
            return arr;
        }

        /**
         * Return a mutable empty array.
         *
         * @see ArrayList()
         * @return a mutable and empty array
         */
        public static JsonArray mutableEmpty() {
            return new JsonArray(new ArrayList<>());
        }
    }

    /**
     * Raw interface to the underlying parser.
     *
     * Parses using recursive decent with one token of lookahead (LL(1)).
     * In addition to {@link JsonSyntaxException},
     * it may also throw {@link StackOverflowError} and {@link OutOfMemoryError}.
     */
    public static final class Parser {
        private final Reader reader;
        /**
         * Reuse the underlying buffer, to avoid excessive allocation.
         */
        private final StringBuilder buffer = new StringBuilder();
        private long offset = 0;
        /**
         * One character of pushback.
         *
         * We do not use {@link java.io.PushbackReader} because we only ever need
         * one character of push back (and want to avoid double buffering).
         */
        private int pushBack = -1;

        private void clearBuffer() {
            this.buffer.setLength(0);
        }

        /**
         * Construct a new parser wrapping the specified reader.
         *
         * @param reader the reader to wrap
         */
        public Parser(Reader reader) {
            this.reader = Objects.requireNonNull(reader);
        }

        public void expectEnd() throws IOException {
            int c = readChar();
            if (c >= 0) throw unexpectedChar((char) c, "Expected EOF");
        }
        public JsonValue parseValue() throws IOException {
            skipWhitespace();
            int res = expectChar();
            switch (res) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                default:
                    return parsePrimitive(true);
            }
        }
        public JsonObject parseObject() throws IOException {
            skipWhitespace();
            expect('{');
            skipWhitespace();
            Map<String, JsonValue> res = new LinkedHashMap<>();
            while (true) {
                String key = jsonString();
                skipWhitespace();
                expect(':');
                JsonValue value = parseValue();
                skipWhitespace();
                char c = expectChar();
                skipWhitespace();
                res.put(key, value);
                if (c != ',') {
                    expect('}');
                    break;
                }
            }
            return JsonObject.viewOf(res);
        }
        public JsonArray parseArray() throws IOException {
            skipWhitespace();
            expect('[');
            List<JsonValue> elements = new ArrayList<>();
            elementsLoop: while (true) {
                JsonValue value = parseValue();
                elements.add(value);
                skipWhitespace();
                char next = expectChar();
                switch (next) {
                    case ',':
                        break; // break switch
                    case ']':
                        break elementsLoop;
                    default:
                        throw unexpectedChar(next, "Expected either `,` or `]`");
                }
            }
            return JsonArray.viewOf(elements);
        }
        public JsonPrimitive parsePrimitive() throws IOException {
            return parsePrimitive(false);
        }
        private JsonPrimitive parsePrimitive(boolean expectedAnyValue) throws IOException {
            skipWhitespace();
            char c = expectChar();
            switch (c) {
                case '"':
                    pushBack(c);
                    return JsonPrimitive.of(jsonString());
                case 't':
                    expectNamedConstant(c, "rue");
                    return JsonPrimitive.of(true);
                case 'f':
                    expectNamedConstant(c, "alse");
                    return JsonPrimitive.of(false);
                case 'n':
                    expectNamedConstant(c, "ull");
                    return JsonPrimitive.NULL;
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '9':
                    pushBack(c);
                    return parseNumber();
                default:
                    throw unexpectedChar(c, expectedAnyValue ? "Expected a json value" : "Expected a primitive");
            }
        }
        private JsonPrimitive parseNumber() throws IOException {
            clearBuffer();
            boolean negative = false;
            /*
             * Need to make a decision here, in order to decide between int/double
             *
             * Also, I believe we only accept a subset of numbers that Integer.parseInt does.
             */
            int next = expectChar();
            final int primaryVal;
            if (next == '-') {
                negative = true;
                next = expectChar();
            }
            if (next == '0') {
                next = expectChar();
                if (isAsciiDigit(next)) {
                    throw genericError("JSON does not permit leading zeroes for numbers");
                } else {
                    pushBack(next);
                }
                primaryVal = 0;
            } else {
                pushBack(next);
                primaryVal = parseRawUnsignedInteger();
            }
            OptionalInt fraction = OptionalInt.empty();
            OptionalInt exponent = OptionalInt.empty();
            next = readChar();
            partLoop: while (true) {
                switch (next) {
                    case '.':
                        if (fraction.isPresent()) throw genericError("Duplicate fraction for number");
                        /*
                         * TODO: Technically speaking we should only validate this, not parse it
                         *
                         * Parsing here could theoretically throw an OverflowError even if Double.parseString
                         * is able to handle it
                         */
                        fraction = OptionalInt.of(parseRawUnsignedInteger());
                        next = readChar();
                        break;
                    case 'e':
                    case 'E':
                        if (exponent.isPresent()) throw genericError("Duplicate exponent for number");
                        /*
                         * Don't really need to worry about OverflowError here because exponents > 4 billion
                         * are obviously invalid.
                         */
                        exponent = OptionalInt.of(parseRawUnsignedInteger());
                        next = readChar();
                        break;
                    default:
                        pushBack(next);
                    case -1:
                        break partLoop;
                }
            }
            if (fraction.isEmpty() && exponent.isEmpty()) {
                return JsonPrimitive.of(negative ? -primaryVal : primaryVal);
            } else {
                clearBuffer();
                if (negative) this.buffer.append('-');
                this.buffer.append(primaryVal);
                if (fraction.isPresent()) {
                    this.buffer.append('.');
                    this.buffer.append(fraction.getAsInt());
                }
                if (exponent.isPresent()) {
                    this.buffer.append('e');
                    this.buffer.append(exponent.getAsInt());
                }
                // Floating point parsing is *REALLY* hard, so I get a pass here
                return JsonPrimitive.of(Double.parseDouble(buffer.toString()));
            }
        }
        private static boolean isAsciiDigit(int c) {
            return c >= '0' && c <= '9';
        }
        private int parseRawUnsignedInteger() throws IOException {
            int c = expectChar();
            int res = 0;
            if (!isAsciiDigit(c)) throw unexpectedChar((char) c, "Invalid digit while parsing integer");
            try {
                do {
                    /*
                     * This should be noticeably faster than Integer.parseInt because:
                     * 1. It only supports ASCII characters, avoiding Character.digitValue
                     * 2. addExact/multiplyExact are VM intrinsics
                     * 3. It avoids buffering and works in a single pass (unlike double parsing)
                     */
                    res = Math.multiplyExact(res, 10);
                    res = Math.addExact(res, (c - '0'));
                    c = readChar();
                } while (c >= 0 && isAsciiDigit(c));
                return res;
            } catch (ArithmeticException e) {
                throw genericError("Integer is too large");
            }
        }
        private void expectNamedConstant(char first, String remaining) throws IOException {
            String actualRemaining = readChars(remaining.length());
            if (!actualRemaining.equals(remaining)) {
                throw genericError("Expected `" + first + remaining + "`, but got `" + first + actualRemaining + "`");
            }
        }
        private static boolean isWhitespace(char c) {
            return c == ' ' | c == '\n' | c == '\r' | c == '\t';
        }
        public void skipWhitespace() throws IOException {
            if (pushBack >= 0) {
                if (isWhitespace((char) pushBack)) {
                    readChar(); // consume pushback
                } else {
                    return; // not whitespace ;)
                }
            }
            char c;
            do {
                c = expectChar();
            } while (isWhitespace(c));
            pushBack(c);
        }
        public String jsonString() throws IOException {
            skipWhitespace();
            this.buffer.setLength(0); // clear buffer
            this.expect('"');
            int c;
            while ((c = expectChar()) != '"') {
                if (c == '\\') {
                    this.buffer.append(readEscapedChar());
                } else {
                    this.buffer.append((char) c);
                }
            }
            return this.buffer.toString();
        }
        private char readEscapedChar() throws IOException {
            char c = expectChar();
            switch (c) {
                // Escapes where value == escape
                case '"':
                case '\\':
                case '/':
                    return c;
                // Magic escapes where value != escape
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 't':
                    return '\t';
                // Unicode escape (the most magic of all)
                case 'u': {
                    String hex = readChars(4);
                    int val;
                    try {
                        val = Integer.parseUnsignedInt(hex, 16);
                    } catch (NumberFormatException e) {
                        throw genericError("Invalid hex for unicode escape: " + hex);
                    }
                    if (val < 0 || val > Character.MAX_CODE_POINT) {
                        throw genericError("Invalid code point: " + hex);
                    }
                    return (char) val;
                }
                default:
                    throw unexpectedChar(c, "Unexpected lookup char");
            }
        }
        private void expect(char expected) throws IOException {
            char c = expectChar();
            if (c != expected) throw unexpectedChar(c, "Expected a `" + expected + "`, but got");
        }
        private char expectChar() throws IOException {
            int i = this.readChar();
            if (i < 0) throw this.unexpectedEof();
            return (char) i;
        }
        private void pushBack(int c) {
            if (c < 0) throw new IllegalArgumentException();
            // This is a logic error on my part. It means we already have push back.
            if (this.pushBack >= 0) throw new IllegalStateException();
            this.pushBack = c;
            this.offset -= 1;
        }
        public String readChars(int amount) throws IOException {
            char[] buf = new char[amount];
            for (int i = 0; i < amount; i++) {
                buf[i] = expectChar();
            }
            return new String(buf);

        }
        private int readChar() throws IOException {
            int i;
            if ((i = pushBack) >= 0) {
                this.pushBack = -1; // "consume" the push back
            } else {
                i = this.reader.read();
            }
            if (i >= 0) {
                this.offset += 1;
            }
            return i;
        }
        private JsonSyntaxException unexpectedEof() {
            return new JsonSyntaxException("Unexpected EOF", this.offset);
        }
        public JsonSyntaxException unexpectedChar(char c, String reason) {
            return new JsonSyntaxException(reason + ": `" + c + "`", this.offset - 1);
        }
        public JsonSyntaxException genericError(String msg) {
            return new JsonSyntaxException(msg, this.offset);
        }
        public void expectFinished() throws IOException {
            int c = this.readChar();
            if (c >= 0) {
                throw new JsonSyntaxException("Expected EOF, but got " + c, this.offset);
            }
        }
    }

    /**
     * Indicates that an error occurred parsing json.
     */
    public abstract static class JsonException extends RuntimeException {
        public JsonException(String msg) {
            super(msg);
        }
        public JsonException(String msg, Exception cause) {
            super(msg, cause);
        }
    }
    public static class JsonSyntaxException extends JsonException {
        public JsonSyntaxException(String msg, long loc) {
            super(msg + " at offset " + loc);
        }
    }

    /**
     * An unechecked wrapper around a {@link IOException}.
     *
     * This is the JSON equivalent of {@link java.io.UncheckedIOException}.
     */
    public static class JsonIOException extends JsonException {
        private final IOException cause;

        public JsonIOException(IOException cause) {
            super("IO error while parsing json", cause);
            this.cause = Objects.requireNonNull(cause);
        }
    }
}
