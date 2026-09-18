plugins {
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.hilt)
}

android {
    namespace = "com.spreva.core.audio"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.media3.common)
    // ExoPlayer/Session land here in Phase 4.2 (bundled course audio);
    // included now so the Media3 foundation is versioned and cached.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
