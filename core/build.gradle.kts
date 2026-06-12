import java.util.Properties

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
                implementation(libs.ktor.client.mock)
            }
        }

        @Suppress("unused")
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.java.keyring)
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
            }
        }

        @Suppress("unused")
        val desktopTest by getting
    }
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val localPropsFile = project.rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        inputs.file(localPropsFile).optional()
    }
    
    val outputDir = file("${layout.buildDirectory.get().asFile}/generated-sources/buildconfig")
    outputs.dir(outputDir)
    doLast {
        val enableSub = if (localPropsFile.exists()) {
            val props = Properties()
            localPropsFile.inputStream().use { props.load(it) }
            props.getProperty("codeoba.enable_subscription")?.toBoolean() ?: false
        } else {
            false
        }

        
        val buildConfigFile = file("$outputDir/com/whataicando/codeoba/core/util/BuildConfig.kt")
        buildConfigFile.parentFile.mkdirs()
        buildConfigFile.writeText("""
            package com.whataicando.codeoba.core.util
            
            object BuildConfig {
                const val ENABLE_SUBSCRIPTION = $enableSub
            }
        """.trimIndent())
    }
}

kotlin.sourceSets.commonMain.configure {
    kotlin.srcDirs(generateBuildConfig)
}


