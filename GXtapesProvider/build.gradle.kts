plugins {
    id("com.android.library")
    kotlin("android")
    id("com.lagradost.cloudstream3.gradle")
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(kotlin("stdlib"))

}

version = 1

cloudstream {

    description = "GXtapes provider with full pagination (Latest, Categories, Channels)"
    authors = listOf("Don")
    status = 1
    tvTypes = listOf("NSFW")
    language = "en"
    iconUrl = "https://gay.xtapes.in/wp-content/uploads/logo6.png"
    requiresResources = true
}

android {
    compileSdk = 35
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}