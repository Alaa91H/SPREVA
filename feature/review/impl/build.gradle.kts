plugins {
    alias(libs.plugins.spreva.android.feature)
}

android {
    namespace = "com.spreva.feature.review.impl"
}

dependencies {
    implementation(project(":feature:review:api"))
    implementation(project(":core:common"))
    implementation(project(":domain:review"))
    implementation(project(":domain:learning"))
}
