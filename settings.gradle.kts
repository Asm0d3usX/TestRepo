pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.library") {
                useVersion("8.2.0")
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion("2.1.0")
            }
            if (requested.id.id == "com.lagradost.cloudstream3.gradle") {
                useModule("com.github.recloudstream:gradle:-SNAPSHOT")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "CloudstreamPlugins"

// This file sets what projects are included.
// All new projects should get automatically included unless specified in the "disabled" variable.

val disabled = listOf<String>()

File(rootDir, ".").eachDir { dir ->
    if (!disabled.contains(dir.name) && File(dir, "build.gradle.kts").exists()) {
        include(":${dir.name}")
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}

// To only include a single project, comment out the previous lines (except the first one), and include your plugin like so:
// include("PluginName")