package com.spreva.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared configuration values for Spreva convention plugins.
 */
internal object SprevaBuild {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val TARGET_SDK = 37
    const val JDK = 17
}

/**
 * Access the shared version catalog from convention plugin code.
 */
internal fun Project.libsCatalog(): VersionCatalog =
    extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

/**
 * Shared Android configuration for application modules.
 */
internal fun ApplicationExtension.configureSpreva(project: Project) {
    compileSdk = SprevaBuild.COMPILE_SDK

    defaultConfig {
        minSdk = SprevaBuild.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    project.extensions.configure(KotlinAndroidProjectExtension::class.java) {
        jvmToolchain(SprevaBuild.JDK)
    }
}

/**
 * Shared Android configuration for library modules.
 */
internal fun LibraryExtension.configureSpreva(project: Project) {
    compileSdk = SprevaBuild.COMPILE_SDK

    defaultConfig {
        minSdk = SprevaBuild.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    project.extensions.configure(KotlinAndroidProjectExtension::class.java) {
        jvmToolchain(SprevaBuild.JDK)
    }
}
