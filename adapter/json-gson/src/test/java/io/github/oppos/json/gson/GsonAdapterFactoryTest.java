package io.github.oppos.json.gson;

import io.github.oppos.json.JsonOptions;
import io.github.oppos.json.adapter.JsonAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class GsonAdapterFactoryTest {

    @Test
    public void ofReusesSameAdapter() {
        GsonAdapterFactory factory = GsonAdapterFactory.of();
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
    }

    @Test
    public void ofWithOptionsReusesSameAdapter() {
        JsonOptions options = new JsonOptions.Builder().setSerializeNulls(true).build();
        GsonAdapterFactory factory = GsonAdapterFactory.of(options);
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
        JsonAdapter other = GsonAdapterFactory.of(options).create();
        assertNotSame(first, other);
    }
}
