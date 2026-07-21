# Keep Kotlin Metadata so Instantiator can detect Kotlin classes.
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# Keep data class / primary constructors used by Instantiator.
-keepclassmembers class * {
    public <init>(...);
}
