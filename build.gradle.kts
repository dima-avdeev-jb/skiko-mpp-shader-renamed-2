plugins {
    kotlin("multiplatform") version "1.7.20"
//    id("org.jetbrains.gradle.apple.applePlugin") version "222.849-0.15.1"
    id("org.jetbrains.gradle.apple.applePlugin") version "222.3345.143-0.16"
}
//222.3345.143-0.16

val RUN_ON_DEVICE = false

val coroutinesVersion = "1.5.2"

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val osName = System.getProperty("os.name")
val hostOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
var hostArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val host = "${hostOs}-${hostArch}"

var version = "0.0.0-SNAPSHOT"
if (project.hasProperty("skiko.version")) {
    version = project.properties["skiko.version"] as String
}

val resourcesDir = "$buildDir/resources"
val skikoWasm by configurations.creating

dependencies {
    skikoWasm("org.jetbrains.skiko:skiko-js-wasm-runtime:$version")
}

val unzipTask = tasks.register("unzipWasm", Copy::class) {
    destinationDir = file(resourcesDir)
    from(skikoWasm.map { zipTree(it) })
}

kotlin {
    if (System.getProperty("os.arch") == "aarch64") {
        iosSimulatorArm64("uikitSimArm64") {
            binaries {
                framework {
                    baseName = "shared"
                    freeCompilerArgs += listOf(
                        "-linker-option", "-framework", "-linker-option", "Metal",
                        "-linker-option", "-framework", "-linker-option", "CoreText",
                        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                    )
                }
            }
        }
    } else {
        ios("uikitX64") {
            binaries {
                framework {
                    baseName = "shared"
                    freeCompilerArgs += listOf(
                        "-linker-option", "-framework", "-linker-option", "Metal",
                        "-linker-option", "-framework", "-linker-option", "CoreText",
                        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                    )
                }
            }
        }
    }
    if (RUN_ON_DEVICE) {
        iosArm64("uikitArm64") {
            binaries {
                framework {
                    baseName = "shared"
                    freeCompilerArgs += listOf(
                        "-linker-option", "-framework", "-linker-option", "Metal",
                        "-linker-option", "-framework", "-linker-option", "CoreText",
                        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                    )
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.skiko:skiko:$version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }
        val uikitMain by creating {  }
        if (System.getProperty("os.arch") == "aarch64") {
            val uikitSimArm64Main by getting { dependsOn(uikitMain) }
        } else {
            val uikitX64Main by getting { dependsOn(uikitMain) }
        }
        if (RUN_ON_DEVICE) {
            val uikitArm64Main by getting { dependsOn(uikitMain) }
        }
    }
}

apple {
    iosApp {
        productName = "SkikoShader"
        sceneDelegateClass = "SceneDelegate"
        launchStoryboard = "LaunchScreen"
        dependencies {
            implementation(project(":"))
        }
    }
}
