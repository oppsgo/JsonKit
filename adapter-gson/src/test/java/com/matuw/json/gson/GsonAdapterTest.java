package com.matuw.json.gson;


import com.matuw.json.JsonKitTest;
import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.config.JsonOptions;

/**
 * @author Shihwan
 */
public class GsonAdapterTest extends JsonKitTest {

    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return GsonAdapterFactory.getInstance().create(options);
    }
}