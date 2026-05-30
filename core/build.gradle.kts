plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    // Desktop (JVM) target
    jvm("desktop")

    sourceSets {
        @Suppress("unused")
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.components.ui.tooling.preview)

                // Coroutines & Serialization
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                // Ktor HTTP/WebSocket Client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
            }
        }

        @Suppress("unused")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        @Suppress("unused")
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)

                // Ktor Server (for local Hub API)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cors)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.server.netty)

                // ONNX Runtime for JVM
                implementation(libs.onnxruntime)

                // SQLite JDBC — pure-Java, no subprocess needed
                implementation(libs.sqlite.jdbc)
                // Ktor Server (for local Hub API)
                implementation("io.ktor:ktor-server-core:2.3.12")
                implementation("io.ktor:ktor-server-netty:2.3.12")
                implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-server-cors:2.3.12")
                implementation("com.github.javakeyring:java-keyring:1.0.4")
            }
        }

        @Suppress("unused")
        val desktopTest by getting
    }
}
