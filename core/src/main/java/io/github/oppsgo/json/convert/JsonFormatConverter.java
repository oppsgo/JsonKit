package io.github.oppsgo.json.convert;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import io.github.oppsgo.json.annotation.JsonFormat;

/**
 * Built-in temporal converter driven by {@link FormatSpec}.
 * Supports {@link Date}, {@link Calendar}, and {@link java.time.Instant}.
 */
public final class JsonFormatConverter {

    private JsonFormatConverter() {
    }

    public static Object serialize(Object value, FormatSpec format, Type fieldType) {
        if (value == null) {
            throw new IllegalArgumentException("value == null");
        }
        if (format == null) {
            throw new IllegalArgumentException("format == null");
        }
        Class<?> raw = rawClass(fieldType);
        long millis = toEpochMillis(value, raw);
        if (format.getShape() == JsonFormat.Shape.NUMBER) {
            return millis;
        }
        String pattern = format.getPattern();
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException(
                    "@JsonFormat STRING shape requires a non-empty pattern for " + raw.getName());
        }
        SimpleDateFormat sdf = newSimpleDateFormat(pattern, format.resolvedTimezone());
        return sdf.format(new Date(millis));
    }

    public static Object deserialize(Object jsonValue, FormatSpec format, Type fieldType) {
        if (jsonValue == null) {
            return null;
        }
        if (format == null) {
            throw new IllegalArgumentException("format == null");
        }
        Class<?> raw = rawClass(fieldType);
        long millis;
        if (format.getShape() == JsonFormat.Shape.NUMBER) {
            millis = parseEpochMillis(jsonValue);
        } else {
            String pattern = format.getPattern();
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException(
                        "@JsonFormat STRING shape requires a non-empty pattern for " + raw.getName());
            }
            String text = String.valueOf(jsonValue);
            SimpleDateFormat sdf = newSimpleDateFormat(pattern, format.resolvedTimezone());
            try {
                Date parsed = sdf.parse(text);
                millis = parsed.getTime();
            } catch (ParseException e) {
                throw new IllegalArgumentException(
                        "Cannot parse '" + text + "' with pattern '" + pattern + "'", e);
            }
        }
        return fromEpochMillis(millis, raw);
    }

    public static boolean isSupportedTemporal(Class<?> raw) {
        if (raw == null) {
            return false;
        }
        return Date.class.isAssignableFrom(raw)
                || Calendar.class.isAssignableFrom(raw)
                || raw == java.time.Instant.class;
    }

    private static SimpleDateFormat newSimpleDateFormat(String pattern, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setLenient(false);
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf;
    }

    private static long toEpochMillis(Object value, Class<?> raw) {
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof Calendar) {
            return ((Calendar) value).getTimeInMillis();
        }
        if (value instanceof java.time.Instant) {
            return ((java.time.Instant) value).toEpochMilli();
        }
        throw unsupported(raw != null ? raw : value.getClass());
    }

    private static Object fromEpochMillis(long millis, Class<?> raw) {
        if (Date.class == raw || java.sql.Date.class == raw || java.sql.Timestamp.class == raw) {
            if (java.sql.Timestamp.class == raw) {
                return new java.sql.Timestamp(millis);
            }
            if (java.sql.Date.class == raw) {
                return new java.sql.Date(millis);
            }
            return new Date(millis);
        }
        if (Date.class.isAssignableFrom(raw)) {
            return new Date(millis);
        }
        if (Calendar.class.isAssignableFrom(raw)) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(millis);
            return calendar;
        }
        if (raw == java.time.Instant.class) {
            return java.time.Instant.ofEpochMilli(millis);
        }
        throw unsupported(raw);
    }

    private static long parseEpochMillis(Object jsonValue) {
        if (jsonValue instanceof Number) {
            return ((Number) jsonValue).longValue();
        }
        if (jsonValue instanceof String) {
            String text = ((String) jsonValue).trim();
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "@JsonFormat NUMBER shape expects a numeric value, got: " + jsonValue, e);
            }
        }
        throw new IllegalArgumentException(
                "@JsonFormat NUMBER shape expects a Number or numeric String, got: "
                        + jsonValue.getClass().getName());
    }

    private static IllegalArgumentException unsupported(Class<?> raw) {
        return new IllegalArgumentException(
                "@JsonFormat is not supported for type " + (raw != null ? raw.getName() : "null")
                        + "; supported: java.util.Date, java.util.Calendar, java.time.Instant");
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof java.lang.reflect.ParameterizedType) {
            Type raw = ((java.lang.reflect.ParameterizedType) type).getRawType();
            if (raw instanceof Class) {
                return (Class<?>) raw;
            }
        }
        throw new IllegalArgumentException("Cannot resolve raw class for " + type);
    }
}
