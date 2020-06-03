package me.champeau.gradle

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import java.io.File

//@CompileStatic
@CacheableTask
open class JmhBytecodeGeneratorTask : DefaultTask() {
    val sourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    val includeTestsState: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @Classpath
    val runtimeClasspath = sourceSets.getByName("jmh").runtimeClasspath

    @Classpath
    val testClasses = sourceSets.getByName("test").output

    @Classpath
    val testRuntimeClasspath = sourceSets.getByName("test").runtimeClasspath

    @Classpath
    val classesDirs = sourceSets.getByName("jmh").output.classesDirs

    @OutputDirectory
    lateinit var generatedClassesDir: File

    @OutputDirectory
    lateinit var generatedSourcesDir: File

    @Input
    val generatorType = "default"

    @Input
    fun getIncludeTests(): Property<Boolean> = includeTestsState

    @TaskAction
    fun generate() {
        val workerExecutor = services.get(WorkerExecutor::class.java)
        workerExecutor.submit(JmhBytecodeGeneratorRunnable::class.java) {
            isolationMode = IsolationMode.PROCESS
            val classpath = runtimeClasspath.files.toMutableSet()
            if (getIncludeTests().get())
                classpath += testClasses.files + testRuntimeClasspath.files
            this.classpath = classpath
            params(classesDirs.files.toTypedArray(), generatedSourcesDir, generatedClassesDir, generatorType)
        }
    }
}