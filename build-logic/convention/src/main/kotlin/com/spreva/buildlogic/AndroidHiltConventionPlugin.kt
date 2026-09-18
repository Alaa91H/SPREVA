package com.spreva.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt convention: hilt plugin + runtime + compiler via KSP.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")

        dependencies {
            add("implementation", libsCatalog().findLibrary("hilt-android").get())
            add("ksp", libsCatalog().findLibrary("hilt-compiler").get())
        }
    }
}
