plugins {
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.compose)
}

android {
    namespace = "com.spreva.feature.course.api"
}

dependencies {
    api(project(":core:navigation"))
}
