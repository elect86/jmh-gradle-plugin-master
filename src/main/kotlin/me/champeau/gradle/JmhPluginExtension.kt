package me.champeau.gradle

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.options.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

open class JmhPluginExtension(
        private val project: Project) {

    val includeTests: Property<Boolean> = project.objects.property(Boolean::class.java)

    init {
        includeTests(true)
    }

    var include: List<String> = ArrayList()
        private set
    var exclude: List<String> = ArrayList()
        private set
    var benchmarkMode: List<String>? = null
    var iterations: Int? = null
    var batchSize: Int? = null
    var fork: Int? = null
    var failOnError: Boolean? = null
    var forceGC: Boolean? = null
    var jvm: String? = null
    var jvmArgs: List<String>? = ArrayList() // do not use `null` or VM args would be copied over
        internal set
    var jvmArgsAppend: List<String>? = null
        internal set
    var jvmArgsPrepend: List<String>? = null
        internal set
    var humanOutputFile: File? = null
    var resultsFile: File? = null
    var operationsPerInvocation: Int? = null
    var benchmarkParameters: Map<String?, Collection<String>>? = null
    var profilers: List<String>? = null
    var timeOnIteration: String? = null
    private var resultExtension: String? = null
    var resultFormat: String? = null
    var synchronizeIterations: Boolean? = null
    var threads: Int? = null
    var threadGroups: List<Int>? = null
    var timeUnit: String? = null
    var verbosity: String? = null
    var timeout: String? = null
    var warmup: String? = null
    var warmupBatchSize: Int? = null
    var warmupForks: Int? = null
    var warmupIterations: Int? = null
    var warmupMode: String? = null
    var warmupBenchmarks: List<String>? = null
    var isZip64 = false
    var duplicateClassesStrategy = DuplicatesStrategy.FAIL

    fun resolveArgs(): Options {
        resolveResultExtension()
        resolveResultFormat()
        resolveResultsFile()

        // TODO: Maybe the extension can just set the options as we go instead of building this up at the end.
        val optionsBuilder = OptionsBuilder()
        if (profilers != null) {
            for (profiler in profilers!!) {
                val idx = profiler.indexOf(":")
                val profName = if (idx == -1) profiler else profiler.substring(0, idx)
                val params = if (idx == -1) "" else profiler.substring(idx + 1)
                optionsBuilder.addProfiler(profName, params)
            }
        }
        for (pattern in include)
            optionsBuilder.include(pattern)
        for (pattern in exclude)
            optionsBuilder.exclude(pattern)
        humanOutputFile?.absolutePath?.let(optionsBuilder::output)
        resultFormat?.toUpperCase()?.let { optionsBuilder.resultFormat(org.openjdk.jmh.results.format.ResultFormatType.valueOf(value = it)) }
        resultsFile?.absolutePath?.let(optionsBuilder::result)
        forceGC?.let(optionsBuilder::shouldDoGC)
        verbosity?.toUpperCase()?.let { optionsBuilder.verbosity(VerboseMode.valueOf(value = it)) }
        failOnError?.let(optionsBuilder::shouldFailOnError)
        threads?.let(optionsBuilder::threads)
        threadGroups?.toIntArray()?.let { optionsBuilder.threadGroups(*it) }
        synchronizeIterations?.let(optionsBuilder::syncIterations)
        warmupIterations?.let(optionsBuilder::warmupIterations)
        warmup?.let { optionsBuilder.warmupTime(TimeValue.fromString(it)) }
        warmupBatchSize?.let(optionsBuilder::warmupBatchSize)
        warmupMode?.let { optionsBuilder.warmupMode(WarmupMode.valueOf(value = it)) }
        warmupBenchmarks?.forEach { optionsBuilder.includeWarmup(it) }
        iterations?.let(optionsBuilder::measurementIterations)
        timeOnIteration?.let { optionsBuilder.measurementTime(TimeValue.fromString(it)) }
        batchSize?.let(optionsBuilder::measurementBatchSize)
        benchmarkMode?.forEach { optionsBuilder.mode(Mode.deepValueOf(it)) }
        timeUnit?.let { optionsBuilder.timeUnit(toTimeUnit(it)) }
        operationsPerInvocation?.let(optionsBuilder::operationsPerInvocation)
        fork?.let(optionsBuilder::forks)
        warmupForks?.let(optionsBuilder::warmupForks)
        jvm?.let(optionsBuilder::jvm)
        jvmArgs?.toTypedArray()?.let { optionsBuilder.jvmArgs(*it) }
        jvmArgsAppend?.toTypedArray()?.let { optionsBuilder.jvmArgsAppend(*it) }
        jvmArgsPrepend?.toTypedArray()?.let { optionsBuilder.jvmArgsPrepend(*it) }
        timeout?.let { optionsBuilder.timeout(TimeValue.fromString(it)) }
        benchmarkParameters?.forEach { k, v -> optionsBuilder.param(k, *v.toTypedArray()) }
        return optionsBuilder.build()
    }

    private fun resolveResultsFile() {
        resultsFile = resultsFile ?: project.file("${project.buildDir}/reports/jmh/results.$resultExtension")
    }

    private fun resolveResultExtension() {
        resultExtension = if (resultFormat != null) parseResultFormat() else "txt"
    }

    private fun resolveResultFormat() {
        resultFormat = resultFormat ?: "text"
    }

    private fun toTimeUnit(str: String): TimeUnit = when (str.toLowerCase()) {
        "ns" -> TimeUnit.NANOSECONDS
        "us" -> TimeUnit.MICROSECONDS
        "ms" -> TimeUnit.MILLISECONDS
        "s" -> TimeUnit.SECONDS
        "m" -> TimeUnit.MINUTES
        "h" -> TimeUnit.HOURS
        else -> throw IllegalArgumentException("Unknown time unit: $str")
    }

    private fun parseResultFormat(): String = ResultFormatType.translate(resultFormat!!)

    var isIncludeTests: Boolean
        get() = includeTests()
        set(includeTests) = this.includeTests(includeTests)

    private enum class ResultFormatType(private val extension: String) {
        TEXT("txt"), CSV("csv"), SCSV("scsv"), JSON("json"), LATEX("tex");

        companion object {
            fun translate(resultFormat: String): String = valueOf(resultFormat.toUpperCase()).extension
        }
    }
}
