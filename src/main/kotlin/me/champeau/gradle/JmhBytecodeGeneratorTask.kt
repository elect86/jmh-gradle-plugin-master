package me.champeau.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
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

//@CompileStatic
@CacheableTask
open class JmhBytecodeGeneratorTask : DefaultTask() {
    private val sourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    @Classpath
    val runtimeClasspath = sourceSets.jmh.runtimeClasspath

    @Classpath
    val testClasses = sourceSets.test.output

    @Classpath
    val testRuntimeClasspath = sourceSets.test.runtimeClasspath

    @Classpath
    val classesDirs = sourceSets.jmh.output.classesDirs

    @OutputDirectory
    lateinit var generatedClassesDir: File

    @OutputDirectory
    lateinit var generatedSourcesDir: File

    @Input
    val generatorType = "default"

    @Input
    val includeTests: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @TaskAction
    fun generate() {
        val workerExecutor = services.get(WorkerExecutor::class.java)
        val workQueue = workerExecutor.processIsolation {
            classpath.from(runtimeClasspath.files)
            if (includeTests())
                classpath.from(testClasses.files + testRuntimeClasspath.files)
        }
        workQueue.submit(JmhBytecodeGeneratorWorker::class.java) {
            compiledBytecodeDirectories = classesDirs.files.toTypedArray()
            outputSourceDirectory = generatedSourcesDir
            outputResourceDirectory = generatedClassesDir
            generatorType = this@JmhBytecodeGeneratorTask.generatorType
        }
    }
}

interface JmhBytecodeGeneratorWorkParameters : WorkParameters {
    var compiledBytecodeDirectories: Array<File>
    var outputSourceDirectory: File
    var outputResourceDirectory: File
    var generatorType: String
}

abstract class JmhBytecodeGeneratorWorker @Inject constructor() : WorkAction<JmhBytecodeGeneratorWorkParameters> {

    override fun execute() {
        cleanup(parameters.outputSourceDirectory)
        cleanup(parameters.outputResourceDirectory)
        var generatorType = parameters.generatorType
        if (generatorType == JmhBytecodeGenerator.GENERATOR_TYPE_DEFAULT)
            generatorType = JmhBytecodeGenerator.DEFAULT_GENERATOR_TYPE
        val urls = arrayOfNulls<URL>(parameters.compiledBytecodeDirectories.size)
        for (i in parameters.compiledBytecodeDirectories.indices)
            try {
                urls[i] = parameters.compiledBytecodeDirectories[i].toURI().toURL()
            } catch (e: MalformedURLException) {
                throw RuntimeException(e)
            }

        // Include compiled bytecode on classpath, in case we need to
        // resolve the cross-class dependencies
        val amendedCL = URLClassLoader(urls, this.javaClass.classLoader)
        val ocl = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = amendedCL
            val destination = FileSystemDestination(parameters.outputResourceDirectory, parameters.outputSourceDirectory)
            val allClasses: MutableMap<File, Collection<File>> = HashMap(urls.size)
            for (compiledBytecodeDirectory in parameters.compiledBytecodeDirectories) {
                val classes = FileUtils.getClasses(compiledBytecodeDirectory)
                println("Processing ${classes.size} classes from $compiledBytecodeDirectory with \"$generatorType\" generator")
                allClasses[compiledBytecodeDirectory] = classes
            }
            println("Writing out Java source to ${parameters.outputSourceDirectory} and resources to ${parameters.outputResourceDirectory}")
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
                file.listFiles()?.forEach(::cleanup)
                file.delete()
            }
        }
    }
}

//@CompileStatic
//@CacheableTask
//open class JmhBytecodeGeneratorTask : DefaultTask() {
//    val sourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
//    val includeTestsState: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)
//
//    @Classpath
//    val runtimeClasspath = sourceSets.jmh.runtimeClasspath
//
//    @Classpath
//    val testClasses = sourceSets.test.output
//
//    @Classpath
//    val testRuntimeClasspath = sourceSets.test.runtimeClasspath
//
//    @Classpath
//    val classesDirs = sourceSets.jmh.output.classesDirs
//
//    @OutputDirectory
//    lateinit var generatedClassesDir: File
//
//    @OutputDirectory
//    lateinit var generatedSourcesDir: File
//
//    @Input
//    val generatorType = "default"
//
//    @Input
//    fun getIncludeTests(): Property<Boolean> = includeTestsState
//
//    @TaskAction
//    fun generate() {
//        val workerExecutor = services.get(WorkerExecutor::class.java)
////        val workQueue = workerExecutor.processIsolation {
////            classpath.from(runtimeClasspath.files)
////            if (getIncludeTests()())
////                classpath.from(testClasses.files + testRuntimeClasspath.files)
////        }
////        workQueue.submit(JmhBytecodeGeneratorRunnable::class) {
////
////        }
//        workerExecutor.submit(JmhBytecodeGeneratorRunnable::class.java) {
//            isolationMode = IsolationMode.PROCESS
//            val classpath = runtimeClasspath.files.toMutableSet()
//            if (getIncludeTests()())
//                classpath += testClasses.files + testRuntimeClasspath.files
//            this.classpath = classpath
//            params(classesDirs.files.toTypedArray(), generatedSourcesDir, generatedClassesDir, generatorType)
//        }
//    }
//}
//
//class JmhBytecodeGeneratorRunnable @Inject constructor(private val compiledBytecodeDirectories: Array<File>,
//                                                       private val outputSourceDirectory: File,
//                                                       private val outputResourceDirectory: File,
//                                                       private val generatorType: String) : Runnable {
//    override fun run() {
//        cleanup(outputSourceDirectory)
//        cleanup(outputResourceDirectory)
//        var generatorType = generatorType
//        if (generatorType == JmhBytecodeGenerator.GENERATOR_TYPE_DEFAULT)
//            generatorType = JmhBytecodeGenerator.DEFAULT_GENERATOR_TYPE
//        val urls = arrayOfNulls<URL>(compiledBytecodeDirectories.size)
//        for (i in compiledBytecodeDirectories.indices)
//            try {
//                urls[i] = compiledBytecodeDirectories[i].toURI().toURL()
//            } catch (e: MalformedURLException) {
//                throw RuntimeException(e)
//            }
//
//        // Include compiled bytecode on classpath, in case we need to
//        // resolve the cross-class dependencies
//        val amendedCL = URLClassLoader(urls, this.javaClass.classLoader)
//        val ocl = Thread.currentThread().contextClassLoader
//        try {
//            Thread.currentThread().contextClassLoader = amendedCL
//            val destination = FileSystemDestination(outputResourceDirectory, outputSourceDirectory)
//            val allClasses: MutableMap<File, Collection<File>> = HashMap(urls.size)
//            for (compiledBytecodeDirectory in compiledBytecodeDirectories) {
//                val classes = FileUtils.getClasses(compiledBytecodeDirectory)
//                println("Processing ${classes.size} classes from $compiledBytecodeDirectory with \"$generatorType\" generator")
//                allClasses[compiledBytecodeDirectory] = classes
//            }
//            println("Writing out Java source to $outputSourceDirectory and resources to $outputResourceDirectory")
//            for ((compiledBytecodeDirectory, classes) in allClasses) {
//                var source: GeneratorSource? = null
//                if (generatorType.equals(JmhBytecodeGenerator.GENERATOR_TYPE_ASM, ignoreCase = true)) {
//                    val src = ASMGeneratorSource()
//                    try {
//                        src.processClasses(classes)
//                    } catch (e: IOException) {
//                        throw RuntimeException(e)
//                    }
//                    source = src
//                } else if (generatorType.equals(JmhBytecodeGenerator.GENERATOR_TYPE_REFLECTION, ignoreCase = true)) {
//                    val src = RFGeneratorSource()
//                    for (f in classes) {
//                        var name = f.absolutePath.substring(compiledBytecodeDirectory.absolutePath.length + 1)
//                        name = name.replace("\\\\".toRegex(), ".")
//                        name = name.replace("/".toRegex(), ".")
//                        if (name.endsWith(".class"))
//                            try {
//                                src.processClasses(Class.forName(name.substring(0, name.length - 6), false, amendedCL))
//                            } catch (e: ClassNotFoundException) {
//                                throw RuntimeException(e)
//                            }
//                    }
//                    source = src
//                }
//                val gen = BenchmarkGenerator()
//                gen.generate(source, destination)
//                gen.complete(source, destination)
//            }
//            if (destination.hasErrors()) {
//                var errCount = 0
//                val sb = StringBuilder()
//                for (e in destination.errors) {
//                    errCount++
//                    sb.append("  - ").append(e.toString()).append("\n")
//                }
//                throw RuntimeException("Generation of JMH bytecode failed with $errCount errors:\n$sb")
//            }
//        } finally {
//            try {
//                amendedCL.close()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//            Thread.currentThread().contextClassLoader = ocl
//        }
//    }
//
//    companion object {
//        private fun cleanup(file: File) {
//            if (file.exists()) {
//                file.listFiles()?.forEach(::cleanup)
//                file.delete()
//            }
//        }
//    }
//}
