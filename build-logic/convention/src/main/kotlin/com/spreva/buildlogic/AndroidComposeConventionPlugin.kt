package com.spreva.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose enablement for Android modules.
 *
 * Applies the Compose compiler plugin (Kotlin 2.x style) and adds the
 * Compose BOM-aligned ui/foundation/material3 dependencies.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        dependencies {
            add("implementation", platform(libsCatalog().findLibrary("androidx-compose-bom").get()))
            add("implementation", libsCatalog().findBundle("compose").get())
            add("debugImplementation", libsCatalog().findBundle("compose-debug").get())
        }
    }
}
