plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    // Desktop (JVM) target
    jvm("desktop") {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Coroutines & Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // Ktor HTTP/WebSocket Client
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-cio:2.3.12")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

                // SQLite JDBC — pure-Java, no subprocess needed
                implementation("org.xerial:sqlite-jdbc:3.45.3.0")

                // Ktor Server (for local Hub API)
                implementation("io.ktor:ktor-server-core:2.3.12")
                implementation("io.ktor:ktor-server-netty:2.3.12")
                implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-server-cors:2.3.12")
            }
        }

        val desktopTest by getting
    }
}
