package com.matuw.json.config;

/**
 * @author Shihwan
 */
public class JsonOptions {
    private boolean serializeNulls;

    public JsonOptions(JsonOptions options) {
        if (options == null) return;

        this.serializeNulls = options.serializeNulls;
    }

    protected JsonOptions(Builder builder) {
        this.serializeNulls = builder.serializeNulls;
    }

    public boolean isSerializeNulls() {
        return serializeNulls;
    }

    public static class Builder {
        boolean serializeNulls;

        public Builder() {
        }

        public Builder setSerializeNulls(boolean serializeNulls) {
            this.serializeNulls = serializeNulls;
            return this;
        }

        public JsonOptions build() {
            return new JsonOptions(this);
        }
    }
}
