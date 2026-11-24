plugins { `java-library` }
repositories { mavenCentral() }
dependencies { implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3") }
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
