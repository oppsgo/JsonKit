package io.github.oppsgo.json.kotlin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.oppsgo.json.support.KotlinClasspath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STATE profile smoke checks. Test classpath includes kotlin-reflect → STATE 2.
 * STATE 1 (stdlib only) is covered by compileOnly publication (no reflect api dep).
 */
public class KotlinClasspathStateTest {

    @BeforeEach
    public void enable() {
        JsonKitKotlin.enable();
    }

    @AfterEach
    public void disable() {
        JsonKitKotlin.disable();
    }

    @Test
    public void testClasspathIsState2WithReflect() {
        assertTrue(KotlinClasspath.state() >= KotlinClasspath.STATE_STDLIB);
        assertTrue(KotlinClasspath.hasStdlib());
        // Test deps include kotlin-reflect.
        assertTrue(KotlinClasspath.hasReflect());
        assertEquals(KotlinClasspath.STATE_REFLECT, KotlinClasspath.state());
    }
}
