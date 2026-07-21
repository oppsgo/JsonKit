package io.github.oppsgo.json.support;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;

/**
 * Default Instantiator: no-arg constructor; fields applied by the adapter.
 */
public final class DefaultObjectInstantiator implements ObjectInstantiator {

    public static final DefaultObjectInstantiator INSTANCE = new DefaultObjectInstantiator();

    private DefaultObjectInstantiator() {
    }

    @Override
    public boolean supports(Class<?> type) {
        if (type == null) {
            return false;
        }
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public boolean constructsFromProperties(Class<?> type) {
        return false;
    }

    @Override
    public Object instantiate(Class<?> type, Map<String, Object> properties)
            throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * Empty property map for no-arg construction call sites.
     */
    public static Map<String, Object> noProperties() {
        return Collections.emptyMap();
    }
}
