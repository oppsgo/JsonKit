package io.github.oppsgo.json.kotlin

import io.github.oppsgo.json.support.DefaultObjectInstantiator
import io.github.oppsgo.json.support.KotlinSupport
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Instantiator micro bench (module-local). Engine contract tests live in adapter modules
 * via {@link KotlinDataClassContractTest}.
 */
class KotlinInstantiatorMicroBenchTest {

    class JavaPojo {
        @JvmField
        var id: Int = 0

        @JvmField
        var name: String? = null
    }

    @BeforeEach
    fun enable() {
        JsonKitKotlin.enable()
    }

    @AfterEach
    fun disable() {
        JsonKitKotlin.disable()
    }

    @Test
    fun kotlinConstructorPathNotSlowerThanNoArgFieldPathByLargeMargin() {
        val kotlin = KotlinSupport.getInstantiator()!!
        val javaDefault = DefaultObjectInstantiator.INSTANCE

        val props = LinkedHashMap<String, Any>()
        props["id"] = 1
        props["name"] = "bench"

        for (i in 0 until 2_000) {
            kotlin.instantiate(SimpleUser::class.java, props)
            val pojo = javaDefault.instantiate(JavaPojo::class.java, props) as JavaPojo
            pojo.id = 1
            pojo.name = "bench"
        }

        val n = 20_000
        var t0 = System.nanoTime()
        for (i in 0 until n) {
            kotlin.instantiate(SimpleUser::class.java, props)
        }
        val kotlinNs = System.nanoTime() - t0

        t0 = System.nanoTime()
        for (i in 0 until n) {
            val pojo = javaDefault.instantiate(JavaPojo::class.java, props) as JavaPojo
            pojo.id = 1
            pojo.name = "bench"
        }
        val javaNs = System.nanoTime() - t0

        val kotlinAvg = kotlinNs / n.toDouble()
        val javaAvg = javaNs / n.toDouble()
        println("Kotlin Instantiator avg ns=$kotlinAvg Java no-arg+field avg ns=$javaAvg ratio=${kotlinAvg / javaAvg}")

        assertTrue(
            kotlinAvg < javaAvg * 5.0,
            "Kotlin Instantiator path unexpectedly slow: $kotlinAvg vs $javaAvg",
        )
    }
}
