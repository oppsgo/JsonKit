package com.matuw.json.reflect;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * <p>
 * {@code JsonTypeReference<List<String>> list = new JsonTypeReference<List<String>>() {};}
 * </p>
 *
 * @author Shihwan
 */
public abstract class JsonTypeReference<T> {

    private final Type type;

    /**
     * 通过匿名内部类的方式获取泛型类型信息
     */
    protected JsonTypeReference() {
        // 获取当前类的父类（即JsonTypeReference）的泛型信息
        Type superClass = getClass().getGenericSuperclass();

        // 检查是否是参数化类型（即带有泛型参数）
        if (superClass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) superClass;
            // 获取泛型参数的实际类型
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (actualTypeArguments.length == 1) {
                this.type = actualTypeArguments[0];
            } else {
                throw new IllegalArgumentException("JsonTypeReference only supports a single generic parameter.");
            }
        } else {
            throw new IllegalArgumentException("Please create an instance using an anonymous inner class.");
        }
    }

    private JsonTypeReference(Type type) {
        if (type == null) throw new NullPointerException();
        this.type = type;
    }

    /**
     * Gson 解析时直接使用我们自己获取的 Type 可能导致问题
     */
    public static <T> JsonTypeReference<T> get(@NotNull Type type) {
        return new JsonTypeReference<T>(type) {
        };
    }

    public final Type getType() {
        return type;
    }
}
