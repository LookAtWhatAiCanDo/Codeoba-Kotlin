plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val defaultVersion = "1.0.0"
val appVersion = project.findProperty("appVersion")?.toString() ?: defaultVersion

val generateVersionResource by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources/version")
    inputs.property("appVersion", appVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("version.txt").asFile
        file.parentFile.mkdirs()
        file.writeText(appVersion)
    }
}

tasks.withType<org.gradle.language.jvm.tasks.ProcessResources>().configureEach {
    dependsOn(generateVersionResource)
}

kotlin {
    jvm("desktop") {
        withJava()
    }

    sourceSets {
        val desktopMain by getting {
            resources.srcDir(generateVersionResource.map { it.outputs.files.singleFile })
            dependencies {
                implementation(project(":core"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation(compose.desktop.currentOs)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "llc.lookatwhataicando.codeoba.desktop.MainKt"
            nativeDistributions {
                targetFormats(
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
                )
                packageName = "Codeoba"
                packageVersion = appVersion.trim().substringBefore('-').substringBefore('+').toLowerCase().removePrefix("v")
                vendor = "LookAtWhatAiCanDo"
                includeAllModules = true
                macOS {
                    iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
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
