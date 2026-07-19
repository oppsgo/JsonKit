package io.github.oppsgo.json.fastjson;

import io.github.oppsgo.json.JsonContractTest;
import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;

public class FastjsonAdapterTest extends JsonContractTest {
    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return new FastjsonAdapter(options);
    }
}
