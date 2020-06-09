package me.champeau.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class MultiProjectLanguageSpec {

    @Test
    fun `Should not execute JMH tests from different projects concurrently`() {

        val projectDir = File("src/funcTest/resources/java-multi-project")
        val pluginClasspathResource = this::class.java.classLoader.getResourceAsStream("plugin-classpath.txt")
                ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        val pluginClasspath = pluginClasspathResource.reader().readLines().map(::File)

        val project = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath(pluginClasspath)
                .withArguments("-S", "clean", "jmh")
                .build()

        var taskResult = project.task(":jmh")!!
        var benchmarkResults = File(projectDir, "build/reports/benchmarks.csv").readText()

        assert(taskResult.outcome == TaskOutcome.SUCCESS)
        assert("JavaBenchmark.sqrtBenchmark" in benchmarkResults)

        taskResult = project.task(":subproject:jmh")!!
        benchmarkResults = File(projectDir, "subproject/build/reports/benchmarks.csv").readText()

        assert(taskResult.outcome == TaskOutcome.SUCCESS)
        assert("JavaMultiBenchmark.sqrtBenchmark" in benchmarkResults)
    }
}