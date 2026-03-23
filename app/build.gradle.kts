import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "io.gatil"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3")
        bundledPlugin("com.intellij.java")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    pluginConfiguration {
        id = "io.gatil.codeexporter"
        name = "Code Exporter for AI"
        version = project.version.toString()

        // Настройки совместимости
        ideaVersion {
            sinceBuild = "243"
            // Убираем ограничение до конкретной версии для лучшей совместимости
            untilBuild = null
        }

        vendor {
            name = "Gatil"
        }
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("243")
        // Убираем ограничение до конкретной версии
        untilBuild.set(provider { null })
    }
}