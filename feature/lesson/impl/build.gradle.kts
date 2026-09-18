plugins {
    alias(libs.plugins.spreva.android.feature)
}

android {
    namespace = "com.spreva.feature.lesson.impl"
}

dependencies {
    implementation(project(":feature:lesson:api"))
    implementation(project(":core:common"))
    implementation(project(":core:audio"))
    implementation(project(":domain:curriculum"))
    implementation(project(":domain:learning"))
    // Speaker/volume icons live in the extended set (R8 strips unused ones).
    implementation(libs.androidx.compose.material.icons.extended)
}
