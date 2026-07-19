package io.github.oppsgo.json.fastjson2;

import io.github.oppsgo.json.JsonContractTest;
import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;

public class Fastjson2AdapterTest extends JsonContractTest {
    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return new Fastjson2Adapter(options);
    }
}
