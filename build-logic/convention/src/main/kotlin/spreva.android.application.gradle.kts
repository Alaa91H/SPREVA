// Precompiled convention plugin for Spreva Android application modules.
// AGP 9 ships built-in Kotlin support; Compose/Serialization compiler
// plugins are added on top (plan section 9).

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val libsCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

android {
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libsCatalog.findLibrary("hilt-android").get())
    ksp(libsCatalog.findLibrary("hilt-compiler").get())

    implementation(platform(libsCatalog.findLibrary("androidx-compose-bom").get()))
    implementation(libsCatalog.findBundle("compose").get())
    debugImplementation(libsCatalog.findBundle("compose-debug").get())

    implementation(libsCatalog.findLibrary("androidx-core-ktx").get())
    implementation(libsCatalog.findLibrary("androidx-activity-compose").get())
    implementation(libsCatalog.findBundle("androidx-lifecycle").get())

    testImplementation(libsCatalog.findLibrary("junit4").get())
    testImplementation(libsCatalog.findLibrary("kotlinx-coroutines-test").get())
}
