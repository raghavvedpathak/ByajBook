pluginManagement {
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ByajBook"
include(":app")

// Core Modules
include(":core:domain")
include(":core:navigation")
include(":core:ui")
include(":core:data")
include(":core:calculations")
include(":core:pdf")

// Feature Modules
include(":feature:dashboard")
include(":feature:entry")
include(":feature:customers")
include(":feature:payments")
include(":feature:reports")
include(":feature:settings")
