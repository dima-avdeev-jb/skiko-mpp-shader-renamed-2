pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    
}
rootProject.name = "SkiaMultiplatformShader"

//if (System.getenv("SKIKO_COMPOSITE_BUILD") == "1") {
//    includeBuild("../../skiko") {
//        dependencySubstitution {
//            substitute(module("org.jetbrains.skiko:skiko")).using(project(":"))
//        }
//    }
//}
