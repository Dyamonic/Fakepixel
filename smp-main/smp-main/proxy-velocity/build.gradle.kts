plugins { java; id("com.gradleup.shadow") version "9.0.0" }
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    implementation(project(":proxy-common"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
tasks.shadowJar { archiveClassifier.set("") }
tasks.build { dependsOn(tasks.shadowJar) }
