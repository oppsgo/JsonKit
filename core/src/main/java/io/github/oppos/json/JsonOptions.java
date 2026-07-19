package io.github.oppos.json;

/**
 * Shared options for {@link io.github.oppos.json.adapter.JsonAdapter} implementations.
 * Bound when an adapter (or its factory) is constructed; not changed per call.
 */
public final class JsonOptions {

    private final boolean serializeNulls;

    private JsonOptions(Builder builder) {
        this.serializeNulls = builder.serializeNulls;
    }

    /**
     * Copy constructor. {@code other == null} yields {@link #defaults()}.
     */
    public JsonOptions(JsonOptions other) {
        if (other == null) {
            this.serializeNulls = false;
            return;
        }
        this.serializeNulls = other.serializeNulls;
    }

    /**
     * Default options ({@code serializeNulls = false}).
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
     * Builder seeded from this instance.
     */
    public Builder newBuilder() {
        return new Builder().setSerializeNulls(serializeNulls);
    }

    /**
     * Builds {@link JsonOptions}.
     */
    public static final class Builder {
        private boolean serializeNulls;

        /**
         * When {@code true}, adapters include null-valued fields in serialized JSON.
         * Default is {@code false} (omit nulls).
         */
        public Builder setSerializeNulls(boolean serializeNulls) {
            this.serializeNulls = serializeNulls;
            return this;
        }

        public JsonOptions build() {
            return new JsonOptions(this);
        }
    }
}
