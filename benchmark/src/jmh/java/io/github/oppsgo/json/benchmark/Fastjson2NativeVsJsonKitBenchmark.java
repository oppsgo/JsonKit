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

import com.alibaba.fastjson2.JSON;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.fastjson2.Fastjson2Adapter;

/**
 * Bare Fastjson2 vs JsonKit {@link Fastjson2Adapter} on the same payloads.
 * <p>
 * {@code PLAIN}: Java field names == JSON keys (JsonKit path D / native {@code parseObject}).
 * {@code ANNOTATED}: rename + alias + ignore (JsonKit C′ vs Fastjson2 {@code @JSONField} twin).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class Fastjson2NativeVsJsonKitBenchmark {

    public enum Stack {
        NATIVE,
        JSONKIT
    }

    public enum ModelKind {
        PLAIN,
        ANNOTATED
    }

    @Param({"NATIVE", "JSONKIT"})
    public Stack stack;

    @Param({"PLAIN", "ANNOTATED"})
    public ModelKind modelKind;

    private JsonAdapter jsonKit;
    private Object bean;
    private Class<?> beanType;
    private String json;

    @Setup(Level.Trial)
    public void setUp() {
        jsonKit = new Fastjson2Adapter(JsonOptions.defaults());
        if (modelKind == ModelKind.PLAIN) {
            bean = BenchmarkModels.samplePlainEmployee();
            beanType = BenchmarkModels.PlainEmployee.class;
            json = stack == Stack.NATIVE
                    ? JSON.toJSONString(bean)
                    : jsonKit.toJson(bean);
        } else {
            if (stack == Stack.NATIVE) {
                bean = BenchmarkModels.sampleNativeAnnotatedEmployee();
                beanType = BenchmarkModels.Fastjson2NativeAnnotatedEmployee.class;
                json = JSON.toJSONString(bean);
            } else {
                bean = BenchmarkModels.sampleEmployee();
                beanType = BenchmarkModels.AnnotatedEmployee.class;
                json = jsonKit.toJson(bean);
            }
        }
        for (int i = 0; i < 1_000; i++) {
            deserialize();
            serialize();
        }
    }

    @Benchmark
    public String serialize() {
        if (stack == Stack.NATIVE) {
            return JSON.toJSONString(bean);
        }
        return jsonKit.toJson(bean);
    }

    @Benchmark
    public Object deserialize() {
        if (stack == Stack.NATIVE) {
            return JSON.parseObject(json, beanType);
        }
        return jsonKit.fromJson(json, beanType);
    }

    @Benchmark
    public Object roundTrip() {
        if (stack == Stack.NATIVE) {
            return JSON.parseObject(JSON.toJSONString(bean), beanType);
        }
        return jsonKit.fromJson(jsonKit.toJson(bean), beanType);
    }
}
