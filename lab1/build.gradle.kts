plugins {
    application
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories { }

// For local dev we run an embedded HTTP server (no external deps).
application { mainClass.set("itmo.web.lab1.FastCgiMain") }

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

// Wire the provided FastCGI jar (placed at project root as fastcgi-lib.jar)
dependencies {
    implementation(files("fastcgi-lib.jar"))
}

tasks.register<JavaExec>("runFastCgi") {
    group = "application"
    description = "Run FastCGI external server on port 9000 (FCGI_PORT)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("itmo.web.lab1.FastCgiMain")
    environment("FCGI_PORT", "9000")
}
