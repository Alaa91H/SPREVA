plugins {
    alias(libs.plugins.spreva.android.feature)
}

android {
    namespace = "com.spreva.feature.settings.impl"
}

dependencies {
    implementation(project(":feature:settings:api"))
    implementation(project(":core:datastore"))
    implementation(project(":domain:curriculum"))
    implementation("androidx.appcompat:appcompat:1.7.1")
}
