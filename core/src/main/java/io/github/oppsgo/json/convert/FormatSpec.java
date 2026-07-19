package io.github.oppsgo.json.convert;

import java.util.Objects;

import io.github.oppsgo.json.annotation.JsonFormat;

/**
 * Immutable snapshot of {@link JsonFormat} on a field.
 */
public final class FormatSpec {

    private final String pattern;
    private final String timezone;
    private final JsonFormat.Shape shape;

    public FormatSpec(String pattern, String timezone, JsonFormat.Shape shape) {
        this.pattern = pattern != null ? pattern : "";
        this.timezone = timezone != null ? timezone : "";
        this.shape = shape != null ? shape : JsonFormat.Shape.STRING;
    }

    public static FormatSpec from(JsonFormat format) {
        if (format == null) {
            return null;
        }
        return new FormatSpec(format.pattern(), format.timezone(), format.shape());
    }

    public String getPattern() {
        return pattern;
    }

    public String getTimezone() {
        return timezone;
    }

    /**
     * Resolved timezone id (empty annotation → {@code UTC}).
     */
    public String resolvedTimezone() {
        return timezone.isEmpty() ? "UTC" : timezone;
    }

    public JsonFormat.Shape getShape() {
        return shape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FormatSpec)) {
            return false;
        }
        FormatSpec that = (FormatSpec) o;
        return pattern.equals(that.pattern)
                && timezone.equals(that.timezone)
                && shape == that.shape;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, timezone, shape);
    }
}
