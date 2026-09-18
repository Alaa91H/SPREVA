plugins {
    alias(libs.plugins.spreva.android.library)
    alias(libs.plugins.spreva.android.hilt)
}

android {
    namespace = "com.spreva.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(project(":core:content"))
    api(project(":core:memory"))
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.junit4)
}
