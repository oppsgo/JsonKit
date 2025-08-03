package com.matuw.json.adapter;

import com.matuw.json.config.JsonOptions;

/**
 * @author Shihwan
 */
public interface JsonAdapterFactory {

    JsonAdapter create(JsonOptions options);
}
