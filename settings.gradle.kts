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
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT) // ✅ 프로젝트 내 저장소 허용 (필요할 경우)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SmartBulk"
include(":app")
