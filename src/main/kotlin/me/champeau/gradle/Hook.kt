package me.champeau.gradle

//import org.gradle.BuildAdapter
//import org.gradle.BuildResult
//import org.gradle.api.Project
//import org.gradle.api.artifacts.Configuration
//import org.gradle.api.initialization.Settings
//import org.gradle.api.invocation.Gradle
//import org.gradle.api.tasks.bundling.Jar
//import org.gradle.plugins.ide.eclipse.EclipsePlugin
//import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin
//import org.gradle.plugins.ide.eclipse.model.EclipseModel
//import org.gradle.plugins.ide.idea.IdeaPlugin
//import java.io.File
//
//class Hook {
//
//    companion object {
//        // TODO: This is really bad. We shouldn't use "runtime", but use the configurations provided by Gradle
//        // automatically when creating a source set. That is to say, "jmhRuntimeOnly" for example and wire
//        // our classpath properly
//        @JvmStatic
//        fun createJmhRuntimeConfiguration(project: Project, extension: JmhPluginExtension): Configuration =
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
//        @JvmStatic
//        fun ensureTasksNotExecutedConcurrently(project: Project) {
//            val rootExtra = project.rootProject.extensions.extraProperties
//            val lastAddedRef = rootExtra.jmhLastAddedTask
//            rootExtra.jmhLastAddedTask = lastAddedRef
//
//            project.tasks.withType(JmhTask::class.java) {
//                lastAddedRef.getAndSet(this)?.let { mustRunAfter(it) }
//            }
//        }
//
//        @JvmStatic
//        fun createJmhSourceSet(project: Project) {
//            project.sourceSets {
//                jmh {
//                    java.srcDir("src/jmh/java")
//                    resources.srcDir("src/jmh/resources")
//                    compileClasspath += main.output
//                    runtimeClasspath += main.output
//                }
//            }
//            project.configurations {
//                // the following line is for backwards compatibility
//                // no one should really add directly to the "jmh" configuration
//                jmhImplementation.extendsFrom(jmh)
//
//                jmhCompileClasspath.extendsFrom(implementation, compileOnly)
//                jmhRuntimeClasspath.extendsFrom(implementation, runtimeOnly)
//            }
//        }
//
//        @JvmStatic
//        fun registerBuildListener(project: Project, extension: JmhPluginExtension) {
//            println("registerBuildListener")
//            project.gradle.addBuildListener(object : BuildAdapter() {
//                //                            override fun beforeSettings(settings: Settings) = println("beforeSettings")
//                override fun buildFinished(result: BuildResult) = println("buildFinished")
//                override fun projectsLoaded(gradle: Gradle) = println("projectsLoaded")
//                override fun settingsEvaluated(settings: Settings) = println("settingsEvaluated")
//                override fun buildStarted(gradle: Gradle) = println("buildStarted")
//                override fun projectsEvaluated(gradle: Gradle) {
//                    println("projectsEvaluated")
//                    if (extension.includeTests.get())
//                        project.sourceSets {
//                            jmh {
//                                compileClasspath += test.output + project.configurations.testCompileClasspath
//                                runtimeClasspath += test.output + project.configurations.testRuntimeClasspath
//                            }
//                        }
//
//                    project.tasks.jmhJar.isZip64 = extension.isZip64
//                }
//            })
//        }
//
//        @JvmStatic
//        fun createJmhRunBytecodeGeneratorTask(project: Project, jmhGeneratedSourcesDir: File,
//                                              extension: JmhPluginExtension, jmhGeneratedResourcesDir: File) =
//                project.tasks.jmhRunBytecodeGenerator {
//                    group = Jmh.group
//                    dependsOn("jmhClasses")
//                    includeTestsState.set(extension.includeTests.get())
//                    generatedClassesDir = jmhGeneratedResourcesDir
//                    generatedSourcesDir = jmhGeneratedSourcesDir
//                }
//
//        @JvmStatic
//        fun createJmhCompileGeneratedClassesTask(project: Project, jmhGeneratedSourcesDir: File,
//                                                 jmhGeneratedClassesDir: File, extension: JmhPluginExtension) =
//                project.tasks.jmhCompileGeneratedClasses {
//                    group = Jmh.group
//                    dependsOn("jmhRunBytecodeGenerator")
//                    project.sourceSets {
//                        classpath = jmh.runtimeClasspath
//                        if (extension.includeTests.get())
//                            classpath += test.output + test.runtimeClasspath
//                    }
//                    source(jmhGeneratedSourcesDir)
//                    destinationDir = jmhGeneratedClassesDir
//                }
//
//        @JvmStatic
//        fun createStandardJmhJar(project: Project, extension: JmhPluginExtension, metaInfExcludes: List<String>,
//                                 jmhGeneratedResourcesDir: File, jmhGeneratedClassesDir: File, runtimeConfiguration: Configuration) {
//            project.tasks.create(Jmh.jarTaskName, Jar::class.java) {
//                group = Jmh.group
//                dependsOn(Jmh.taskCompileGeneratedClassesName)
//                inputs.files(project.sourceSets.jmh.output)
//                inputs.files(project.sourceSets.main.output)
//                if (extension.includeTests.get())
//                    inputs.files.plus(project.sourceSets.test.output)
//                from({
//                    runtimeConfiguration.asFileTree.map { f ->
//                        if (f.isDirectory) f else project.zipTree(f)
//                    }
//                }).exclude(metaInfExcludes)
//                doFirst {
//                    this as Jar
//                    from(project.sourceSets.jmh.output)
//                    from(project.sourceSets.main.output)
//                    from(project.file(jmhGeneratedClassesDir))
//                    from(project.file(jmhGeneratedResourcesDir))
//                    if (extension.includeTests.get())
//                        from(project.sourceSets.test.output)
//                    eachFile {
//                        if (name.endsWith(".class"))
//                            duplicatesStrategy = extension.duplicateClassesStrategy
//                    }
//                }
//
//                manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"
//
//                archiveClassifier.set(Jmh.name)
//            }
//        }
//
//        @JvmStatic
//        fun configureIDESupport(project: Project) {
//            project.afterEvaluate {
//                project.plugins.findPlugin(IdeaPlugin::class.java)?.model?.module {
//                    scopes["TEST"]!!["plus"]!!.add(project.configurations.getAt("jmh"))
//                    project.sourceSets.jmh.java.srcDirs.forEach { dir ->
//                        testSourceDirs.add(project.file(dir))
//                    }
//                }
//                val hasEclipsePlugin = project.plugins.findPlugin(EclipsePlugin::class.java)
//                val hasEclipseWtpPlugin = project.plugins.findPlugin(EclipseWtpPlugin::class.java)
//                if (hasEclipsePlugin != null || hasEclipseWtpPlugin != null)
//                    project.extensions.getByType(EclipseModel::class.java).classpath
//                            .plusConfigurations.plus(project.configurations.getAt("jmh"))
//            }
//        }
//    }
//}