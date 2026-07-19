package io.github.oppos.json.fastjson2;

import io.github.oppos.json.JsonContractTest;
import io.github.oppos.json.JsonOptions;
import io.github.oppos.json.adapter.JsonAdapter;

public class Fastjson2AdapterTest extends JsonContractTest {
    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return new Fastjson2Adapter(options);
    }
}
