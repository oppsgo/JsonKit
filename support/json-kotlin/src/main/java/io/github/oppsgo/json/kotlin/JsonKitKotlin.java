package io.github.oppsgo.json.kotlin;

import io.github.oppsgo.json.support.KotlinSupport;

/**
 * Opt-in entry point for JsonKit Kotlin constructor binding.
 * <p>
 * Call once at process start (before building adapter factories that need Kotlin):
 * <pre>{@code
 * JsonKitKotlin.enable();
 * JsonKit.setDefault(Fastjson2AdapterFactory.of());
 * }</pre>
 * Consumers must depend on {@code kotlin-stdlib} (STATE 1). Add {@code kotlin-reflect}
 * for richer parameter names (STATE 2). Minimum Kotlin: <b>1.6.21</b>.
 * <p>
 * Fastjson 1.x is out of scope for idiomatic Kotlin {@code data class} binding;
 * use Fastjson2, Moshi, or Gson with Kotlin support enabled.
 */
public final class JsonKitKotlin {

    private JsonKitKotlin() {
    }

    /**
     * Registers the Kotlin Instantiator and enables process-wide Kotlin support.
     */
    public static void enable() {
        KotlinSupport.enable(KotlinObjectInstantiator.INSTANCE);
    }

    /**
     * Clears Kotlin Instantiator registration (tests / teardown).
     */
    public static void disable() {
        KotlinSupport.disable();
    }

    /**
     * Whether {@link #enable()} has been called.
     */
    public static boolean isEnabled() {
        return KotlinSupport.isEnabled();
    }
}
