package com.matuw.json.fastjson;

import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.adapter.JsonAdapterFactory;
import com.matuw.json.config.JsonOptions;

/**
 * @author Shihwan
 */
public class FastJsonAdapterFactory implements JsonAdapterFactory {

    public static FastJsonAdapterFactory getInstance() {
        return new FastJsonAdapterFactory();
    }

    @Override
    public JsonAdapter create(JsonOptions options) {
        return new FastJsonAdapter(options);
    }
}
