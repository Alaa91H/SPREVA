plugins {
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.compose)
}

android {
    namespace = "com.spreva.core.designsystem"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.androidx.lifecycle)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit4)
}
