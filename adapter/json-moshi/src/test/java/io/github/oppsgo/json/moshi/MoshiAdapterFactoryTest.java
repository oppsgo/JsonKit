package io.github.oppsgo.json.moshi;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MoshiAdapterFactoryTest {

    @Test
    public void ofReusesSameAdapter() {
        MoshiAdapterFactory factory = MoshiAdapterFactory.of();
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
    }

    @Test
    public void ofWithOptionsReusesSameAdapter() {
        JsonOptions options = new JsonOptions.Builder().setSerializeNulls(true).build();
        MoshiAdapterFactory factory = MoshiAdapterFactory.of(options);
        JsonAdapter first = factory.create();
        JsonAdapter second = factory.create();
        assertSame(first, second);
        JsonAdapter other = MoshiAdapterFactory.of(options).create();
        assertNotSame(first, other);
    }
}
