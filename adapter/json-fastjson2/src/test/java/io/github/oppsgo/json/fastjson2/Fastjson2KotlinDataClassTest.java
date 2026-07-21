package io.github.oppsgo.json.fastjson2;

import org.jetbrains.annotations.NotNull;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.kotlin.KotlinDataClassContractTest;

/**
 * Kotlin {@code data class} contract for Fastjson2.
 */
public class Fastjson2KotlinDataClassTest extends KotlinDataClassContractTest {
    @Override
    protected @NotNull JsonAdapter createAdapter(JsonOptions options) {
        return new Fastjson2Adapter(options);
    }
}
