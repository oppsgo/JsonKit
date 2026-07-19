package io.github.oppsgo.json.gson;

import io.github.oppsgo.json.JsonContractTest;
import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;

public class GsonAdapterTest extends JsonContractTest {
    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return new GsonAdapter(options);
    }
}
