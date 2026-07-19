package io.github.oppsgo.json.benchmark;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.fastjson2.Fastjson2Adapter;
import io.github.oppsgo.json.support.BindingCache;

/** Fastjson2 end-to-end round-trip with/without binding cache. */
public class Fastjson2RoundTripBenchmark extends AbstractAdapterRoundTripBenchmark {

    @Override
    protected JsonAdapter createAdapter(BindingCache bindings) {
        return new Fastjson2Adapter(JsonOptions.defaults(), bindings);
    }
}
