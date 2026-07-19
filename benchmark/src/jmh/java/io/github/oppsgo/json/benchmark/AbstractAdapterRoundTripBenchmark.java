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

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.support.BindingCache;

/**
 * Shared round-trip shape for adapter benchmarks. Subclasses supply the adapter under test.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public abstract class AbstractAdapterRoundTripBenchmark {

    public enum CacheMode {
        UNCACHED,
        CACHED
    }

    @Param({"UNCACHED", "CACHED"})
    public CacheMode mode;

    private JsonAdapter adapter;
    private BenchmarkModels.AnnotatedEmployee employee;
    private String json;

    protected abstract JsonAdapter createAdapter(BindingCache bindings);

    @Setup(Level.Trial)
    public void setUp() {
        adapter = createAdapter(new BindingCache(mode == CacheMode.CACHED));
        employee = BenchmarkModels.sampleEmployee();
        json = adapter.toJson(employee);
        for (int i = 0; i < 1_000; i++) {
            adapter.fromJson(adapter.toJson(employee), BenchmarkModels.AnnotatedEmployee.class);
        }
    }

    @Benchmark
    public BenchmarkModels.AnnotatedEmployee roundTrip() {
        return adapter.fromJson(adapter.toJson(employee), BenchmarkModels.AnnotatedEmployee.class);
    }

    @Benchmark
    public String serialize() {
        return adapter.toJson(employee);
    }

    @Benchmark
    public BenchmarkModels.AnnotatedEmployee deserialize() {
        return adapter.fromJson(json, BenchmarkModels.AnnotatedEmployee.class);
    }
}
