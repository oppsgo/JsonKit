package io.github.oppsgo.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.annotation.JsonAlias;
import io.github.oppsgo.json.annotation.JsonIgnore;
import io.github.oppsgo.json.annotation.JsonIgnoreProperties;
import io.github.oppsgo.json.annotation.JsonProperty;
import io.github.oppsgo.json.reflect.JsonTypeReference;

/**
 * Shared contract tests for {@link JsonAdapter} (exported as {@code testFixtures}).
 * <p>
 * Adapter modules should extend this class, depend on
 * {@code testImplementation(testFixtures(project(":core")))}, and implement
 * {@link #createAdapter(JsonOptions)}. Fixture models use public fields so
 * field-binding adapters are exercised. Requires JUnit 5 (Jupiter).
 */
public abstract class JsonContractTest {

    protected JsonAdapter json;
    protected User testUser;
    protected List<User> testUserList;
    protected Map<String, User> testUserMap;
    protected String invalidJson = "{invalid json}";

    /**
     * Builds the adapter under test. {@code setUp} calls this with
     * {@code serializeNulls = true}; individual tests may call again with other options.
     */
    protected abstract JsonAdapter createAdapter(JsonOptions options);

    @BeforeEach
    public void setUp() {
        JsonKit.clear();
        testUser = new User("Alice", 30, "alice@example.com");
        testUserList = new ArrayList<User>();
        testUserList.add(testUser);
        testUserList.add(new User("Bob", 25, null));
        testUserMap = new HashMap<String, User>();
        testUserMap.put("admin", testUser);
        json = createAdapter(new JsonOptions.Builder().setSerializeNulls(true).build());
    }

    @Test
    public void testBasicTypeSerialization() {
        assertEquals("\"test\"", json.toJson("test"));
        assertEquals("123", json.toJson(123));
        assertEquals("true", json.toJson(true));
        assertEquals("false", json.toJson(false));
        assertEquals("\"\"", json.toJson(""));
    }

    @Test
    public void testNullRootValue() {
        String encoded = json.toJson(null);
        assertTrue(encoded == null || "null".equals(encoded));

        assertNull(json.fromJson((String) null, String.class));
        assertNull(json.fromJson((String) null, Integer.class));
        assertNull(json.fromJson((String) null, User.class));
        assertNull(json.fromJson("null", String.class));
        assertNull(json.fromJson("null", Integer.class));
        assertNull(json.fromJson("null", User.class));
    }

    @Test
    public void testNullObject() throws IOException {
        String encoded = json.toJson(null);
        assertTrue(encoded == null || "null".equals(encoded));
        assertNull(json.fromJson("null", User.class));
        assertNull(json.fromJson((String) null, User.class));

        assertNull(json.fromJson("null", new JsonTypeReference<User>() {
        }));
        assertNull(json.fromJson((String) null, new JsonTypeReference<List<User>>() {
        }));
        assertNull(json.fromJson("null", new JsonTypeReference<List<User>>() {
        }));

        assertNull(json.fromJson(new StringReader("null"), User.class));
        assertNull(json.fromJson(new StringReader("null"), new JsonTypeReference<User>() {
        }));

        StringWriter writer = new StringWriter();
        json.toJson(null, writer);
        String written = writer.toString();
        assertTrue(written.isEmpty() || "null".equals(written));
    }

    @Test
    public void testStringRoundTrip() {
        assertEquals("hello", json.fromJson("\"hello\"", String.class));
        assertEquals("", json.fromJson("\"\"", String.class));
        assertEquals("hello", json.fromJson(requireJson(json.toJson("hello")), String.class));
        assertEquals("a\"b", json.fromJson(requireJson(json.toJson("a\"b")), String.class));
    }

    @Test
    public void testPrimitiveRoundTrip() {
        assertEquals(Integer.valueOf(123), json.fromJson("123", Integer.class));
        assertEquals(Integer.valueOf(-7), json.fromJson(requireJson(json.toJson(-7)), Integer.class));
        assertEquals(Long.valueOf(99L), json.fromJson("99", Long.class));
        assertEquals(Boolean.TRUE, json.fromJson("true", Boolean.class));
        assertEquals(Boolean.FALSE, json.fromJson(requireJson(json.toJson(false)), Boolean.class));
        assertEquals(Double.valueOf(1.5), json.fromJson("1.5", Double.class));
        Double decoded = requireDecoded(json.fromJson(requireJson(json.toJson(1.5)), Double.class));
        assertEquals(1.5, decoded, 0.0);
    }

    @Test
    public void testObjectRoundTrip() {
        String userJson = requireJson(json.toJson(testUser));
        assertContainsAny(userJson, "\"name\":\"Alice\"", "\"name\": \"Alice\"");
        assertContainsAny(userJson, "\"age\":30", "\"age\": 30");
        User decoded = requireDecoded(json.fromJson(userJson, User.class));
        assertEquals(testUser, decoded);
    }

    @Test
    public void testReaderWriterRoundTrip() throws IOException {
        StringWriter writer = new StringWriter();
        json.toJson(testUser, writer);
        String encoded = writer.toString();
        assertContainsAny(encoded, "\"name\":\"Alice\"", "\"name\": \"Alice\"");

        User decoded = requireDecoded(json.fromJson(new StringReader(encoded), User.class));
        assertEquals(testUser, decoded);

        List<User> list = requireDecoded(json.fromJson(
                new StringReader(requireJson(json.toJson(testUserList))),
                new JsonTypeReference<List<User>>() {
                }));
        assertEquals(2, list.size());
        assertEquals(testUser, list.get(0));
    }

    @Test
    public void testReaderWriterListAndMapRoundTrip() throws IOException {
        StringWriter listWriter = new StringWriter();
        json.toJson(testUserList, listWriter);
        String listJson = listWriter.toString();
        assertTrue(listJson.startsWith("["), listJson);

        List<User> list = requireDecoded(json.fromJson(
                new StringReader(listJson),
                new JsonTypeReference<List<User>>() {
                }));
        assertEquals(2, list.size());
        assertEquals(testUser, list.get(0));

        StringWriter mapWriter = new StringWriter();
        json.toJson(testUserMap, mapWriter);
        String mapJson = mapWriter.toString();
        assertTrue(mapJson.startsWith("{"), mapJson);

        Map<String, User> map = requireDecoded(json.fromJson(
                new StringReader(mapJson),
                new JsonTypeReference<Map<String, User>>() {
                }));
        assertEquals(1, map.size());
        assertEquals(testUser, map.get("admin"));
    }

    @Test
    public void testReaderWriterNestedGenericRoundTrip() throws IOException {
        List<Map<String, User>> nested = new ArrayList<Map<String, User>>();
        nested.add(testUserMap);

        StringWriter writer = new StringWriter();
        json.toJson(nested, writer);
        String encoded = writer.toString();

        JsonTypeReference<List<Map<String, User>>> reference =
                new JsonTypeReference<List<Map<String, User>>>() {
                };
        List<Map<String, User>> viaReference = requireDecoded(
                json.fromJson(new StringReader(encoded), reference));
        assertEquals(1, viaReference.size());
        assertEquals(testUser, viaReference.get(0).get("admin"));

        Type type = reference.getType();
        List<Map<String, User>> viaType = requireDecoded(
                json.fromJson(new StringReader(encoded), type));
        assertEquals(1, viaType.size());
        assertEquals(testUser, viaType.get(0).get("admin"));
    }

    @Test
    public void testReaderWriterDoNotCloseStreams() throws IOException {
        TrackingStringWriter writer = new TrackingStringWriter();
        json.toJson(testUser, writer);
        assertFalse(writer.closed, "toJson(Writer) must not close the writer");
        assertTrue(writer.toString().length() > 0);

        TrackingStringReader reader = new TrackingStringReader(writer.toString());
        User decoded = requireDecoded(json.fromJson(reader, User.class));
        assertEquals(testUser, decoded);
        assertFalse(reader.closed, "fromJson(Reader, Class) must not close the reader");

        TrackingStringReader typedReader = new TrackingStringReader(requireJson(json.toJson(testUserList)));
        List<User> list = requireDecoded(json.fromJson(
                typedReader,
                new JsonTypeReference<List<User>>() {
                }));
        assertEquals(2, list.size());
        assertFalse(typedReader.closed, "fromJson(Reader, JsonTypeReference) must not close the reader");

        TrackingStringReader typeReader = new TrackingStringReader(requireJson(json.toJson(testUserMap)));
        Type mapType = new JsonTypeReference<Map<String, User>>() {
        }.getType();
        Map<String, User> map = requireDecoded(json.fromJson(typeReader, mapType));
        assertEquals(testUser, map.get("admin"));
        assertFalse(typeReader.closed, "fromJson(Reader, Type) must not close the reader");
    }

    @Test
    public void testSerializeNullsTrue() {
        User user = new User("Charlie", 28, null);
        String encoded = requireJson(json.toJson(user));
        assertContainsAny(encoded, "\"email\":null", "\"email\": null");
    }

    @Test
    public void testSerializeNullsFalse() {
        JsonAdapter local = createAdapter(new JsonOptions.Builder().setSerializeNulls(false).build());
        User user = new User("Charlie", 28, null);
        String encoded = requireJson(local.toJson(user));
        assertContainsAny(encoded, "\"name\":\"Charlie\"", "\"name\": \"Charlie\"");
        assertFalse(encoded.contains("\"email\":null") || encoded.contains("\"email\": null"));
    }

    @Test
    public void testListTypeReference() {
        String listJson = requireJson(json.toJson(testUserList));
        assertTrue(listJson.startsWith("["));
        List<User> decoded = requireDecoded(json.fromJson(listJson, new JsonTypeReference<List<User>>() {
        }));
        assertEquals(2, decoded.size());
        assertEquals(testUser, decoded.get(0));
        assertNull(decoded.get(1).email);
    }

    @Test
    public void testMapTypeReference() {
        String mapJson = requireJson(json.toJson(testUserMap));
        assertTrue(mapJson.startsWith("{"));
        Map<String, User> decoded = requireDecoded(json.fromJson(
                mapJson, new JsonTypeReference<Map<String, User>>() {
                }));
        assertEquals(1, decoded.size());
        assertEquals(testUser, decoded.get("admin"));
    }

    @Test
    public void testNestedGenericType() {
        List<Map<String, User>> nested = new ArrayList<Map<String, User>>();
        nested.add(testUserMap);
        String encoded = requireJson(json.toJson(nested));
        List<Map<String, User>> decoded = requireDecoded(json.fromJson(
                encoded, new JsonTypeReference<List<Map<String, User>>>() {
                }));
        assertEquals(1, decoded.size());
        assertEquals(testUser, decoded.get(0).get("admin"));
    }

    @Test
    public void testInvalidJson() {
        assertThrows(Exception.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                json.fromJson(invalidJson, User.class);
            }
        });
    }

    @Test
    public void testJsonPropertyRename() {
        NamedUser named = new NamedUser();
        named.userName = "neo";
        String encoded = requireJson(json.toJson(named));
        assertContains(encoded, "\"user_name\"");
        assertNotContains(encoded, "\"userName\"");
        NamedUser decoded = requireDecoded(json.fromJson("{\"user_name\":\"trinity\"}", NamedUser.class));
        assertEquals("trinity", decoded.userName);
    }

    @Test
    public void testJsonIgnoreBothWays() {
        SecretUser secret = new SecretUser();
        secret.name = "visible";
        secret.password = "hidden";
        String encoded = requireJson(json.toJson(secret));
        assertContains(encoded, "\"name\"");
        assertNotContains(encoded, "password");
        SecretUser decoded = requireDecoded(json.fromJson(
                "{\"name\":\"a\",\"password\":\"leak\"}", SecretUser.class));
        assertEquals("a", decoded.name);
        assertNull(decoded.password);
    }

    @Test
    public void testJsonAliasDeserialize() {
        NamedUser decoded = requireDecoded(json.fromJson("{\"userName\":\"alias\"}", NamedUser.class));
        assertEquals("alias", decoded.userName);
    }

    @Test
    public void testJsonIgnoreProperties() {
        IgnoredPropsUser user = new IgnoredPropsUser();
        user.name = "ok";
        user.debug = "nope";
        String encoded = requireJson(json.toJson(user));
        assertContains(encoded, "\"name\"");
        assertNotContains(encoded, "debug");
        IgnoredPropsUser decoded = requireDecoded(json.fromJson(
                "{\"name\":\"x\",\"debug\":\"y\"}", IgnoredPropsUser.class));
        assertEquals("x", decoded.name);
        assertNull(decoded.debug);
    }

    @Test
    public void testInheritedFieldsRoundTrip() {
        Employee employee = new Employee();
        employee.id = "p1";
        employee.displayName = "Ada";
        employee.title = "Engineer";
        String encoded = requireJson(json.toJson(employee));
        assertContainsAny(encoded, "\"person_id\":\"p1\"", "\"person_id\": \"p1\"");
        assertContains(encoded, "\"display_name\"");
        assertContains(encoded, "\"title\"");
        assertNotContains(encoded, "\"id\"");
        assertNotContains(encoded, "\"displayName\"");

        Employee decoded = requireDecoded(json.fromJson(
                "{\"person_id\":\"p2\",\"display_name\":\"Lin\",\"title\":\"PM\"}",
                Employee.class));
        assertEquals("p2", decoded.id);
        assertEquals("Lin", decoded.displayName);
        assertEquals("PM", decoded.title);
    }

    @Test
    public void testInheritedJsonIgnore() {
        Employee employee = new Employee();
        employee.id = "p1";
        employee.displayName = "Ada";
        employee.internalToken = "secret";
        employee.title = "Engineer";
        String encoded = requireJson(json.toJson(employee));
        assertNotContains(encoded, "internalToken");
        assertNotContains(encoded, "secret");

        Employee decoded = requireDecoded(json.fromJson(
                "{\"person_id\":\"p1\",\"display_name\":\"Ada\",\"title\":\"Eng\","
                        + "\"internalToken\":\"leak\"}",
                Employee.class));
        assertNull(decoded.internalToken);
        assertEquals("Eng", decoded.title);
    }

    @Test
    public void testInheritedJsonAlias() {
        Employee decoded = requireDecoded(json.fromJson(
                "{\"pid\":\"p9\",\"name\":\"Alias\",\"title\":\"Dev\"}",
                Employee.class));
        assertEquals("p9", decoded.id);
        assertEquals("Alias", decoded.displayName);
        assertEquals("Dev", decoded.title);
    }

    @Test
    public void testInheritedJsonIgnoreProperties() {
        Employee employee = new Employee();
        employee.id = "p1";
        employee.displayName = "Ada";
        employee.debug = "noise";
        employee.title = "Engineer";
        String encoded = requireJson(json.toJson(employee));
        assertContains(encoded, "\"title\"");
        assertNotContains(encoded, "debug");

        Employee decoded = requireDecoded(json.fromJson(
                "{\"person_id\":\"p1\",\"display_name\":\"Ada\",\"title\":\"Eng\",\"debug\":\"y\"}",
                Employee.class));
        assertEquals("Eng", decoded.title);
        assertNull(decoded.debug);
    }

    @Test
    public void testDefaultFactory() {
        final JsonAdapter shared = json;
        JsonKit.setDefault(new JsonAdapter.Factory() {
            @Override
            public JsonAdapter create() {
                return shared;
            }
        });
        assertTrue(JsonKit.hasDefault());
        assertSame(JsonKit.getDefault(), shared);
        assertEquals(shared.toJson(testUser), JsonKit.getDefault().toJson(testUser));
    }

    @Test
    public void testNamedFactoriesForDifferentOptions() {
        final JsonAdapter withNulls = createAdapter(
                new JsonOptions.Builder().setSerializeNulls(true).build());
        final JsonAdapter withoutNulls = createAdapter(
                new JsonOptions.Builder().setSerializeNulls(false).build());

        JsonKit.register("withNulls", new JsonAdapter.Factory() {
            @Override
            public JsonAdapter create() {
                return withNulls;
            }
        });
        JsonKit.register("withoutNulls", new JsonAdapter.Factory() {
            @Override
            public JsonAdapter create() {
                return withoutNulls;
            }
        });

        assertTrue(JsonKit.has("withNulls"));
        assertTrue(JsonKit.has("withoutNulls"));
        assertTrue(JsonKit.get("withNulls").getOptions().isSerializeNulls());
        assertFalse(JsonKit.get("withoutNulls").getOptions().isSerializeNulls());
    }

    @Test
    public void testNullNameMeansDefault() {
        final JsonAdapter shared = json;
        JsonKit.register(null, new JsonAdapter.Factory() {
            @Override
            public JsonAdapter create() {
                return shared;
            }
        });
        assertTrue(JsonKit.has(null));
        assertTrue(JsonKit.hasDefault());
        assertSame(JsonKit.get(null), shared);
        assertSame(JsonKit.getDefault(), shared);
    }

    @Test
    public void testLazyFactory() {
        final int[] created = new int[]{0};
        final JsonOptions options = json.getOptions();
        JsonKit.setDefault(new JsonAdapter.Factory() {
            private JsonAdapter cached;

            @Override
            public JsonAdapter create() {
                if (cached == null) {
                    created[0]++;
                    cached = createAdapter(options);
                }
                return cached;
            }
        });
        JsonKit.getDefault();
        JsonKit.getDefault();
        assertEquals(1, created[0]);
    }

    private static String requireJson(String value) {
        return Objects.requireNonNull(value, "toJson returned null");
    }

    private static <T> T requireDecoded(T value) {
        return Objects.requireNonNull(value, "fromJson returned null");
    }

    private static void assertContains(String actual, String expected) {
        assertTrue(actual.contains(expected), "Expected [" + expected + "] in: " + actual);
    }

    private static void assertNotContains(String actual, String unexpected) {
        assertFalse(actual.contains(unexpected),
                "Did not expect [" + unexpected + "] in: " + actual);
    }

    private static void assertContainsAny(String actual, String first, String second) {
        assertTrue(actual.contains(first) || actual.contains(second),
                "Expected one of [" + first + ", " + second + "] in: " + actual);
    }

    /** Records whether {@link #close()} was called; used to assert adapters never close caller streams. */
    private static final class TrackingStringReader extends StringReader {
        boolean closed;

        TrackingStringReader(String data) {
            super(data);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }

    /** Records whether {@link #close()} was called; used to assert adapters never close caller streams. */
    private static final class TrackingStringWriter extends StringWriter {
        boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    public static class User {
        public String name;
        public int age;
        public String email;

        @SuppressWarnings("unused")
        public User() {
        }

        public User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof User)) {
                return false;
            }
            User user = (User) o;
            return age == user.age
                    && Objects.equals(name, user.name)
                    && Objects.equals(email, user.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age, email);
        }
    }

    public static class NamedUser {
        @JsonProperty("user_name")
        @JsonAlias({"userName"})
        public String userName;
    }

    public static class SecretUser {
        public String name;
        @JsonIgnore
        public String password;
    }

    @JsonIgnoreProperties({"debug"})
    public static class IgnoredPropsUser {
        public String name;
        public String debug;
    }

    @JsonIgnoreProperties({"debug"})
    public static class Person {
        @JsonProperty("person_id")
        @JsonAlias({"pid"})
        public String id;

        @JsonProperty("display_name")
        @JsonAlias({"name"})
        public String displayName;

        @JsonIgnore
        public String internalToken;

        public String debug;
    }

    public static class Employee extends Person {
        public String title;
    }
}
