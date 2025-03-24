plugins {
    application
    id("java")
}

group = "space.itoncek.cvss"
version = "v0.0.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass.set("space.itoncek.cvss.server.CVSS_Server")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "space.itoncek.cvss.server.CVSS_Server"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val lombokVersion = "1.18.34"
val postgresqlVersion = "42.7.3"
val hibernateVersion = "7.0.0.Beta1"
val junitVersion = "5.10.3"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("io.javalin:javalin:6.5.0")
    implementation("com.github.kmehrunes:javalin-jwt:0.7.0")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("org.hibernate.orm:hibernate-core:$hibernateVersion")
    implementation("org.hibernate.orm:hibernate-processor:7.0.0.Beta4")
    annotationProcessor("org.hibernate.orm:hibernate-jpamodelgen:$hibernateVersion")
    implementation("org.json:json:20240303")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("commons-io:commons-io:2.15.1")
}

tasks.test {
    useJUnitPlatform()
}