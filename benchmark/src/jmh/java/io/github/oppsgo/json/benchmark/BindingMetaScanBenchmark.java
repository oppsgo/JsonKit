package io.github.oppsgo.json.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import io.github.oppsgo.json.annotation.JsonAlias;
import io.github.oppsgo.json.annotation.JsonIgnore;
import io.github.oppsgo.json.annotation.JsonIgnoreProperties;
import io.github.oppsgo.json.annotation.JsonProperty;
import io.github.oppsgo.json.support.BindingCache;
import io.github.oppsgo.json.support.BindingMeta;

/**
 * Isolates annotation binding cost: full {@link BindingMeta#scan} vs cached {@link BindingCache#get}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class BindingMetaScanBenchmark {

    public enum CacheMode {
        UNCACHED,
        CACHED
    }

    @Param({"UNCACHED", "CACHED"})
    public CacheMode mode;

    private Class<?> type;
    private BindingCache cache;

    @Setup(Level.Trial)
    public void setUp() {
        type = AnnotatedEmployee.class;
        cache = new BindingCache(mode == CacheMode.CACHED);
        // Warm the cache once so CACHED measures hit cost, not first-scan.
        if (mode == CacheMode.CACHED) {
            cache.get(type);
        }
    }

    @Benchmark
    public BindingMeta resolve() {
        return cache.get(type);
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
}
