plugins {
    alias(libs.plugins.spreva.android.room)
    alias(libs.plugins.spreva.android.hilt)
}

android {
    namespace = "com.spreva.core.database"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room3.testing)
    testImplementation(libs.androidx.test.core)
}
