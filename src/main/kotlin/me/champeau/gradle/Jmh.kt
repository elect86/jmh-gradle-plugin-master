package me.champeau.gradle

object Jmh {

    val version = "1.23"
    val coreDependency = "org.openjdk.jmh:jmh-core:"
    val generatorDependency = "org.openjdk.jmh:jmh-generator-bytecode:"
    val group = "jmh"
    val name = "jmh"
    val jarTaskName = "jmhJar"
    val taskCompileGeneratedClassesName = "jmhCompileGeneratedClasses"
    val runtimeConfiguration = "jmhRuntime"
}