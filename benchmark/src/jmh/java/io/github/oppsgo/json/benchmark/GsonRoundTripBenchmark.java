package io.github.oppsgo.json.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.gson.GsonAdapter;

/**
 * Gson steady-state throughput (no BindingCache toggle).
 * Gson relies on its TypeAdapter cache; JsonKit does not retain BindingMeta beside it.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class GsonRoundTripBenchmark {

    private GsonAdapter adapter;
    private BenchmarkModels.AnnotatedEmployee employee;
    private String json;

    @Setup(Level.Trial)
    public void setUp() {
        adapter = new GsonAdapter(JsonOptions.defaults());
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
