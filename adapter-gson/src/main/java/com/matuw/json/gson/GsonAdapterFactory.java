package com.matuw.json.gson;

import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.adapter.JsonAdapterFactory;
import com.matuw.json.config.JsonOptions;

/**
 * @author Shihwan
 */
public class GsonAdapterFactory implements JsonAdapterFactory {

    public static GsonAdapterFactory getInstance() {
        return new GsonAdapterFactory();
    }

    @Override
    public JsonAdapter create(JsonOptions options) {
        return new GsonAdapter(options);
    }
}
