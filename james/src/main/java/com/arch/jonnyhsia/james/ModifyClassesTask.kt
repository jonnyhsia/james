package com.arch.jonnyhsia.james

import com.joom.grip.GripFactory
import com.joom.grip.classes
import com.joom.grip.interfaces
import groovyjarjarasm.asm.Opcodes
import javassist.ClassPool
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

private const val TASK_COLLECTOR_CLZ_NAME = "com/arch/jonnyhsia/startup/TaskRegister.class"

abstract class ModifyClassesTask : DefaultTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:Classpath
    abstract val bootClasspath: ListProperty<RegularFile>

    @get:CompileClasspath
    abstract var classpath: FileCollection

    @TaskAction
    fun taskAction() {
        println("task action begin----------")

        // 输入的 jar、aar、源码
        val inputs = (allJars.get() + allDirectories.get()).map { it.asFile.toPath() }

        // 系统依赖
        val classPaths = bootClasspath.get().map { it.asFile.toPath() }
            .toSet() + classpath.files.map { it.toPath() }

        // 扫描收集所有实现 ModuleTaskRegister 的类
        val grip = GripFactory.newInstance(Opcodes.ASM9)
            .create(classPaths + inputs)
        val moduleTaskRegisters = grip.select(classes)
            .from(inputs)
            .where(interfaces { _, types ->
                types.size == 1 && types.first().className == "com.arch.jonnyhsia.startup.facade.ModuleTaskRegister"
            })
            .execute()
            .classes
            .toList()

        // 打印
        for (classMirror in moduleTaskRegisters) {
            println("ModuleTaskRegister is found: ${classMirror.name}")
        }

        val pool = ClassPool(ClassPool.getDefault())

        val jarOutput = JarOutputStream(
            BufferedOutputStream(
                FileOutputStream(output.get().asFile)
            )
        )
        allJars.get().forEach { file ->
            val jarFile = JarFile(file.asFile)
            jarFile.entries().iterator().forEach { jarEntry ->
                // 查找需要插桩的目标类文件
                if (TASK_COLLECTOR_CLZ_NAME == jarEntry.name) {
                    println("Found: ${jarEntry.name}")
                    val classBytes =
                        ASMUtilClassUtil.getClassBytes(pool, moduleTaskRegisters)
                    jarOutput.putNextEntry(JarEntry(jarEntry.name))
                    // 覆盖原代码文件
                    jarOutput.write(classBytes)
                    jarOutput.closeEntry()
                } else {
                    try {
                        jarOutput.putNextEntry(JarEntry(jarEntry.name))
                        jarFile.getInputStream(jarEntry).use {
                            it.copyTo(jarOutput)
                        }
                        jarOutput.closeEntry()
                    } catch (e: Exception) {
                        println(e.message)
                        println(jarEntry.name)
                    }
                }
            }
            jarFile.close()
        }

        allDirectories.get().forEach { directory ->
            println("handling " + directory.asFile.absolutePath)
            directory.asFile.walk().forEach { file ->
                if (file.isFile) {
                    val relativePath = directory.asFile.toURI().relativize(file.toURI()).path
                    jarOutput.putNextEntry(JarEntry(relativePath.replace(File.separatorChar, '/')))
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(jarOutput)
                    }
                    jarOutput.closeEntry()
                }
            }
        }

        jarOutput.close()

        println("task action end------------")
    }
}