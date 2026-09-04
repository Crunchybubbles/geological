pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
            content {
                includeGroupByRegex("net\\.neoforged.*")
            }
        }
    }
}

plugins {
    id("net.neoforged.moddev.repositories") version "2.0.146"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
            content {
                includeGroupByRegex("net\\.neoforged.*")
            }
        }
    }
}

rootProject.name = "geological"

include("geology-core", "atlas-cli", "neoforge-adapter")
