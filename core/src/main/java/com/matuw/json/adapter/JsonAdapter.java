package com.matuw.json.adapter;

import com.matuw.json.reflect.JsonTypeReference;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * @author Shihwan
 */
public interface JsonAdapter {

    String toJson(Object object);

    <T> T fromJson(String json, @NotNull Class<T> clazz);

    <T> T fromJson(String json, @NotNull Type type);

    <T> T fromJson(String json, @NotNull JsonTypeReference<T> reference);
}
