package me.champeau.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * The JMH task converts our [extension configuration][JMHPluginExtension] into JMH specific
 * [Options][org.openjdk.jmh.runner.options.Options] then serializes them to disk. Then a forked
 * JVM is created and a runner is executed using the JMH version that was used to compile the benchmarks.
 * This runner will read the options from the serialized file and execute JMH using them.
 */
open class JmhTask
@Inject constructor(
        private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @TaskAction
    fun before() {
        val extension = project.extensions.getByType(JmhPluginExtension::class.java)
        val options = extension.resolveArgs()
        extension.resultsFile!!.parentFile.mkdirs()
        workerExecutor.submit(IsolatedRunner::class.java) {
            isolationMode = IsolationMode.PROCESS
            val configurations = project.configurations
            var classpath = configurations.getByName("jmh").plus(project.files(jarArchive))
            if (extension.isIncludeTests)
                classpath += (configurations.getByName("testRuntimeClasspath"))
            // TODO: This isn't quite right.  JMH is already a part of the worker classpath,
            // but we need it to be part of the "classpath under test" too.
            // We only need the jar for the benchmarks on the classpath so that the BenchmarkList resource reader
            // can find the BenchmarkList file in the jar.
            classpath(classpath)
            params(options, classpath.files)
            forkOptions.systemProperties[JAVA_IO_TMPDIR] = temporaryDir
        }
    }

    private val jarArchive: Provider<RegularFile>
        get() = (project.tasks.getByName(Jmh.jarTaskName) as Jar).archiveFile

    companion object {
        private const val JAVA_IO_TMPDIR = "java.io.tmpdir"
    }
}