plugins {
    alias(libs.plugins.spreva.android.feature)
}

android {
    namespace = "com.spreva.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:api"))
    implementation(project(":core:common"))
    implementation(project(":domain:curriculum"))
    implementation(project(":domain:learning"))
    implementation(project(":domain:review"))
}
