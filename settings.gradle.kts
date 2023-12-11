rootProject.name = "kotlin-signalhandler-demo"

pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("multiplatform") version kotlinVersion
    }
}


