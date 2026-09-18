plugins {
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.hilt)
}

android {
    namespace = "com.spreva.core.datastore"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
