plugins {
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.compose)
}

android {
    namespace = "com.spreva.feature.review.api"
}

dependencies {
    api(project(":core:navigation"))
}
