plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

group = "net.momirealms"
version = "0.2"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.ow2.asm:asm:9.9")
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("net.fabricmc:mapping-io:0.8.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    dependsOn(tasks.clean)
}

artifacts {
    implementation(tasks.shadowJar)
}

tasks {
    shadowJar {
        archiveClassifier = ""
        archiveFileName = "sparrow-reflection-${project.version}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }
}

publishing {
    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.momirealms.net/releases")
            credentials(PasswordCredentials::class) {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("core") {
            groupId = "net.momirealms"
            artifactId = "sparrow-reflection"
            version = project.version.toString()
            from(components["java"])
            pom {
                name = "Sparrow Reflection"
                url = "https://github.com/Xiao-MoMi/sparrow-reflection"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                        distribution = "repo"
                    }
                }
            }
        }
    }
}