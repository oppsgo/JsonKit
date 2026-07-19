# JsonKit JMH benchmarks

Measures binding-cache impact across core + all adapters. Uses the JDK that runs Gradle (Java 8+).

## Run

```bash
# all benches
./gradlew :benchmark:jmh

# one adapter only
./gradlew :benchmark:jmh -PjmhInclude=GsonRoundTrip
./gradlew :benchmark:jmh -PjmhInclude=FastjsonRoundTrip
./gradlew :benchmark:jmh -PjmhInclude=Fastjson2RoundTrip
./gradlew :benchmark:jmh -PjmhInclude=BindingMetaScan
```

Results: `benchmark/build/results/jmh/results.txt`

## What to look at

| Benchmark                          | Meaning                                                                    |
|------------------------------------|----------------------------------------------------------------------------|
| `BindingMetaScanBenchmark.resolve` | Pure scan vs cache hit (engine-agnostic)                                   |
| `Fastjson2RoundTripBenchmark.*`    | Fastjson2 + `BindingCache(true/false)`                                     |
| `FastjsonRoundTripBenchmark.*`     | Fastjson 1.x + cache toggle                                                |
| `GsonRoundTripBenchmark.*`         | Gson steady-state only (no BindingCache; relies on Gson TypeAdapter cache) |

`UNCACHED` = `new BindingCache(false)`. `CACHED` = production behavior.
