plugins {
    alias(libs.plugins.spreva.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core:model"))
    testImplementation(libs.junit4)
}
