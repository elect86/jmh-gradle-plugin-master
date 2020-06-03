package me.champeau.gradle

import org.gradle.api.GradleException
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.RunnerException
import org.openjdk.jmh.runner.options.Options
import java.io.File
import javax.inject.Inject

class IsolatedRunner @Inject constructor(private val options: Options, private val classpathUnderTest: Set<File>) : Runnable {
    override fun run() {
        val originalClasspath = System.getProperty("java.class.path")
        val runner = Runner(options)
        try {
            System.setProperty("java.class.path", toPath(classpathUnderTest))
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

    private fun toPath(classpathUnderTest: Set<File>): String {
        val sb = StringBuilder()
        for (entry in classpathUnderTest) {
            sb.append(entry.absolutePath)
            sb.append(File.pathSeparatorChar)
        }
        return sb.toString()
    }
}