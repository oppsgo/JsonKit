package io.github.oppsgo.json.kotlin

import io.github.oppsgo.json.JsonKit
import io.github.oppsgo.json.support.KotlinClasspath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Enablement / classpath smoke. Data class round-trips live in {@link KotlinDataClassTest}.
 */
class KotlinSupportContractTest {

    @BeforeEach
    fun setUp() {
        JsonKit.clear()
        JsonKitKotlin.enable()
    }

    @AfterEach
    fun tearDown() {
        JsonKitKotlin.disable()
        JsonKit.clear()
    }

    @Test
    fun classpathIsAtLeastStdlibInTests() {
        assertTrue(KotlinClasspath.state() >= KotlinClasspath.STATE_STDLIB)
    }

    @Test
    fun enableRegistersKotlinSupport() {
        assertTrue(JsonKitKotlin.isEnabled())
    }
}
