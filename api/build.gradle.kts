plugins {
    `maven-publish`
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt-jvm:3.0.1")
    api("com.typesafe:config:1.4.0")
    api("br.com.devsrsouza.eventkt:eventkt-core-jvm:0.1.0-SNAPSHOT")
    api("org.slf4j:slf4j-api:1.7.30")
}

publishing {
    publications {
        val sources by tasks.registering(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }

        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifact(sources.get())

            pom {
                scm {
                    url.set("https://github.com/KatanPanel/Katan/tree/master/api")
                }
            }
        }
    }
}