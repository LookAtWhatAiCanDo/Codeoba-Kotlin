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
        val props = Properties()
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { props.load(it) }
        }
        val enableSub = System.getenv("CODEOBA_ENABLE_SUBSCRIPTION")?.toBoolean()
            ?: project.findProperty("codeoba.enable_subscription")?.toString()?.toBoolean()
            ?: props.getProperty("codeoba.enable_subscription")?.toBoolean()
            ?: false
        
        val premiumPublicKey = System.getenv("CODEOBA_PREMIUM_PUBLIC_KEY")
            ?: project.findProperty("codeoba.premium.public_key")?.toString()
            ?: props.getProperty("codeoba.premium.public_key")
            ?: ""

        if (enableSub && premiumPublicKey.isBlank()) {
            throw GradleException(
                "Error: Subscription features are enabled (codeoba.enable_subscription=true), but the premium public verification key is missing.\n" +
                "Please configure 'codeoba.premium.public_key' in local.properties, or run developer key generation tasks to set up verification keys."
            )
        }
        
        val defaultFirebaseApiKey = "EMULATOR_ONLY"
        val firebaseApiKey = System.getenv("CODEOBA_FIREBASE_API_KEY")
            ?: project.findProperty("codeoba.firebase.api_key")?.toString()
            ?: props.getProperty("codeoba.firebase.api_key")
            ?: defaultFirebaseApiKey

        val defaultAppSignature = "DEVELOPMENT_ONLY"
        val appSignature = System.getenv("CODEOBA_APP_SIGNATURE_HASH")
            ?: project.findProperty("codeoba.app_signature_hash")?.toString()
            ?: props.getProperty("codeoba.app_signature_hash")
            ?: defaultAppSignature

        val isDebug = System.getenv("CODEOBA_DEBUG")?.toBoolean()
            ?: project.findProperty("codeoba.debug")?.toString()?.toBoolean()
            ?: props.getProperty("codeoba.debug")?.toBoolean()
            ?: !project.gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

        val buildConfigFile = file("$outputDir/com/whataicando/codeoba/core/util/BuildConfig.kt")
        buildConfigFile.parentFile.mkdirs()
        buildConfigFile.writeText("""
            package com.whataicando.codeoba.core.util
            
            object BuildConfig {
                const val ENABLE_SUBSCRIPTION = $enableSub
                const val PREMIUM_PUBLIC_KEY = "$premiumPublicKey"
                const val FIREBASE_API_KEY = "$firebaseApiKey"
                const val APP_SIGNATURE = "$appSignature"
                const val DEBUG = $isDebug
            }
        """.trimIndent())
    }
}

kotlin.sourceSets.commonMain.configure {
    kotlin.srcDirs(generateBuildConfig)
}


