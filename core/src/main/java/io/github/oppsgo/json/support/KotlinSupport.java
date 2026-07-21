package io.github.oppsgo.json.support;

import io.github.oppsgo.json.JsonOptions;

/**
 * Process-wide Kotlin Instantiator registration (Android-friendly; no ServiceLoader).
 * <p>
 * Call {@link #enable(ObjectInstantiator)} from the optional {@code json-kotlin}
 * module ({@code JsonKitKotlin.enable()}). Adapters consult
 * {@link #isActive(JsonOptions)} before using the Instantiator for Kotlin types.
 */
public final class KotlinSupport {

    private static volatile boolean enabled;
    private static volatile ObjectInstantiator instantiator;

    private KotlinSupport() {
    }

    /**
     * Registers {@code kotlinInstantiator} and marks Kotlin support enabled.
     */
    public static void enable(ObjectInstantiator kotlinInstantiator) {
        if (kotlinInstantiator == null) {
            throw new IllegalArgumentException("kotlinInstantiator == null");
        }
        instantiator = kotlinInstantiator;
        enabled = true;
    }

    /**
     * Clears registration (tests / process teardown).
     */
    public static void disable() {
        enabled = false;
        instantiator = null;
    }

    /**
     * Whether {@link #enable} has been called.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Registered Instantiator, or {@code null}.
     */
    public static ObjectInstantiator getInstantiator() {
        return instantiator;
    }

    /**
     * Whether adapters should attempt Kotlin Instantiator construction.
     * True when globally enabled, or when {@code options.isKotlinSupport()} and
     * an Instantiator is registered.
     */
    public static boolean isActive(JsonOptions options) {
        if (instantiator == null) {
            return false;
        }
        if (enabled) {
            return true;
        }
        return options != null && options.isKotlinSupport();
    }

    /**
     * Instantiator to use when {@link #isActive} is true; otherwise the default
     * no-arg Instantiator.
     */
    public static ObjectInstantiator resolve(JsonOptions options) {
        if (isActive(options) && instantiator != null) {
            return instantiator;
        }
        return DefaultObjectInstantiator.INSTANCE;
    }
}
