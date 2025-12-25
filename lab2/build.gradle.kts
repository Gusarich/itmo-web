plugins {
    war
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    compileOnly("jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.1")
    implementation("org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1")
}

// Use the app name consistently for the WAR
val appName = "lab2"

tasks.named<War>("war") {
    archiveBaseName.set(appName)
}
