plugins {
    alias(libs.plugins.spreva.android.feature)
}

android {
    namespace = "com.spreva.feature.lesson.impl"
}

dependencies {
    implementation(project(":feature:lesson:api"))
    implementation(project(":core:common"))
    implementation(project(":domain:curriculum"))
    implementation(project(":domain:learning"))
}
