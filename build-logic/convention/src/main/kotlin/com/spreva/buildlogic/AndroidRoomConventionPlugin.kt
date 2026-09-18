package com.spreva.buildlogic

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room convention: KSP processing, room compiler and schema export
 * into `schemas/` (committed to Git per plan sections 21/228).
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("spreva.android.library")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        dependencies {
            add("implementation", libsCatalog().findLibrary("androidx-room3-runtime").get())
            add("ksp", libsCatalog().findLibrary("androidx-room3-compiler").get())
        }
    }
}
