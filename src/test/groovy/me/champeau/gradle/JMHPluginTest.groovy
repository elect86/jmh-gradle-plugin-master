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
package me.champeau.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Ignore
import org.junit.Test

class JMHPluginTest {
    @Test
    void testPluginIsApplied() {
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        project.apply plugin: 'java'
        project.apply plugin: 'me.champeau.gradle.jmh'


        def task = project.tasks.findByName('jmh')
        assert task instanceof JmhTask

        def jmhConfigurations = project.configurations*.name.findAll { it.startsWith('jmh') }
        println(jmhConfigurations)
        println project.configurations.jmhCompileClasspath.extendsFrom
        println project.configurations.jmhCompileClasspath.files
    }

    @Test
    void testPluginIsAppliedWithGroovy() {
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        project.apply plugin: 'groovy'
        project.apply plugin: 'me.champeau.gradle.jmh'


        def task = project.tasks.findByName('jmh')
        assert task instanceof JmhTask

    }

    @Test
    void testPluginIsAppliedWithoutZip64() {
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        project.apply plugin: 'groovy'
        project.apply plugin: 'me.champeau.gradle.jmh'


        def task = project.tasks.findByName('jmhJar')
        assert task.zip64 == false
        assert task instanceof Jar

    }

    @Ignore
    @Test
    void testPluginIsAppliedWithZip64() {
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        project.apply plugin: 'groovy'
        project.apply plugin: 'me.champeau.gradle.jmh'

        project.jmh.zip64 = true


        def task = project.tasks.findByName('jmhJar')
        assert task instanceof Jar
        assert task.zip64

    }

    @Test
    void testAllJmhTasksBelongToJmhGroup() {
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        project.apply plugin: 'java'
        project.apply plugin: 'me.champeau.gradle.jmh'

        project.tasks.find { it.name.startsWith('jmh') }.each {
            assert it.group == JmhPlugin.JMH_GROUP
        }
    }

    @Test
    void testPluginIsAppliedTogetherWithShadow() {
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.johnrengelman.shadow'
        project.apply plugin: 'me.champeau.gradle.jmh'

        def task = project.tasks.findByName('jmhJar')
        assert task instanceof ShadowJar
    }

    @Test
    void testDuplicateClassesStrategyIsSetToFailByDefault() {
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        project.apply plugin: 'java'
        project.apply plugin: 'me.champeau.gradle.jmh'

        assert project.jmh.duplicateClassesStrategy == DuplicatesStrategy.FAIL
    }
}
