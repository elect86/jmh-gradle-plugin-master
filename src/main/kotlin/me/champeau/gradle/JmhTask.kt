package me.champeau.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.RunnerException
import org.openjdk.jmh.runner.options.Options
import java.io.File
import javax.inject.Inject

/**
 * The JMH task converts our [extension configuration][JmhPluginExtension] into JMH specific
 * [Options][org.openjdk.jmh.runner.options.Options] then serializes them to disk. Then a forked
 * JVM is created and a runner is executed using the JMH version that was used to compile the benchmarks.
 * This runner will read the options from the serialized file and execute JMH using them.
 */
open class JmhTask
@Inject constructor(
        private val workerExecutor: WorkerExecutor) : DefaultTask() {

    private val extension = project.extensions.getByType(JmhPluginExtension::class.java)
    private var classpath: FileCollection

    init {
        val configurations = project.configurations
        classpath = configurations.jmh + project.files(jarArchive)
        if (extension.isIncludeTests)
            classpath += configurations.testRuntimeClasspath
    }

    @TaskAction
    fun before() {
        extension.resultsFile!!.parentFile.mkdirs()
        val workQueue = workerExecutor.processIsolation {
            classpath.from(this@JmhTask.classpath)
            forkOptions.systemProperties[JAVA_IO_TMPDIR] = temporaryDir
        }
        workQueue.submit(IsolatedWorker::class.java) {
            // TODO: This isn't quite right.  JMH is already a part of the worker classpath,
            // but we need it to be part of the "classpath under test" too.
            // We only need the jar for the benchmarks on the classpath so that the BenchmarkList resource reader
            // can find the BenchmarkList file in the jar.
            options = extension.resolveArgs()
            classpathUnderTest = classpath.files
        }
    }

    private val jarArchive: Provider<RegularFile>
        get() = (project.tasks.getByName(Jmh.jarTaskName) as Jar).archiveFile

    companion object {
        private const val JAVA_IO_TMPDIR = "java.io.tmpdir"
    }
}

interface JmhTaskWorkParameters : WorkParameters {
    var options: Options
    var classpathUnderTest: Set<File>
}

abstract class IsolatedWorker
@Inject
constructor() : WorkAction<JmhTaskWorkParameters> {

    override fun execute() {
        val originalClasspath = System.getProperty("java.class.path")
        val runner = Runner(parameters.options)
        try {
            System.setProperty("java.class.path", parameters.classpathUnderTest.toPath())
            // JMH uses the system property java.class.path to derive the runtime classpath of the forked JVM
            runner.run()
        } catch (e: RunnerException) {
            throw GradleException("Error during execution of benchmarks", e)
        } finally {
            runner.runSystemGC()
            if (originalClasspath != null)
                System.setProperty("java.class.path", originalClasspath)
            else
                System.clearProperty("java.class.path")
        }
    }

    private fun Set<File>.toPath(): String {
        val sb = StringBuilder()
        for (entry in this) {
            sb.append(entry.absolutePath)
            sb.append(File.pathSeparatorChar)
        }
        return sb.toString()
    }
}


//open class JmhTask
//@Inject constructor(
//        private val workerExecutor: WorkerExecutor) : DefaultTask() {
//    @TaskAction
//    fun before() {
//        val extension = project.extensions.getByType(JmhPluginExtension::class.java)
//        val options = extension.resolveArgs()
//        extension.resultsFile!!.parentFile.mkdirs()
//        workerExecutor.submit(IsolatedRunner::class.java) {
//            isolationMode = IsolationMode.PROCESS
//            val configurations = project.configurations
//            var classpath = configurations.getByName("jmh").plus(project.files(jarArchive))
//            if (extension.isIncludeTests)
//                classpath += (configurations.getByName("testRuntimeClasspath"))
//            // TODO: This isn't quite right.  JMH is already a part of the worker classpath,
//            // but we need it to be part of the "classpath under test" too.
//            // We only need the jar for the benchmarks on the classpath so that the BenchmarkList resource reader
//            // can find the BenchmarkList file in the jar.
//            classpath(classpath)
//            params(options, classpath.files)
//            forkOptions.systemProperties[JAVA_IO_TMPDIR] = temporaryDir
//        }
//    }
//
//    private val jarArchive: Provider<RegularFile>
//        get() = (project.tasks.getByName(Jmh.jarTaskName) as Jar).archiveFile
//
//    companion object {
//        private const val JAVA_IO_TMPDIR = "java.io.tmpdir"
//    }
//}
//
//class IsolatedRunner
//@Inject
//constructor(
//        private val options: Options,
//        private val classpathUnderTest: Set<File>) : Runnable {
//
//    override fun run() {
//        val originalClasspath = System.getProperty("java.class.path")
//        println("originalClasspath=\n${originalClasspath.splitToSequence(';').joinToString("\n")}")
////        println("options=$options")
////        println("options.excludes=${options.excludes}")
////        println("options.includes=${options.includes}")
//        val runner = Runner(options)
//        try {
//            println("classpathUnderTest=\n${toPathNl(classpathUnderTest)}")
//            System.setProperty("java.class.path", toPath(classpathUnderTest))
//            // JMH uses the system property java.class.path to derive the runtime classpath of the forked JVM
//            runner.run()
//        } catch (e: RunnerException) {
//            throw GradleException("Error during execution of benchmarks", e)
//        } finally {
//            runner.runSystemGC()
//            if (originalClasspath != null)
//                System.setProperty("java.class.path", originalClasspath)
//            else
//                System.clearProperty("java.class.path")
//        }
//    }
//
//    private fun toPath(classpathUnderTest: Set<File>): String {
//        val sb = StringBuilder()
//        for (entry in classpathUnderTest) {
//            sb.append(entry.absolutePath)
//            sb.append(File.pathSeparatorChar)
//        }
//        return sb.toString()
//    }
//
//    private fun toPathNl(classpathUnderTest: Set<File>): String {
//        val sb = StringBuilder()
//        for (entry in classpathUnderTest)
//            sb.append(entry.absolutePath + '\n')
//        return sb.toString()
//    }
//}