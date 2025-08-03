package com.matuw.json;

/**
 * @author Shihwan
 */
class Utils {

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }
}
