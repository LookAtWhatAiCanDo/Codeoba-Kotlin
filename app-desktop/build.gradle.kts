plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm("desktop") {
        withJava()
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":core"))
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
                packageVersion = project.findProperty("appVersion")?.toString() ?: "1.0.0"
                vendor = "LookAtWhatAiCanDo"
                macOS {
                    iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
                }
                windows {
                    iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                }
                linux {
                    iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                }
            }
        }
    }
}
