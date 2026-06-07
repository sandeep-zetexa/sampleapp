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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("${rootDir}/app/lib/repo")
        }
        maven {
            url = uri("https://storage.googleapis.com/download.flutter.io")
        }

    }
}

rootProject.name = "SampleApp"
include(":app")
 