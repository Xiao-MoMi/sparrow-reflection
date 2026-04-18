package net.momirealms.sparrow.reflection.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import kotlin.text.set

class SparrowProxyPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val task = project.tasks.register("generateProxyMap", ProxyScannerTask::class.java) {
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val mainSourceSet = sourceSets.getByName("main")
            val compileJava = project.tasks.named("compileJava", org.gradle.api.tasks.compile.JavaCompile::class.java)
            classesDirs.from(mainSourceSet.output.classesDirs)
            outputFile.set(project.layout.buildDirectory.file("generated/sparrow/proxy_map.txt"))
            dependsOn(compileJava)
        }

        project.tasks.named("processResources", org.gradle.language.jvm.tasks.ProcessResources::class.java) {
            from(task.flatMap { it.outputFile }) {
                into("META-INF/sparrow")
            }
        }
    }
}