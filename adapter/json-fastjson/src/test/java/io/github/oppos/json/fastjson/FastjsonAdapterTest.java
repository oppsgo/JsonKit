package io.github.oppos.json.fastjson;

import io.github.oppos.json.JsonContractTest;
import io.github.oppos.json.JsonOptions;
import io.github.oppos.json.adapter.JsonAdapter;

public class FastjsonAdapterTest extends JsonContractTest {
    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return new FastjsonAdapter(options);
    }
}
