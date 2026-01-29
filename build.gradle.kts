plugins {
    id("java")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "net.momirealms"
version = "1.0.0"

dependencies {
    implementation("org.ow2.asm:asm:9.9")
    compileOnly("org.jetbrains:annotations:26.0.2-1")
//    testImplementation(platform("org.junit:junit-bom:6.1.0-M1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
}

//tasks.test {
//    useJUnitPlatform()
//    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
//}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
    dependsOn(tasks.clean)
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