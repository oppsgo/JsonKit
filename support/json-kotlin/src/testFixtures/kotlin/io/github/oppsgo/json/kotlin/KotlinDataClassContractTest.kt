package io.github.oppsgo.json.kotlin

import io.github.oppsgo.json.JsonKit
import io.github.oppsgo.json.JsonOptions
import io.github.oppsgo.json.adapter.JsonAdapter
import io.github.oppsgo.json.reflect.JsonTypeReference
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Shared Kotlin {@code data class} contract tests (exported as {@code testFixtures}).
 * <p>
 * Adapter modules should extend this class, depend on
 * {@code testImplementation(testFixtures(project(":support:json-kotlin")))}
 * and {@code testImplementation(project(":support:json-kotlin"))}, and implement
 * {@link #createAdapter(JsonOptions)}.
 */
abstract class KotlinDataClassContractTest {

    protected lateinit var json: JsonAdapter

    /**
     * Builds the adapter under test.
     */
    protected abstract fun createAdapter(options: JsonOptions?): JsonAdapter

    @BeforeEach
    fun setUpKotlinContract() {
        JsonKit.clear()
        JsonKitKotlin.enable()
        json = createAdapter(JsonOptions.Builder().setSerializeNulls(true).build())
    }

    @AfterEach
    fun tearDownKotlinContract() {
        JsonKitKotlin.disable()
        JsonKit.clear()
    }

    @Test
    fun simpleDataClassRoundTrip() {
        val original = SimpleUser(1, "Ada")
        val encoded = json.toJson(original)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains("\"name\""))

        val parsed = json.fromJson("""{"id":1,"name":"Ada"}""", SimpleUser::class.java)
        assertEquals(original, parsed)
        assertEquals(original, json.fromJson(encoded, SimpleUser::class.java))
    }

    @Test
    fun dataClassWithDefaultParameters() {
        val user = json.fromJson("""{"id":7}""", UserWithDefaults::class.java)
        assertEquals(7, user.id)
        assertEquals("anonymous", user.name)
        assertEquals(true, user.active)

        val full = json.fromJson(
            """{"id":8,"name":"Eve","active":false}""",
            UserWithDefaults::class.java,
        )
        assertEquals(UserWithDefaults(8, "Eve", false), full)
    }

    @Test
    fun nestedDataClass() {
        val payload = """{"id":2,"profile":{"id":9,"name":"Nested"}}"""
        val user = json.fromJson(payload, NestedUser::class.java)
        assertEquals(NestedUser(2, SimpleUser(9, "Nested")), user)
        assertEquals(user, json.fromJson(json.toJson(user), NestedUser::class.java))
    }

    @Test
    fun fieldJsonPropertyRename() {
        val user = json.fromJson("""{"user_name":"Bob","age":30}""", RenamedUser::class.java)
        assertEquals(RenamedUser("Bob", 30), user)

        val encoded = json.toJson(user)
        assertTrue(encoded.contains("user_name"), encoded)
        assertFalse(encoded.matches(Regex(""".*"userName"\s*:.*""")), encoded)
    }

    @Test
    fun fieldJsonAlias() {
        val viaAlias = json.fromJson("""{"nick":"Neo","id":10}""", AliasedUser::class.java)
        assertEquals(AliasedUser("Neo", 10), viaAlias)

        val viaCanonical = json.fromJson("""{"name":"Trinity","id":11}""", AliasedUser::class.java)
        assertEquals(AliasedUser("Trinity", 11), viaCanonical)
    }

    @Test
    fun fieldJsonIgnoreOnSerializeAndDeserialize() {
        val user = json.fromJson(
            """{"id":1,"name":"Alice","secret":"leak"}""",
            IgnoredFieldUser::class.java,
        )
        assertEquals(1, user.id)
        assertEquals("Alice", user.name)
        assertEquals("hidden", user.secret)

        val encoded = json.toJson(IgnoredFieldUser(2, "Bob", "top-secret"))
        assertFalse(encoded.contains("secret"), encoded)
        assertTrue(encoded.contains("\"id\""), encoded)
    }

    @Test
    fun nullablePropertyOmittedOrNull() {
        val omitted = json.fromJson("""{"id":3}""", NullableUser::class.java)
        assertEquals(3, omitted.id)
        assertNull(omitted.nickname)

        val explicitNull = json.fromJson("""{"id":4,"nickname":null}""", NullableUser::class.java)
        assertEquals(NullableUser(4, null), explicitNull)
    }

    @Test
    fun listOfDataClassViaTypeReference() {
        val listJson = """[{"id":1,"name":"A"},{"id":2,"name":"B"}]"""
        val list = json.fromJson(listJson, object : JsonTypeReference<List<SimpleUser>>() {})
        assertEquals(listOf(SimpleUser(1, "A"), SimpleUser(2, "B")), list)

        val teamJson = """{"name":"core","members":[{"id":1,"name":"A"},{"id":2,"name":"B"}]}"""
        val team = json.fromJson(teamJson, Team::class.java)
        assertEquals("core", team.name)
        assertEquals(2, team.members.size)
        assertEquals(SimpleUser(1, "A"), team.members[0])
    }
}
