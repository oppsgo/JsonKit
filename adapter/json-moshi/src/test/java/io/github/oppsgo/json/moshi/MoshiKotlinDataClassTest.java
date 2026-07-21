package io.github.oppsgo.json.moshi;

import org.jetbrains.annotations.NotNull;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.kotlin.KotlinDataClassContractTest;

/**
 * Kotlin {@code data class} contract for Moshi.
 */
public class MoshiKotlinDataClassTest extends KotlinDataClassContractTest {
    @Override
    protected @NotNull JsonAdapter createAdapter(JsonOptions options) {
        return new MoshiAdapter(options);
    }
}
