package io.github.oppsgo.json.benchmark;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.fastjson.FastjsonAdapter;
import io.github.oppsgo.json.support.BindingCache;

/**
 * Fastjson 1.x end-to-end round-trip with/without binding cache.
 * Expect a large relative win for {@code CACHED}, similar to Fastjson2 (filters hit cache each call).
 */
public class FastjsonRoundTripBenchmark extends AbstractAdapterRoundTripBenchmark {

    @Override
    protected JsonAdapter createAdapter(BindingCache bindings) {
        return new FastjsonAdapter(JsonOptions.defaults(), bindings);
    }
}
