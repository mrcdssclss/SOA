plugins {
    id("java")
    id("war")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    providedCompile("jakarta.platform:jakarta.jakartaee-api:10.0.0")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

tasks.war {
    archiveFileName.set("demography-service.war")
    if (providers.gradleProperty("deploymentProfile").orNull == "helios") {
        destinationDirectory.set(layout.buildDirectory.dir("helios"))
    }
}
