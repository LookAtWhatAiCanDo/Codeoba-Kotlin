# Suppress overall warning failures
-ignorewarnings
-dontoptimize

# --- ONNX Runtime ---
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# --- SQLite JDBC ---
-keep class org.sqlite.** { *; }
-dontwarn org.sqlite.**

# --- JNA ---
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# --- Kotlin Coroutines & Serialization ---
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# --- Ktor Server & Netty ---
-keep class io.netty.** { *; }
-keepnames class io.netty.** { *; }
-dontwarn io.netty.**

-keep class io.ktor.server.netty.Netty { *; }
-keep class io.ktor.server.config.** { *; }
-dontwarn io.ktor.**
-dontwarn com.typesafe.**

# Netty's optional/runtime dependencies that are missing from classpath:
-dontwarn org.jboss.marshalling.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn reactor.blockhound.**
-dontwarn org.apache.logging.log4j.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.jcraft.jzlib.**
-dontwarn net.jpountz.lz4.**
-dontwarn net.jpountz.xxhash.**
-dontwarn com.ning.compress.**
-dontwarn lzma.sdk.**
-dontwarn com.github.luben.zstd.**
