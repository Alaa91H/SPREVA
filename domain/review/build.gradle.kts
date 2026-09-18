plugins {
    alias(libs.plugins.spreva.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:memory"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
