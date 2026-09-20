plugins {
    java
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
    compileOnly("io.papermc.paper:paper-api:26.3.build.19-alpha")
    // Die Fuehrung legt dem Besucher echte Cosmetics an. NexusCosmetics liegt auf dem
    // Showcase-Server daneben; hier wird nur dagegen kompiliert, nicht mitgeliefert.
    compileOnly(files("../build/libs/NexusCosmetics-0.1.0.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
