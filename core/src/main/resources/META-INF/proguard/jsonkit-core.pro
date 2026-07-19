# JsonKit (:core) — minimal R8 / ProGuard consumer rules.
# Bundled at META-INF/proguard/ so R8 merges them automatically from the JAR.
# Engine keep rules (Gson / Moshi / Fastjson / Okio) ship with those libraries.
# App model / strategy classes remain the app's responsibility (same as Gson).

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature
-keep @interface io.github.oppsgo.json.annotation.**
