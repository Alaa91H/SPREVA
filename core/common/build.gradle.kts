plugins {
    alias(libs.plugins.spreva.jvm.library)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
}
