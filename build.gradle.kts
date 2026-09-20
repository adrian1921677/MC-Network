plugins {
    java
    // Zugriff auf Paper-Interna (für Fake-Entities wie Capes, die nur per Paket existieren)
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
    // Startet mit "gradlew runServer" einen fertigen Test-Server inklusive Plugin
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "dev.nexus"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // Paper 26.3 inklusive Server-Interna
    paperweight.paperDevBundle("26.3.build.19-alpha")

    // Verbindungs-Pool für den MySQL-Speicher. Wird nicht mitgeliefert: Paper lädt ihn zur
    // Laufzeit selbst nach (siehe "libraries" in der plugin.yml).
    compileOnly("com.zaxxer:HikariCP:5.1.0")
}

// Ab Minecraft 26.1 ist der Server nicht mehr verschleiert, das Plugin wird direkt so ausgeliefert
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        // Setzt die Versionsnummer automatisch in die plugin.yml ein
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    runServer {
        minecraftVersion("26.3")
    }
}
