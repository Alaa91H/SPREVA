plugins {
    // API modules expose @Composable function-type contracts; they must be
    // Android libraries with the Compose compiler so the @Composable
    // signature (with Composer params) is emitted consistently for impl
    // modules. A pure-JVM api module compiles but crashes at runtime with
    // NoSuchMethodError (documented in ADR-0003).
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.compose)
}

android {
    namespace = "com.spreva.feature.onboarding.api"
}

dependencies {
    api(project(":core:navigation"))
}
