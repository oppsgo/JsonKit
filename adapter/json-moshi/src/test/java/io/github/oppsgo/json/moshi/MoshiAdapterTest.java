package io.github.oppsgo.json.moshi;

import io.github.oppsgo.json.JsonContractTest;
import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;

public class MoshiAdapterTest extends JsonContractTest {

    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return new MoshiAdapter(options);
    }
}
