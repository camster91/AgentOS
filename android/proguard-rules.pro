# AgentOS ProGuard Rules

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Gson (used by agents and sync for JSON serialization) ─────────────────────
-keepattributes Signature
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep all agent data classes (serialized to/from storage)
-keep class com.agentOS.agents.Note { *; }
-keep class com.agentOS.agents.Task { *; }
-keep class com.agentOS.agents.Email { *; }
-keep class com.agentOS.agents.Message { *; }
-keep class com.agentOS.agents.Transaction { *; }
-keep class com.agentOS.agents.Priority { *; }
-keep class com.agentOS.api.** { *; }
-keep class com.agentOS.core.** { *; }
-keep class com.agentOS.agents.** { *; }
-keep class com.agentOS.ai.** { *; }
-keep class com.agentOS.sync.** { *; }

# ── OkHttp (used by GeminiClient and SyncClient) ──────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# ── Compose ───────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class * extends androidx.compose.runtime.Composable { *; }

# ── AndroidX Lifecycle / ViewModel ───────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ── General: keep all public constructors for reflection ─────────────────────
-keepclassmembers class * {
    public <init>(...);
}

# ── Logback (JVM logging, excluded on Android but referenced) ─────────────────
-dontwarn ch.qos.logback.**
-dontwarn org.slf4j.**
