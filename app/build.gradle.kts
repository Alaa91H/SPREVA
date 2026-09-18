plugins {
    // Includes library config + compose + hilt + shared core deps.
    alias(libs.plugins.spreva.android.application)
}

android {
    namespace = "com.spreva.app"

    defaultConfig {
        applicationId = "com.spreva.app"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:content"))
    implementation(project(":core:audio"))
    implementation(project(":core:database"))
    implementation(libs.androidx.room3.runtime)
    implementation(project(":core:datastore"))
    implementation(project(":core:memory"))
    implementation(project(":core:featureflags"))
    implementation(project(":core:logging"))

    implementation(project(":domain:curriculum"))
    implementation(project(":domain:learning"))
    implementation(project(":domain:review"))

    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:course:api"))
    implementation(project(":feature:lesson:api"))
    implementation(project(":feature:review:api"))
    implementation(project(":feature:settings:api"))

    implementation(project(":feature:onboarding:impl"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:course:impl"))
    implementation(project(":feature:lesson:impl"))
    implementation(project(":feature:review:impl"))
    implementation(project(":feature:settings:impl"))

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3.wsize)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.hilt.navigation.compose)
}
