package io.github.oppsgo.json;

/**
 * Shared options for {@link io.github.oppsgo.json.adapter.JsonAdapter} implementations.
 * Bound when an adapter (or its factory) is constructed; not changed per call.
 */
public final class JsonOptions {

    private final boolean serializeNulls;
    private final boolean kotlinSupport;

    private JsonOptions(Builder builder) {
        this.serializeNulls = builder.serializeNulls;
        this.kotlinSupport = builder.kotlinSupport;
    }

    /**
     * Copy constructor. {@code other == null} yields {@link #defaults()}.
     */
    public JsonOptions(JsonOptions other) {
        if (other == null) {
            this.serializeNulls = false;
            this.kotlinSupport = false;
            return;
        }
        this.serializeNulls = other.serializeNulls;
        this.kotlinSupport = other.kotlinSupport;
    }

    /**
     * Default options ({@code serializeNulls = false}, {@code kotlinSupport = false}).
     */
    public static JsonOptions defaults() {
        return new Builder().build();
    }

    /**
     * Whether {@code null} fields are written into JSON output.
     */
    public boolean isSerializeNulls() {
        return serializeNulls;
    }

    /**
     * When {@code true}, adapters may use a registered Kotlin Instantiator
     * (see {@code JsonKitKotlin.enable()} / {@link io.github.oppsgo.json.support.KotlinSupport})
     * for {@code kotlin.Metadata} types. Has no effect unless an Instantiator is registered.
     */
    public boolean isKotlinSupport() {
        return kotlinSupport;
    }

    /**
     * Builder seeded from this instance.
     */
    public Builder newBuilder() {
        return new Builder()
                .setSerializeNulls(serializeNulls)
                .setKotlinSupport(kotlinSupport);
    }

    /**
     * Builds {@link JsonOptions}.
     */
    public static final class Builder {
        private boolean serializeNulls;
        private boolean kotlinSupport;

        /**
         * When {@code true}, adapters include null-valued fields in serialized JSON.
         * Default is {@code false} (omit nulls).
         */
        public Builder setSerializeNulls(boolean serializeNulls) {
            this.serializeNulls = serializeNulls;
            return this;
        }

        /**
         * When {@code true}, this adapter opts into Kotlin Instantiator construction
         * if {@link io.github.oppsgo.json.support.KotlinSupport} has a registered Instantiator.
         * Prefer {@code JsonKitKotlin.enable()} for process-wide enablement.
         */
        public Builder setKotlinSupport(boolean kotlinSupport) {
            this.kotlinSupport = kotlinSupport;
            return this;
        }

        public JsonOptions build() {
            return new JsonOptions(this);
        }
    }
}
