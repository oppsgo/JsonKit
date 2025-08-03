package com.matuw.json;


import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.adapter.JsonAdapterFactory;
import com.matuw.json.config.JsonOptions;
import com.matuw.json.reflect.JsonTypeReference;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 抽象测试基类 兼容JDK 1.6 + JUnit 4.12）
 */
public class JsonKitTest {
    protected JsonAdapter adapter;
    protected User testUser;
    protected List<User> testUserList;
    protected Map<String, User> testUserMap;
    protected String invalidJson = "{invalid json}";

    public static class User {
        private String name;
        private int age;
        private String email;

        public User() {
        }

        public User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return age == user.age &&
                    (name == null ? user.name == null : name.equals(user.name)) &&
                    (email == null ? user.email == null : email.equals(user.email));
        }
    }

    protected JsonAdapter createAdapter(JsonOptions options) {
        // TODO 测试对应的框架时需要替换成对应的工厂类
        JsonAdapterFactory factory = /* com.matuw.json.gson.GsonAdapterFactory.getInstance() */ null;
        return JsonKit.newBuilder(factory).setOptions(options).build();
    }

    @Before
    public void setUp() {
        testUser = new User("Alice", 30, "alice@example.com");

        testUserList = new ArrayList<User>();
        testUserList.add(testUser);
        testUserList.add(new User("Bob", 25, null));

        testUserMap = new HashMap<String, User>();
        testUserMap.put("admin", testUser);

        JsonOptions options = new JsonOptions.Builder()
                .setSerializeNulls(true)
                .build();

        this.adapter = createAdapter(options);
    }

    @Test
    public void testBasicTypeSerialization() {
        // 字符串
        String strJson = adapter.toJson("test");
        assertEquals("字符串序列化错误", "\"test\"", strJson);

        // 整数
        String intJson = adapter.toJson(123);
        assertEquals("整数序列化错误", "123", intJson);

        // 布尔值
        String boolJson = adapter.toJson(true);
        assertEquals("布尔值序列化错误", "true", boolJson);
    }

    @Test
    public void testObjectSerializationAndDeserialization() {
        // 序列化
        String userJson = adapter.toJson(testUser);
        assertTrue("JSON应包含name字段", userJson.contains("\"name\":\"Alice\""));
        assertTrue("JSON应包含age字段", userJson.contains("\"age\":30"));

        // 反序列化
        User deserializedUser = adapter.fromJson(userJson, User.class);
        assertNotNull("反序列化结果不应为null", deserializedUser);
        assertEquals("反序列化对象应与原始对象一致", testUser, deserializedUser);
    }

    @Test
    public void testNullValueSerialization() {
        User userWithNull = new User("Charlie", 28, null);
        String json = adapter.toJson(userWithNull);

        assertNotNull("序列化结果不应为null", json);
        assertTrue("应包含非空字段name", json.contains("\"name\":\"Charlie\""));
        assertTrue("配置serializeNulls=true时应包含null字段email", json.contains("\"email\":null"));
    }

    @Test
    public void testNonNullValueSerialization() {
        User userWithNull = new User("Charlie", 28, null);
        JsonAdapter adapter = createAdapter(new JsonOptions.Builder().setSerializeNulls(false).build());
        String json = adapter.toJson(userWithNull);

        assertNotNull("序列化结果不应为null", json);
        assertTrue("应包含非空字段name", json.contains("\"name\":\"Charlie\""));
        assertFalse("配置serializeNulls=false时不包含null字段email", json.contains("\"email\":null"));
    }

    @Test
    public void testCollectionDeserialization() {
        String listJson = adapter.toJson(testUserList);

        assertNotNull("List序列化结果不应为null", listJson);
        assertTrue("List序列化应返回JSON数组", listJson.startsWith("["));
        assertTrue("数组应包含第一个元素", listJson.contains("\"name\":\"Alice\""));
        assertTrue("数组应包含第二个元素", listJson.contains("\"name\":\"Bob\""));

        // 反序列化
        JsonTypeReference<List<User>> listType = new JsonTypeReference<List<User>>() {
        };
        List<User> deserializedList = adapter.fromJson(listJson, listType);

        assertNotNull("反序列化List不应为null", deserializedList);
        assertEquals("List大小应与原始一致", 2, deserializedList.size());
        assertEquals("第一个元素应匹配", testUser, deserializedList.get(0));
        assertNull("第二个元素的null字段应保留", deserializedList.get(1).getEmail());
    }

    @Test
    public void testMapDeserialization() {
        String mapJson = adapter.toJson(testUserMap);

        assertNotNull("Map序列化结果不应为null", mapJson);
        assertTrue("Map序列化应返回JSON对象", mapJson.startsWith("{"));
        assertTrue("应包含admin键", mapJson.contains("\"admin\""));

        // 反序列化
        JsonTypeReference<Map<String, User>> mapType = new JsonTypeReference<Map<String, User>>() {
        };
        Map<String, User> deserializedMap = adapter.fromJson(mapJson, mapType);

        assertNotNull("反序列化Map不应为null", deserializedMap);
        assertEquals("Map大小应与原始一致", 1, deserializedMap.size());
        assertEquals("admin的值应匹配", testUser, deserializedMap.get("admin"));
    }

    @Test
    public void testNestedGenericType() {
        List<Map<String, User>> nestedList = new ArrayList<Map<String, User>>();
        nestedList.add(testUserMap);

        String json = adapter.toJson(nestedList);
        assertNotNull("嵌套泛型序列化结果不应为null", json);
        assertTrue("应包含嵌套的admin键", json.contains("\"admin\""));

        // 反序列化
        JsonTypeReference<List<Map<String, User>>> nestedType =
                new JsonTypeReference<List<Map<String, User>>>() {
                };
        List<Map<String, User>> deserializedNested = adapter.fromJson(json, nestedType);

        assertNotNull("反序列化嵌套泛型不应为null", deserializedNested);
        assertEquals("嵌套List大小应正确", 1, deserializedNested.size());

        Map<String, User> nestedMap = deserializedNested.get(0);
        assertNotNull("嵌套Map不应为null", nestedMap);
        assertEquals("嵌套Map中的值应正确", testUser, nestedMap.get("admin"));
    }

    @Test(expected = Exception.class)
    public void testInvalidJsonDeserialization() {
        adapter.fromJson(invalidJson, User.class);
    }
}
