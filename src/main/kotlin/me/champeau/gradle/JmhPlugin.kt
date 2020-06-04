//package me.champeau.gradle
//
////import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
////import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
//import org.gradle.BuildAdapter
//import org.gradle.BuildResult
//import org.gradle.api.Plugin
//import org.gradle.api.Project
//import org.gradle.api.artifacts.Configuration
//import org.gradle.api.initialization.Settings
//import org.gradle.api.invocation.Gradle
//import org.gradle.api.plugins.JavaPlugin
//import org.gradle.jvm.tasks.Jar
//import org.gradle.plugins.ide.eclipse.EclipsePlugin
//import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin
//import org.gradle.plugins.ide.eclipse.model.EclipseModel
//import org.gradle.plugins.ide.idea.IdeaPlugin
//import org.gradle.util.GradleVersion
//import java.io.File
//
//open class JmhPlugin : Plugin<Project> {
//
//    override fun apply(project: Project) {
//        if (!`is gradle 5,5+?`)
//            throw RuntimeException("This version of the JMH Gradle plugin requires Gradle 5.5+. Please upgrade Gradle or use an older version of the plugin.")
//        project.plugins.apply(JavaPlugin::class.java)
//        val extension = project.extensions.create(Jmh.name, JmhPluginExtension::class.java, project)
//        val configuration = project.configurations.create(Jmh.name)
//        val runtimeConfiguration = createJmhRuntimeConfiguration(project, extension)
//
//        val dependencyHandler = project.dependencies
//        configuration.withDependencies {
//            add(dependencyHandler.create("${Jmh.coreDependency}${Jmh.version}"))
//            add(dependencyHandler.create("${Jmh.generatorDependency}${Jmh.version}"))
//        }
//
//        ensureTasksNotExecutedConcurrently(project)
//
//        createJmhSourceSet(project)
//
//        registerBuildListener(project, extension)
//
//        val jmhGeneratedSourcesDir = project.file("$project.buildDir/jmh-generated-sources")
//        val jmhGeneratedClassesDir = project.file("$project.buildDir/jmh-generated-classes")
//        val jmhGeneratedResourcesDir = project.file("$project.buildDir/jmh-generated-resources")
//        createJmhRunBytecodeGeneratorTask(project, jmhGeneratedSourcesDir, extension, jmhGeneratedResourcesDir)
//
//        createJmhCompileGeneratedClassesTask(project, jmhGeneratedSourcesDir, jmhGeneratedClassesDir, extension)
//
//        val metaInfExcludes = listOf("module-info.class", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
////        when {
////            project.plugins.findPlugin(ShadowPlugin::class.java) != null ->
////                createShadowJmhJar(project, extension, jmhGeneratedResourcesDir, jmhGeneratedClassesDir, metaInfExcludes, runtimeConfiguration)
////            else ->
//        createStandardJmhJar(project, extension, metaInfExcludes, jmhGeneratedResourcesDir, jmhGeneratedClassesDir, runtimeConfiguration)
////        }
//        project.tasks.jmh {
//            group = Jmh.group
//            dependsOn(project.tasks.jmhJar)
//        }
//
//        configureIDESupport(project)
//    }
//
//    private fun configureIDESupport(project: Project) {
//        project.afterEvaluate {
//            project.plugins.findPlugin(IdeaPlugin::class.java)?.model?.module {
//                scopes["TEST"]!!["plus"]!!.add(project.configurations.getAt("jmh"))
//                project.sourceSets.jmh.java.srcDirs.forEach { dir ->
//                    testSourceDirs.add(project.file(dir))
//                }
//            }
//            val hasEclipsePlugin = project.plugins.findPlugin(EclipsePlugin::class.java)
//            val hasEclipseWtpPlugin = project.plugins.findPlugin(EclipseWtpPlugin::class.java)
//            if (hasEclipsePlugin != null || hasEclipseWtpPlugin != null)
//                project.extensions.getByType(EclipseModel::class.java).classpath
//                        .plusConfigurations.plus(project.configurations.getAt("jmh"))
//        }
//    }
//
//    private fun createJmhCompileGeneratedClassesTask(
//            project: Project, jmhGeneratedSourcesDir: File,
//            jmhGeneratedClassesDir: File, extension: JmhPluginExtension
//    ) = project.tasks.jmhCompileGeneratedClasses {
//        group = Jmh.group
//        dependsOn("jmhRunBytecodeGenerator")
//        project.sourceSets {
//            classpath = jmh.runtimeClasspath
//            if (extension.includeTests.get())
//                classpath += test.output + test.runtimeClasspath
//        }
//        source(jmhGeneratedSourcesDir)
//        destinationDir = jmhGeneratedClassesDir
//    }
//
//    private fun createJmhRunBytecodeGeneratorTask(
//            project: Project, jmhGeneratedSourcesDir: File,
//            extension: JmhPluginExtension, jmhGeneratedResourcesDir: File
//    ) = project.tasks.jmhRunBytecodeGenerator {
//        group = Jmh.group
//        dependsOn("jmhClasses")
//        includeTestsState.set(extension.includeTests.get())
//        generatedClassesDir = jmhGeneratedResourcesDir
//        generatedSourcesDir = jmhGeneratedSourcesDir
//    }
//
//    private fun createJmhSourceSet(project: Project) {
//        project.sourceSets {
//            jmh {
//                java.srcDir("src/jmh/java")
//                resources.srcDir("src/jmh/resources")
//                compileClasspath += main.output
//                runtimeClasspath += main.output
//            }
//        }
//        project.configurations {
//            // the following line is for backwards compatibility
//            // no one should really add directly to the "jmh" configuration
//            jmhImplementation.extendsFrom(jmh)
//
//            jmhCompileClasspath.extendsFrom(implementation, compileOnly)
//            jmhRuntimeClasspath.extendsFrom(implementation, runtimeOnly)
//        }
//    }
//
////    private fun createShadowJmhJar(
////            project: Project, extension: JmhPluginExtension, jmhGeneratedResourcesDir: File,
////            jmhGeneratedClassesDir: File, metaInfExcludes: List<String>,
////            runtimeConfiguration: Configuration
////    ) {
////        project.tasks.create(Jmh.jarTaskName, ShadowJar::class.java) {
////            group = Jmh.group
////            dependsOn(Jmh.taskCompileGeneratedClassesName)
////            description = "Create a combined JAR of project and runtime dependencies"
////            conventionMapping.map("classifier") { Jmh.name }
////            manifest.inheritFrom(project.tasks.jar.manifest)
////            manifest.attributes.mainClass = "org.openjdk.jmh.Main"
////            from(runtimeConfiguration)
////            doFirst {
////                fun processLibs(files: MutableSet<File>) {
////                    if (files.isNotEmpty()) {
////                        val libs = files.map { it.name } + manifest.attributes.classPath
////                        manifest.attributes.classPath = libs.distinct().joinToString(" ")
////                    }
////                }
////                processLibs(runtimeConfiguration.files)
////                processLibs(project.configurations.shadow.files)
////
////                if (extension.isIncludeTests)
////                    from(project.sourceSets.test.output)
////                eachFile {
////                    if (name.endsWith(".class"))
////                        duplicatesStrategy = extension.duplicateClassesStrategy
////                }
////            }
////            from(project.sourceSets.jmh.output)
////            from(project.sourceSets.main.output)
////            from(project.file(jmhGeneratedClassesDir))
////            from(project.file(jmhGeneratedResourcesDir))
////
////            exclude(metaInfExcludes)
////            configurations.clear()
////        }
////    }
//
//    private fun createStandardJmhJar(
//            project: Project, extension: JmhPluginExtension, metaInfExcludes: List<String>,
//            jmhGeneratedResourcesDir: File, jmhGeneratedClassesDir: File,
//            runtimeConfiguration: Configuration
//    ) {
//        project.tasks.create(Jmh.jarTaskName, Jar::class.java) {
//            group = Jmh.group
//            dependsOn(Jmh.taskCompileGeneratedClassesName)
//            inputs.files(project.sourceSets.jmh.output)
//            inputs.files(project.sourceSets.main.output)
//            if (extension.includeTests.get())
//                inputs.files.plus(project.sourceSets.test.output)
//            println(runtimeConfiguration.files.size)
//            from(runtimeConfiguration.asFileTree.map { f ->
//                if (f.isDirectory) f else project.zipTree(f)
//            }.toTypedArray()).exclude(metaInfExcludes)
//            doFirst {
//                this as Jar
//                from(project.sourceSets.jmh.output)
//                from(project.sourceSets.main.output)
//                from(project.file(jmhGeneratedClassesDir))
//                from(project.file(jmhGeneratedResourcesDir))
//                if (extension.includeTests.get())
//                    from(project.sourceSets.test.output)
//                eachFile {
//                    if (name.endsWith(".class"))
//                        duplicatesStrategy = extension.duplicateClassesStrategy
//                }
//            }
//
//            manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"
//
//            archiveClassifier.set(Jmh.name)
//        }
//    }
//
//    private fun registerBuildListener(project: Project, extension: JmhPluginExtension) {
//        println("registerBuildListener")
//        project.gradle.addBuildListener(object : BuildAdapter() {
//            //            override fun beforeSettings(settings: Settings) = println("beforeSettings")
//            override fun buildFinished(result: BuildResult) = println("buildFinished")
//            override fun projectsLoaded(gradle: Gradle) = println("projectsLoaded")
//            override fun settingsEvaluated(settings: Settings) = println("settingsEvaluated")
//            override fun buildStarted(gradle: Gradle) = println("buildStarted")
//            override fun projectsEvaluated(gradle: Gradle) {
//                println("projectsEvaluated")
//                if (extension.includeTests.get())
//                    project.sourceSets {
//                        jmh {
//                            compileClasspath += test.output + project.configurations.testCompileClasspath
//                            runtimeClasspath += test.output + project.configurations.testRuntimeClasspath
//                        }
//                    }
//
//                project.tasks.jmhJar.isZip64 = extension.isZip64
//            }
//        })
//    }
//
//    companion object {
//        val `is gradle 5,5+?`: Boolean
//            get() = GradleVersion.current() >= GradleVersion.version("5.5.0")
//
//        // TODO: This is really bad. We shouldn't use "runtime", but use the configurations provided by Gradle
//        // automatically when creating a source set. That is to say, "jmhRuntimeOnly" for example and wire
//        // our classpath properly
//        private fun createJmhRuntimeConfiguration(project: Project, extension: JmhPluginExtension): Configuration =
//                project.configurations.create(Jmh.runtimeConfiguration).apply {
//                    isCanBeConsumed = false
//                    isCanBeResolved = true
//                    isVisible = false
//                    extendsFrom(project.configurations.getByName("jmh"))
//                    extendsFrom(project.configurations.getByName("runtimeClasspath"))
//                    project.afterEvaluate {
//                        if (extension.includeTests.get())
//                            extendsFrom(project.configurations.getByName("testRuntimeClasspath"))
//                    }
//                }
//
//        private fun ensureTasksNotExecutedConcurrently(project: Project) {
//            val rootExtra = project.rootProject.extensions.extraProperties
//            val lastAddedRef = rootExtra.jmhLastAddedTask
//            rootExtra.jmhLastAddedTask = lastAddedRef
//
//            project.tasks.withType(JmhTask::class.java) {
//                lastAddedRef.getAndSet(this)?.let { mustRunAfter(it) }
//            }
//        }
//    }
//}