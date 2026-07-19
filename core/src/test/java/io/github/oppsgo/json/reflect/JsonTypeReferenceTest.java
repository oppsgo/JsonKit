package io.github.oppsgo.json.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonTypeReferenceTest {

    @Test
    public void capturesListType_likeFastjsonAnonymousUsage() {
        JsonTypeReference<List<String>> ref = new JsonTypeReference<List<String>>() {
        };
        assertEquals("java.util.List<java.lang.String>", ref.getType().toString());
        assertSame(List.class, ref.getRawType());
    }

    @Test
    public void capturesNestedMapType() {
        JsonTypeReference<Map<String, List<Integer>>> ref =
                new JsonTypeReference<Map<String, List<Integer>>>() {
                };
        String typeName = ref.getType().toString();
        assertTrue(typeName.contains("java.util.Map"));
        assertTrue(typeName.contains("java.lang.Integer"));
        assertSame(Map.class, ref.getRawType());
    }

    @Test
    public void replacesTypeVariables_likeFastjsonTypeVarArgsConstructor() {
        JsonTypeReference<Box<String>> filled = newTypeVariableBox(String.class);
        Type type = filled.getType();
        assertInstanceOf(ParameterizedType.class, type);
        ParameterizedType parameterized = (ParameterizedType) type;
        assertEquals(Box.class, parameterized.getRawType());
        assertEquals(String.class, parameterized.getActualTypeArguments()[0]);
        assertSame(Box.class, filled.getRawType());
    }

    @Test
    public void rejectsIndirectSubclass_withClearMessage() {
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        new IndirectListRef();
                    }
                });
        String message = thrown.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("direct subclass") || message.contains("type argument"));
    }

    @Test
    public void allowsDirectNamedSubclass_likeFastjson() {
        JsonTypeReference<List<String>> ref = new DirectListRef();
        assertEquals("java.util.List<java.lang.String>", ref.getType().toString());
    }

    private static <E> JsonTypeReference<Box<E>> newTypeVariableBox(Class<E> elementType) {
        return new JsonTypeReference<Box<E>>(elementType) {
        };
    }

    public static final class Box<E> {
        public E value;
    }

    private static class DirectListRef extends JsonTypeReference<List<String>> {
    }

    private static class IndirectListRef extends DirectListRef {
    }
}
