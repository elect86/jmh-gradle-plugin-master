package me.champeau.gradle

//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.java.archives.Attributes
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import java.util.concurrent.atomic.AtomicReference

val ExtensionContainer.jmh: JmhPluginExtension
    get() = getByType(JmhPluginExtension::class.java)


val Project.sourceSets: SourceSetContainer
    get() = properties["sourceSets"] as SourceSetContainer

fun Project.sourceSets(block: SourceSetContainer.() -> Unit) =
        sourceSets.block()


val SourceSetContainer.jmh: SourceSet
    get() = findByName(Jmh.name) ?: create(Jmh.name)

fun SourceSetContainer.jmh(block: SourceSet.() -> Unit) =
        jmh.block()

operator fun TaskContainer.invoke(block: TaskContainer.() -> Unit) = block()

val TaskContainer.jmhJar: Jar
    get() = (findByName(Jmh.jarTaskName) ?: create(Jmh.jarTaskName, Jar::class.java)) as Jar

fun TaskContainer.jmhJar(block: Jar.() -> Unit) = jmhJar.block()

//fun TaskContainer.jmhJar(block: Jar.() -> Unit) =
//    jmhJar.block()


val TaskContainer.jar: Jar
    get() = (findByName("jar") ?: create("jar")) as Jar

fun TaskContainer.jar(block: Jar.() -> Unit) = jar.block()


val TaskContainer.jmh: JmhTask
    get() = findByName(Jmh.name) as? JmhTask ?: create(Jmh.name, JmhTask::class.java)

fun TaskContainer.jmh(block: JmhTask.() -> Unit) = jmh.block()


val TaskContainer.jmhRunBytecodeGenerator: JmhBytecodeGeneratorTask
    get() = findByName("jmhRunBytecodeGenerator") as? JmhBytecodeGeneratorTask
            ?: create("jmhRunBytecodeGenerator", JmhBytecodeGeneratorTask::class.java)

fun TaskContainer.jmhRunBytecodeGenerator(block: JmhBytecodeGeneratorTask.() -> Unit) = jmhRunBytecodeGenerator.block()


val TaskContainer.jmhCompileGeneratedClasses: JavaCompile
    get() = findByName("jmhCompileGeneratedClasses") as? JavaCompile
            ?: create("jmhCompileGeneratedClasses", JavaCompile::class.java)

fun TaskContainer.jmhCompileGeneratedClasses(block: JavaCompile.() -> Unit) = jmhCompileGeneratedClasses.block()


//val TaskContainer.shadowJar: ShadowJar
//    get() = findByName("shadowJar") as? ShadowJar ?: create("shadowJar", ShadowJar::class.java)
//
//fun TaskContainer.shadowJar(block: ShadowJar.() -> Unit) = shadowJar.block()


val SourceSetContainer.main: SourceSet
    get() = findByName("main") ?: create("main")

val SourceSetContainer.test: SourceSet
    get() = findByName("test") ?: create("test")


var Attributes.mainClass: String
    get() = get("Main-Class") as String
    set(value) = set("Main-Class", value)

var Attributes.classPath: String
    get() = get("Class-Path") as String
    set(value) = set("Class-Path", value)

// Configurations

operator fun <R> ConfigurationContainer.invoke(block: ConfigurationContainer.() -> R): R =
        block()


val ConfigurationContainer.jmhImplementation: Configuration
    get() = findByName("jmhImplementation") ?: create("jmhImplementation")

val ConfigurationContainer.jmh: Configuration
    get() = findByName(Jmh.name) ?: create(Jmh.name)

val ConfigurationContainer.jmhCompileClasspath: Configuration
    get() = findByName("jmhCompileClasspath") ?: create("jmhCompileClasspath")

val ConfigurationContainer.jmhRuntimeClasspath: Configuration
    get() = findByName("jmhRuntimeClasspath") ?: create("jmhRuntimeClasspath")

val ConfigurationContainer.implementation: Configuration
    get() = findByName("implementation") ?: create("implementation")

val ConfigurationContainer.compileOnly: Configuration
    get() = findByName("compileOnly") ?: create("compileOnly")

val ConfigurationContainer.runtimeOnly: Configuration
    get() = findByName("runtimeOnly") ?: create("runtimeOnly")

val ConfigurationContainer.runtimeClasspath: Configuration
    get() = findByName("runtimeClasspath") ?: create("runtimeClasspath")

val ConfigurationContainer.testRuntimeClasspath: Configuration
    get() = findByName("testRuntimeClasspath") ?: create("testRuntimeClasspath")

val ConfigurationContainer.testCompileClasspath: Configuration
    get() = findByName("testCompileClasspath") ?: create("testCompileClasspath")

val ConfigurationContainer.shadow: Configuration
    get() = findByName("shadow") ?: create("shadow")

val ConfigurationContainer.jmhRuntime: Configuration
    get() = findByName(Jmh.runtimeConfiguration) ?: create(Jmh.runtimeConfiguration)

fun ConfigurationContainer.jmhRuntime(block: Configuration.() -> Unit) =
        jmhRuntime.block()


var ExtraPropertiesExtension.jmhLastAddedTask: AtomicReference<JmhTask>
    get() = when {
        has("jmhLastAddedTask") -> get("jmhLastAddedTask") as AtomicReference<JmhTask>
        else -> AtomicReference<JmhTask>()
    }
    set(value) = set("jmhLastAddedTask", value)


operator fun <T>Property<T>.invoke(): T = get()
operator fun <T>Property<T>.invoke(value: T?) = set(value)

//inline operator fun <reified S : Task> DomainObjectCollection<S>.invoke(configureAction: Action<in S>) =
//        withType(S::class.java, configureAction)

inline operator fun <reified S : Task> TaskContainer.invoke(configureAction: Action<in S>): DomainObjectCollection<S> =
        withType(S::class.java, configureAction)

inline fun <reified P : Plugin<Project>> PluginContainer.find(): P? =
        findPlugin(P::class.java)

inline fun <reified E> ExtensionContainer.get(): E =
        getByType(E::class.java)