package com.spreva.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Testing convention: JUnit4, coroutines-test, Turbine for unit tests and
 * AndroidX test dependencies for instrumentation.
 */
class AndroidTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("spreva.android.library")

        dependencies {
            add("testImplementation", libsCatalog().findLibrary("junit4").get())
            add("testImplementation", libsCatalog().findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libsCatalog().findLibrary("turbine").get())
            add("androidTestImplementation", libsCatalog().findLibrary("androidx-test-junit").get())
            add("androidTestImplementation", libsCatalog().findLibrary("androidx-test-runner").get())
            add("androidTestImplementation", libsCatalog().findLibrary("androidx-espresso-core").get())
        }
    }
}
