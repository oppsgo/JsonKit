package io.github.oppos.json.gson;

import io.github.oppos.json.JsonContractTest;
import io.github.oppos.json.JsonOptions;
import io.github.oppos.json.adapter.JsonAdapter;

public class GsonAdapterTest extends JsonContractTest {
    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return new GsonAdapter(options);
    }
}
