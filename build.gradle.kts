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
    implementation("org.projectlombok:lombok:1.18.42")
    providedCompile("jakarta.platform:jakarta.jakartaee-api:10.0.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

tasks.war {
    archiveFileName.set("person-service.war")
    val helios = providers.gradleProperty("deploymentProfile").orNull == "helios"
    from("client") {
        if (helios) exclude("config.js")
    }
    if (helios) {
        destinationDirectory.set(layout.buildDirectory.dir("helios"))
        from("deployment/helios/client")
        filesMatching("WEB-INF/classes/META-INF/persistence.xml") {
            filter { line -> line.replace("name=\"hibernate.hbm2ddl.auto\" value=\"update\"", "name=\"hibernate.hbm2ddl.auto\" value=\"none\"") }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
