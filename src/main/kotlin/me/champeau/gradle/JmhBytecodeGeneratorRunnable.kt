package me.champeau.gradle

import org.openjdk.jmh.generators.asm.ASMGeneratorSource
import org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator
import org.openjdk.jmh.generators.core.BenchmarkGenerator
import org.openjdk.jmh.generators.core.FileSystemDestination
import org.openjdk.jmh.generators.core.GeneratorSource
import org.openjdk.jmh.generators.reflection.RFGeneratorSource
import org.openjdk.jmh.util.FileUtils
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import javax.inject.Inject


class JmhBytecodeGeneratorRunnable @Inject constructor(private val compiledBytecodeDirectories: Array<File>,
                                                       private val outputSourceDirectory: File,
                                                       private val outputResourceDirectory: File,
                                                       private val generatorType: String) : Runnable {
    override fun run() {
        cleanup(outputSourceDirectory)
        cleanup(outputResourceDirectory)
        var generatorType = generatorType
        if (generatorType == JmhBytecodeGenerator.GENERATOR_TYPE_DEFAULT)
            generatorType = JmhBytecodeGenerator.DEFAULT_GENERATOR_TYPE
        val urls = arrayOfNulls<URL>(compiledBytecodeDirectories.size)
        for (i in compiledBytecodeDirectories.indices)
            try {
                urls[i] = compiledBytecodeDirectories[i].toURI().toURL()
            } catch (e: MalformedURLException) {
                throw RuntimeException(e)
            }

        // Include compiled bytecode on classpath, in case we need to
        // resolve the cross-class dependencies
        val amendedCL = URLClassLoader(urls, this.javaClass.classLoader)
        val ocl = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = amendedCL
            val destination = FileSystemDestination(outputResourceDirectory, outputSourceDirectory)
            val allClasses: MutableMap<File, Collection<File>> = HashMap(urls.size)
            for (compiledBytecodeDirectory in compiledBytecodeDirectories) {
                val classes = FileUtils.getClasses(compiledBytecodeDirectory)
                println("Processing ${classes.size} classes from $compiledBytecodeDirectory with \"$generatorType\" generator")
                allClasses[compiledBytecodeDirectory] = classes
            }
            println("Writing out Java source to $outputSourceDirectory and resources to $outputResourceDirectory")
            for ((compiledBytecodeDirectory, classes) in allClasses) {
                var source: GeneratorSource? = null
                if (generatorType.equals(JmhBytecodeGenerator.GENERATOR_TYPE_ASM, ignoreCase = true)) {
                    val src = ASMGeneratorSource()
                    try {
                        src.processClasses(classes)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                    source = src
                } else if (generatorType.equals(JmhBytecodeGenerator.GENERATOR_TYPE_REFLECTION, ignoreCase = true)) {
                    val src = RFGeneratorSource()
                    for (f in classes) {
                        var name = f.absolutePath.substring(compiledBytecodeDirectory.absolutePath.length + 1)
                        name = name.replace("\\\\".toRegex(), ".")
                        name = name.replace("/".toRegex(), ".")
                        if (name.endsWith(".class"))
                            try {
                                src.processClasses(Class.forName(name.substring(0, name.length - 6), false, amendedCL))
                            } catch (e: ClassNotFoundException) {
                                throw RuntimeException(e)
                            }
                    }
                    source = src
                }
                val gen = BenchmarkGenerator()
                gen.generate(source, destination)
                gen.complete(source, destination)
            }
            if (destination.hasErrors()) {
                var errCount = 0
                val sb = StringBuilder()
                for (e in destination.errors) {
                    errCount++
                    sb.append("  - ").append(e.toString()).append("\n")
                }
                throw RuntimeException("Generation of JMH bytecode failed with $errCount errors:\n$sb")
            }
        } finally {
            try {
                amendedCL.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Thread.currentThread().contextClassLoader = ocl
        }
    }

    companion object {
        private fun cleanup(file: File) {
            if (file.exists()) {
                file.listFiles()?.forEach { sub -> cleanup(sub) }
                file.delete()
            }
        }
    }

}
