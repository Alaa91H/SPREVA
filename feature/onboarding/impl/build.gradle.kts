plugins {
    alias(libs.plugins.spreva.android.feature)
}

android {
    namespace = "com.spreva.feature.onboarding.impl"
}

dependencies {
    implementation(project(":feature:onboarding:api"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":domain:learning"))
}
