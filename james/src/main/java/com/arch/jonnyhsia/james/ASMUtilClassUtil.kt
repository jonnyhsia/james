package com.arch.jonnyhsia.james

import com.joom.grip.mirrors.ClassMirror
import javassist.ClassPool
import javassist.CtClass

object ASMUtilClassUtil {

    fun getClassBytes(classPool: ClassPool, moduleTaskRegisters: List<ClassMirror>): ByteArray {
        val classPath = "com.arch.jonnyhsia.startup.TaskRegister"
        val taskRegisterClz = classPool.getOrNullWithCheck(classPath)
            ?: classPool.makeClass(classPath)

        // 测试
        test(classPool)

        if (taskRegisterClz.isFrozen) {
            println("asmUtilClass is frozen, will call defrost() method")
            taskRegisterClz.defrost()
        }

        val initializeMethod = taskRegisterClz.getDeclaredMethod("initialize")

        for (method in taskRegisterClz.methods) {
            println("Method in TaskRegister: ${method.name}")
        }
        initializeMethod.setBody(buildString {
            append("{")
            moduleTaskRegisters.forEach {
                // register(new com.arch.jonnyhsia.startup.TaskRegister_app());
                append("register(new ${it.name}());")
            }
            append("}")
        })
        // taskRegisterClz.getDeclaredMethods("initialize").forEach {
        //     taskRegisterClz.removeMethod(it)
        // }
        // taskRegisterClz.addMethod(initializeMethod)

        return taskRegisterClz.toBytecode()
    }

    private fun test(classPool: ClassPool) {
        classPool.getOrNullWithCheck("java.lang.annotation.Annotation")
        classPool.getOrNullWithCheck("com.arch.jonnyhsia.james.MyPlugin")
        classPool.getOrNullWithCheck("android.app.Application")
        classPool.getOrNullWithCheck("com.arch.jonnyhsia.startup.StartUp")
    }
}

private fun ClassPool.getOrNullWithCheck(className: String): CtClass? {
    val clz = getOrNull(className)
    if (clz == null) {
        println("CtClass(${className}) not found in ClassPool!")
    } else {
        println("CtClass(${className}) found in ClassPool!")
    }
    return clz
}