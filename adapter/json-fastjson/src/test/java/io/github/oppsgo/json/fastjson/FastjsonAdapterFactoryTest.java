package io.github.oppsgo.json.fastjson;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class FastjsonAdapterFactoryTest {

    @Test
    public void ofReusesSameAdapter() {
        FastjsonAdapterFactory factory = FastjsonAdapterFactory.of();
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
    }

    @Test
    public void ofWithOptionsReusesSameAdapter() {
        JsonOptions options = new JsonOptions.Builder().setSerializeNulls(true).build();
        FastjsonAdapterFactory factory = FastjsonAdapterFactory.of(options);
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
        JsonAdapter other = FastjsonAdapterFactory.of(options).create();
        assertNotSame(first, other);
    }
}
