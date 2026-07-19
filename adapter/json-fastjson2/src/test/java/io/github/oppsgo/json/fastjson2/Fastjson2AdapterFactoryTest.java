package io.github.oppsgo.json.fastjson2;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class Fastjson2AdapterFactoryTest {

    @Test
    public void ofReusesSameAdapter() {
        Fastjson2AdapterFactory factory = Fastjson2AdapterFactory.of();
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
    }

    @Test
    public void ofWithOptionsReusesSameAdapter() {
        JsonOptions options = new JsonOptions.Builder().setSerializeNulls(true).build();
        Fastjson2AdapterFactory factory = Fastjson2AdapterFactory.of(options);
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
        JsonAdapter other = Fastjson2AdapterFactory.of(options).create();
        assertNotSame(first, other);
    }
}
