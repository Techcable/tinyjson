package net.techcable.tinyjson;

import net.techcable.tinyjson.TinyJson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPrimitives {
    @Test
    public void testIntegers() {
        assertEquals(
                JsonPrimitive.of(77),
                TinyJson.parseString("77")
        );
        assertEquals(
                JsonPrimitive.of(0),
                TinyJson.parseString("0")
        );
        assertEquals(
                JsonPrimitive.of(-55),
                TinyJson.parseString("-55")
        );
    }
    @Test
    public void testDoubles() {
        assertEquals(
                JsonPrimitive.of(-0.0),
                TinyJson.parseString("-0.0")
        );
        assertEquals(
                JsonPrimitive.of(+0.0),
                TinyJson.parseString("0.0")
        );
    }
}
