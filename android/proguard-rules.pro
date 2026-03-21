# AgentOS ProGuard Rules

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations

# Keep data classes
-keepclassmembers class * {
    public <init>(...);
}
