package me.champeau.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class MultiLanguageSpec {

    @Test
    fun `Execute #language benchmarks`() {

        listOf("Groovy", "Java", "Kotlin"/*, "Scala"*/).forEach { language ->
            val projectDir = File("src/funcTest/resources/${language.toLowerCase()}-project")
            val pluginClasspathResource = this::class.java.classLoader.getResourceAsStream("plugin-classpath.txt")
                    ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
            val pluginClasspath = pluginClasspathResource.reader().readLines().map(::File)

            val project = GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withPluginClasspath(pluginClasspath)
                    .withArguments("-S", "clean", "jmh")
                    .forwardOutput()
                    .build()

            val taskResult = project.task(":jmh")!!
            val benchmarkResults = File(projectDir, "build/reports/benchmarks.csv").readText()

            assert(taskResult.outcome == TaskOutcome.SUCCESS)
            assert(language + "Benchmark.sqrtBenchmark" in benchmarkResults)
        }
    }
}
