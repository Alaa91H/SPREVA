plugins {
    `kotlin-dsl`
}

group = "com.spreva.buildlogic"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)

    // Precompiled script plugins resolve plugin ids from their `plugins {}`
    // blocks at accessor-generation time, so these markers and plugin jars
    // must be real (implementation) dependencies of build-logic.
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
    implementation(libs.marker.android.application)
    implementation(libs.marker.android.library)
    implementation(libs.marker.compose.compiler)
    implementation(libs.marker.kotlin.serialization)
    implementation(libs.marker.hilt)
}

gradlePlugin {
    plugins {
        // NOTE: spreva.android.application and spreva.android.feature are
        // precompiled script plugins (spreva.android.*.grad.kts) — ids are
        // registered automatically from their file names.
        register("androidLibrary") {
            id = "spreva.android.library"
            implementationClass = "com.spreva.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "spreva.android.compose"
            implementationClass = "com.spreva.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidRoom") {
            id = "spreva.android.room"
            implementationClass = "com.spreva.buildlogic.AndroidRoomConventionPlugin"
        }
        register("androidHilt") {
            id = "spreva.android.hilt"
            implementationClass = "com.spreva.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidTesting") {
            id = "spreva.android.testing"
            implementationClass = "com.spreva.buildlogic.AndroidTestingConventionPlugin"
        }
        register("jvmLibrary") {
            id = "spreva.jvm.library"
            implementationClass = "com.spreva.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
