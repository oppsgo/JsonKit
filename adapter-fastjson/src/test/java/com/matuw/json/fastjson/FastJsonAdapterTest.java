package com.matuw.json.fastjson;

import com.matuw.json.JsonKitTest;
import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.config.JsonOptions;


/**
 * @author Shihwan
 */
public class FastJsonAdapterTest extends JsonKitTest {

    @Override
    protected JsonAdapter createAdapter(JsonOptions options) {
        return FastJsonAdapterFactory.getInstance().create(options);
    }
}