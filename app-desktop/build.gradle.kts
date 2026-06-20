plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

val defaultVersion = "0.1.0"
val appVersion = project.findProperty("appVersion")?.toString() ?: defaultVersion

val generateVersionResource by tasks.registering {
    description = "Generate version.txt resource file"
    val outputDir = layout.buildDirectory.dir("generated/resources/version")
    inputs.property("appVersion", appVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("version.txt").asFile
        file.parentFile.mkdirs()
        file.writeText(appVersion)
    }
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn(generateVersionResource)
}

kotlin {
    jvm("desktop")

    sourceSets {
        @Suppress("unused")
        val desktopMain by getting {
            resources.srcDir(generateVersionResource.map { it.outputs.files.singleFile })
            dependencies {
                implementation(project(":core"))
                implementation(compose.desktop.currentOs)
                implementation(libs.compose.desktop.touch)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.material3)
                implementation(libs.compose.runtime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.compose.ui)
                implementation(libs.slf4j.simple)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "com.whataicando.codeoba.desktop.MainKt"
            nativeDistributions {
                targetFormats(
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
                )
                packageName = "Codeoba"
                val cleanPackageVersion = appVersion.trim().substringBefore('-').substringBefore('+').lowercase().removePrefix("v").ifBlank { defaultVersion }
                val versionParts = cleanPackageVersion.split('.')
                val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
                packageVersion = if (major == 0) {
                    val rest = versionParts.drop(1).joinToString(".")
                    if (rest.isEmpty()) "1.0.0" else "1.$rest"
                } else {
                    cleanPackageVersion
                }
                vendor = "LookAtWhatAiCanDo"
                includeAllModules = true
                macOS {
                    iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
                    bundleID = "com.whataicando.codeoba"
                    val signingIdentity = System.getenv("MACOS_SIGNING_IDENTITY")
                    if (!signingIdentity.isNullOrBlank()) {
                        signing {
                            sign.set(true)
                            identity.set(signingIdentity)
                            entitlementsFile.set(project.file("src/desktopMain/resources/entitlements.plist"))
                        }
                        notarization {
                            appleID.set(System.getenv("APPLE_ID"))
                            password.set(System.getenv("APPLE_ID_PASSWORD"))
                            teamID.set(System.getenv("APPLE_TEAM_ID"))
                        }
                    }
                }
                windows {
                    iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                    shortcut = true
                    menu = true
                    menuGroup = "LookAtWhatAiCanDo"
                    installationPath = "LookAtWhatAiCanDo/Codeoba"
                }
                linux {
                    iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                }
            }
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    // Forward only Codeoba-specific system properties (e.g. -Dcodeoba.base_url=...)
    val forwarded = System.getProperties().entries
        .mapNotNull { (k, v) ->
            val key = k.toString()
            if (key.startsWith("codeoba.")) key to v else null
        }
        .toMap()

    systemProperties(forwarded)
}
