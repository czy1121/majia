package me.reezy.gradle.majia.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.build.gradle.tasks.ProcessApplicationManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

class MajiaPlugin : Plugin<Project> {

    open class PluginExtension {
        var scopes: List<String> = listOf()
        var mappings: Map<String, String> = mapOf()
        var variant: String = "release"
    }

    override fun apply(project: Project) {
        project.extensions.create("majia", PluginExtension::class.java)

        val config = project.extensions.findByName("majia") as PluginExtension

        val ace = project.extensions.getByType(AndroidComponentsExtension::class.java)


        ace.onVariants(ace.selector().all()) { variant ->
            if (!variant.name.contains(config.variant, ignoreCase = true) || variant.name.contains("test", ignoreCase = true)) return@onVariants

            val taskProvider = project.tasks.register("majiaClass${variant.name.capitalized()}", MajiaClassTask::class.java) { task ->
                task.scopes = config.scopes.map { it.replace(".", "/") }
                task.mappings = config.mappings.map { MajiaClassTask.MappingItem(it.key, it.value) }
            }
            variant.artifacts.forScope(ScopedArtifacts.Scope.ALL).use(taskProvider).toTransform(
                ScopedArtifact.CLASSES,
                MajiaClassTask::allJars,
                MajiaClassTask::allDirectories,
                MajiaClassTask::output
            )
        }

        project.afterEvaluate {

            project.tasks.create("majiaXml${config.variant.capitalized()}", MajiaXmlTask::class.java) { task ->

                task.mappings = config.mappings

                project.tasks.withType(LinkApplicationAndroidResourcesTask::class.java).named("process${config.variant.capitalized()}Resources").configure { processRes ->
                    task.ap = File(processRes.resPackageOutputFolder.asFile.orNull, "resources-${config.variant}.ap_")
                    task.dependsOn(processRes)
                    processRes.finalizedBy(task)
                    processRes.proguardOutputFile
                }
            }

            project.tasks.withType(ProcessApplicationManifest::class.java).named("process${config.variant.capitalized()}MainManifest").configure { task ->
                task.doLast {
                    task.mergedManifest.get().asFile.transformManifest(config.mappings)
                }
            }
        }

    }

    private fun File.transformManifest(mapping: Map<String, String>)  {
        val origin = readText()
        var result = origin
        for ((k, v) in mapping) {
            result = result.replace(k, v)
        }
        if (result != origin) {
            writeText(result)
        }
    }

}

