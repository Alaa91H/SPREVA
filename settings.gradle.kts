pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "spreva"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":app-catalog")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:navigation")
include(":core:database")
include(":core:datastore")
include(":core:content")
include(":core:memory")
include(":core:featureflags")
include(":core:logging")
include(":core:testing")
include(":domain:curriculum")
include(":domain:learning")
include(":domain:review")
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:home:api")
include(":feature:home:impl")
include(":feature:course:api")
include(":feature:course:impl")
include(":feature:lesson:api")
include(":feature:lesson:impl")
include(":feature:review:api")
include(":feature:review:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
