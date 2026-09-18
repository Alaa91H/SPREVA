plugins {
    alias(libs.plugins.spreva.android.application)
}

android {
    namespace = "com.spreva.app.catalog"

    defaultConfig {
        applicationId = "com.spreva.app.catalog"
        versionName = "0.1.0-catalog"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.androidx.lifecycle)
}
