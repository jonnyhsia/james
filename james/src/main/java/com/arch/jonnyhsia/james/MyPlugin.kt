package com.arch.jonnyhsia.james

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class MyPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        println("----------------")
        println("hello plugin kt!")
        println("----------------")

//        val android = target.extensions.getByType<AppExtension>()
//        android.registerTransform()

        target.plugins.withType(AppPlugin::class.java) {


            val androidComponents =
                target.extensions.findByType(AndroidComponentsExtension::class.java)
            androidComponents?.onVariants { variant ->
//                variant.instrumentation.transformClassesWith(
//                    ModuleClassVisitorFactory::class.java,
//                    InstrumentationScope.PROJECT
//                ){}
//                variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES)
                useTaskProvider(target, androidComponents, variant)
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun useTaskProvider(
        target: Project,
        androidComponents: AndroidComponentsExtension<*, *, *>,
        variant: Variant
    ) {
        val name = "${variant.name}JamesModify"
        val taskProvider = target.tasks.register<ModifyClassesTask>(name) {
            group = "component"
            description = name
            bootClasspath.set(androidComponents.sdkComponents.bootClasspath)
            classpath = variant.compileClasspath
        }
        variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
            .use(taskProvider)
            .toTransform(
                ScopedArtifact.CLASSES,
                ModifyClassesTask::allJars,
                ModifyClassesTask::allDirectories,
                ModifyClassesTask::output,
            )
    }
}
