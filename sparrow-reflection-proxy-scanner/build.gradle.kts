plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.1"
}

group = "net.momirealms"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.9.1")
}

gradlePlugin {
    plugins {
        create("scanner") {
            id = "net.momirealms.sparrow-reflection-proxy-scanner"
            implementationClass = "net.momirealms.sparrow.reflection.gradle.SparrowProxyPlugin"
            displayName = "Reflection Proxy Scanner"
            description = "Scans @ReflectionProxy annotations and generates mapping files."
        }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.objectweb.asm", "net.momirealms.sparrow.reflection.gradle.internal.asm")
}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.shadowJar)
}

publishing {
    repositories {
        maven {
            name = "XiaoMoMi"
            url = uri("https://repo.momirealms.net/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}