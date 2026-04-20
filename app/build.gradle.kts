import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.14.0"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "io.gatil"
version = "1.0.1"

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
    
    testImplementation("junit:junit:4.13.2")
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
    
    // Disable instrumentCode to avoid JDK path issues
    instrumentCode {
        enabled = false
    }
    
    instrumentTestCode {
        enabled = false
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}