plugins {
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.compose)
}

android {
    namespace = "com.spreva.feature.lesson.api"
}

dependencies {
    api(project(":core:navigation"))
}
