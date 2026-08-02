plugins {
    `java-library`
}

group = "me.mina.manhunt"
version = "1.0.1"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

val paperVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("me.clip:placeholderapi:2.11.6")
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("me.clip:placeholderapi:2.11.6")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    expand("version" to project.version)
}

tasks.jar {
    archiveBaseName.set("ManHunt")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
