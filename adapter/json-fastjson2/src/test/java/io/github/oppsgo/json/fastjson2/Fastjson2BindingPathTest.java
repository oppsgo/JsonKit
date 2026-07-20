package io.github.oppsgo.json.fastjson2;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.github.oppsgo.json.annotation.JsonAlias;
import io.github.oppsgo.json.annotation.JsonProperty;
import io.github.oppsgo.json.convert.StrategyInstanceCache;
import io.github.oppsgo.json.reflect.JsonTypeReference;
import io.github.oppsgo.json.support.BindingCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers 方案 D (native parse) vs 方案 C (ObjectReader) path selection.
 */
public class Fastjson2BindingPathTest {

    public static class PlainDto {
        public String name;
        public int age;
    }

    public static class AnnotatedDto {
        @JsonProperty("user_name")
        @JsonAlias({"userName"})
        public String userName;
        public int age;
    }

    public static class WrapperPlain {
        public PlainDto nested;
    }

    public static class WrapperAnnotated {
        public AnnotatedDto nested;
    }

    @Test
    public void plainDtoDoesNotNeedBinding() {
        Fastjson2BindingSupport support = newSupport();
        assertFalse(support.needsJsonKitDeserializeBinding(PlainDto.class));
        assertFalse(support.needsJsonKitDeserializeBinding(WrapperPlain.class));
    }

    @Test
    public void annotatedAndNestedNeedBinding() {
        Fastjson2BindingSupport support = newSupport();
        assertTrue(support.needsJsonKitDeserializeBinding(AnnotatedDto.class));
        assertTrue(support.needsJsonKitDeserializeBinding(WrapperAnnotated.class));
        Type listType = new JsonTypeReference<List<AnnotatedDto>>() {
        }.getType();
        assertTrue(support.needsJsonKitDeserializeBinding(listType));
        Type mapType = new JsonTypeReference<Map<String, AnnotatedDto>>() {
        }.getType();
        assertTrue(support.needsJsonKitDeserializeBinding(mapType));
    }

    @Test
    public void plainDtoNativeParseMatchesFields() {
        Fastjson2Adapter adapter = new Fastjson2Adapter();
        PlainDto dto = adapter.fromJson("{\"name\":\"Ada\",\"age\":3}", PlainDto.class);
        assertNotNull(dto);
        assertEquals("Ada", dto.name);
        assertEquals(3, dto.age);
    }

    @Test
    public void annotatedObjectReaderBindsRenameAndAlias() {
        Fastjson2Adapter adapter = new Fastjson2Adapter();
        AnnotatedDto byProperty = adapter.fromJson(
                "{\"user_name\":\"Ada\",\"age\":3}", AnnotatedDto.class);
        assertNotNull(byProperty);
        assertEquals("Ada", byProperty.userName);

        AnnotatedDto byAlias = adapter.fromJson(
                "{\"userName\":\"Bob\",\"age\":4}", AnnotatedDto.class);
        assertNotNull(byAlias);
        assertEquals("Bob", byAlias.userName);
    }

    @Test
    public void nestedAnnotatedListUsesBindingReaders() {
        Fastjson2Adapter adapter = new Fastjson2Adapter();
        Type listType = new JsonTypeReference<List<AnnotatedDto>>() {
        }.getType();
        List<AnnotatedDto> list = adapter.fromJson(
                "[{\"user_name\":\"A\",\"age\":1},{\"userName\":\"B\",\"age\":2}]", listType);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("A", list.get(0).userName);
        assertEquals("B", list.get(1).userName);
    }

    private static Fastjson2BindingSupport newSupport() {
        return new Fastjson2BindingSupport(new BindingCache(), new StrategyInstanceCache());
    }
}
