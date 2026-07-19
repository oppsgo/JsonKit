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
}
