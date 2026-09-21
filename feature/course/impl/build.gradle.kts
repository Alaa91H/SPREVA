plugins {
    alias(libs.plugins.spreva.android.feature)
}

android {
    namespace = "com.spreva.feature.course.impl"
}

dependencies {
    implementation(project(":feature:course:api"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":domain:curriculum"))
    implementation(project(":domain:learning"))
}
