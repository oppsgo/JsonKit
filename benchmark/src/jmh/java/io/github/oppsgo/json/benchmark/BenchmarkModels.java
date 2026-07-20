package io.github.oppsgo.json.benchmark;

import io.github.oppsgo.json.annotation.JsonAlias;
import io.github.oppsgo.json.annotation.JsonIgnore;
import io.github.oppsgo.json.annotation.JsonIgnoreProperties;
import io.github.oppsgo.json.annotation.JsonProperty;

/** Shared annotated model for adapter round-trip benchmarks. */
public final class BenchmarkModels {

    private BenchmarkModels() {
    }

    @JsonIgnoreProperties({"debug"})
    public static class AnnotatedPerson {
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

    public static class AnnotatedEmployee extends AnnotatedPerson {
        public String title;
        public int level;
    }

    public static AnnotatedEmployee sampleEmployee() {
        AnnotatedEmployee employee = new AnnotatedEmployee();
        employee.id = "e-1";
        employee.displayName = "Ada";
        employee.internalToken = "secret";
        employee.debug = "nope";
        employee.title = "Engineer";
        employee.level = 3;
        return employee;
    }

    /** Same shape as {@link AnnotatedEmployee} but no JsonKit annotations (path D / native). */
    public static class PlainEmployee {
        public String id;
        public String displayName;
        public String title;
        public int level;
    }

    public static PlainEmployee samplePlainEmployee() {
        PlainEmployee employee = new PlainEmployee();
        employee.id = "e-1";
        employee.displayName = "Ada";
        employee.title = "Engineer";
        employee.level = 3;
        return employee;
    }

    /**
     * Fastjson2-native twin of {@link AnnotatedEmployee} for overhead comparison
     * (same JSON names via {@code @JSONField}, not JsonKit).
     */
    public static class Fastjson2NativeAnnotatedPerson {
        @com.alibaba.fastjson2.annotation.JSONField(name = "person_id", alternateNames = {"pid"})
        public String id;

        @com.alibaba.fastjson2.annotation.JSONField(name = "display_name", alternateNames = {"name"})
        public String displayName;

        @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
        public String internalToken;

        @com.alibaba.fastjson2.annotation.JSONField(serialize = false)
        public String debug;
    }

    public static class Fastjson2NativeAnnotatedEmployee extends Fastjson2NativeAnnotatedPerson {
        public String title;
        public int level;
    }

    public static Fastjson2NativeAnnotatedEmployee sampleNativeAnnotatedEmployee() {
        Fastjson2NativeAnnotatedEmployee employee = new Fastjson2NativeAnnotatedEmployee();
        employee.id = "e-1";
        employee.displayName = "Ada";
        employee.internalToken = "secret";
        employee.debug = "nope";
        employee.title = "Engineer";
        employee.level = 3;
        return employee;
    }
}
