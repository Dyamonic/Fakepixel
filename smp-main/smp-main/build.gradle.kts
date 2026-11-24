plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0"
    alias(libs.plugins.run.paper)
    alias(libs.plugins.paperweight.userdev)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
    paperweight.paperDevBundle(libs.versions.paper.api.get())
    implementation("com.zaxxer:HikariCP:6.3.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "club.abelix.games.fakevival.libs.hikari")
}

tasks.jar {
    archiveClassifier.set("thin")
}

tasks.reobfJar {
    inputJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}
