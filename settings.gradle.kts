pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
rootProject.name = "ocn-node"
// used in local dev only and overriden in CI
gradle.beforeProject {
    project.extensions.extraProperties["profile"] = "dev"
}