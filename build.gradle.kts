/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.gradle.build-scan") version "2.1"
    id("me.champeau.buildscan-recipes") version "0.2.3"
    id("com.jfrog.bintray") version "1.8.0"
    id("com.jfrog.artifactory") version "4.6.0"
    id("com.github.hierynomus.license") version "0.14.0"
    id("net.nemerosa.versioning") version "2.6.1"
    id("com.github.ben-manes.versions") version "0.17.0"
    id("com.gradle.plugin-publish") version "0.9.9"
    id("com.github.kt3k.coveralls") version "2.8.2"
    id("jacoco")
    id("idea")
    id("java-gradle-plugin")
    kotlin("jvm") version "1.3.31"
    `kotlin-dsl`
    groovy
}

kotlinDslPluginOptions.experimentalWarning.set(false)

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

buildScanRecipes {
    recipes("git-status", "travis-ci")
    recipe(mapOf("baseUrl" to "https://github.com/melix/jmh-gradle-plugin/tree"), "git-commit")
}

apply(from="gradle/credentials.gradle")
apply(from="gradle/compile.gradle")
apply(from="gradle/test.gradle")
apply(from="gradle/funcTest.gradle")
apply(from="gradle/publishing.gradle")
apply(from="gradle/bintray.gradle")
apply(from="gradle/artifactory.gradle")
apply(from="gradle/code-quality.gradle")

val jmhVersion: String by project
val junitVersion: String by project
val spockVersion: String by project
val shadowVersion: String by project
val jacocoVersion: String by project
val kotestVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(localGroovy())
    implementation(gradleApi())
    implementation("org.openjdk.jmh:jmh-core:$jmhVersion")
    compileOnly("org.openjdk.jmh:jmh-generator-bytecode:$jmhVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.spockframework:spock-core:$spockVersion") {
        exclude(mapOf("group" to "org.codehaus.groovy", "module" to "groovy-all"))
    }
    testImplementation("com.github.jengelman.gradle.plugins:shadow:$shadowVersion")

    testImplementation("org.openjdk.jmh:jmh-core:$jmhVersion")
    testImplementation("org.openjdk.jmh:jmh-generator-bytecode:$jmhVersion")

    listOf("runner-junit5", "assertions-core", "runner-console"/*, "property"*/).forEach {
        testImplementation("io.kotest:kotest-$it-jvm:$kotestVersion")
    }
}

tasks {
    register("release") {
        description = "Releases a version of the plugin on Artifactory and Bintray"
        dependsOn("build")
        dependsOn("artifactoryPublish")
        dependsOn("bintrayUpload")
    }

    register("publishRelease") {
        dependsOn("bintrayUpload")
        dependsOn("publishPlugins")
    }

    compileGroovy {
        dependsOn(compileKotlin)
        classpath += files(compileKotlin.get().destinationDir)
    }
    classes { dependsOn(compileGroovy) }
}


jacoco {
    toolVersion = jacocoVersion
}

tasks.jacocoTestReport {
    group = "Reporting"
    description = "Generate Jacoco coverage reports after running tests."
    additionalSourceDirs.setFrom(project.files(sourceSets.main.get().allSource.srcDirs))
    sourceDirectories.setFrom(project.files(sourceSets.main.get().allSource.srcDirs))
    classDirectories.setFrom(project.files(sourceSets.main.get().output))
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = true
    }
}
