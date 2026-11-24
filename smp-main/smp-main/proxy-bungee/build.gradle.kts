plugins { java; id("com.gradleup.shadow") version "9.0.0" }
repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
}
dependencies { implementation(project(":proxy-common")); compileOnly("net.md-5:bungeecord-api:1.21-R0.4") }
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
tasks.shadowJar { archiveClassifier.set("") }
tasks.build { dependsOn(tasks.shadowJar) }
