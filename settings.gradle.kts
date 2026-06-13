pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "leafmc-server-dev"

include("LeafGomoku")
include("LeafFriends")
include("LeafResidenceWeb")
include("LeafRecycle")
include("LeafSoulbind")
include("LeafChainHarvest")
include("LeafMenuTool")

project(":LeafGomoku").projectDir = file("src/LeafGomoku")
project(":LeafFriends").projectDir = file("src/LeafFriends")
project(":LeafResidenceWeb").projectDir = file("src/LeafResidenceWeb")
project(":LeafRecycle").projectDir = file("src/LeafRecycle")
project(":LeafSoulbind").projectDir = file("src/LeafSoulbind")
project(":LeafChainHarvest").projectDir = file("src/LeafChainHarvest")
project(":LeafMenuTool").projectDir = file("src/LeafMenuTool")
