// Precompiled convention plugin for Spreva feature implementation modules:
// library + compose + hilt + shared core wiring (plan sections 8/9).

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val libsCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

android {
    compileSdk = 37

    defaultConfig {
        minSdk = 26
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

    implementation(libsCatalog.findBundle("androidx-lifecycle").get())
    implementation(libsCatalog.findLibrary("androidx-hilt-navigation-compose").get())
    implementation(libsCatalog.findLibrary("androidx-core-ktx").get())

    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:model"))
    implementation(project(":core:logging"))

    testImplementation(libsCatalog.findLibrary("junit4").get())
    testImplementation(libsCatalog.findLibrary("kotlinx-coroutines-test").get())
}
