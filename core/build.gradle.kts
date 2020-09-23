dependencies {
    val exposedVersion = "0.27.1"
    val log4jVersion = "2.13.3"
    val jacksonVersion = "2.10.3"

    api(project(":common"))
    api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    api("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.auth0:java-jwt:3.10.3")
    implementation("org.jetbrains.kotlinx:atomicfu:0.14.4")
    implementation("mysql:mysql-connector-java:8.0.21")
    implementation("com.github.docker-java:docker-java:3.2.5")
}