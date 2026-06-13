import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.4.0" apply false
    id("com.gradleup.shadow") version "9.4.2" apply false
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21" apply false
    id("xyz.jpenilla.run-paper") version "3.0.2" apply false
}

val javaLanguageVersion = providers.gradleProperty("javaLanguageVersion").map(String::toInt).get()
val paperApiVersion = providers.gradleProperty("paperApiVersion").get()
val kotlinVersion = providers.gradleProperty("kotlinVersion").get()

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    group = "net.leafmc"

    extensions.configure<JavaPluginExtension>("java") {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
    }

    dependencies {
        "compileOnly"("io.papermc.paper:paper-api:$paperApiVersion")
        "testImplementation"("io.papermc.paper:paper-api:$paperApiVersion")
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaLanguageVersion)
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(javaLanguageVersion.toString()))
    }

    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests = false
    }

    tasks.processResources {
        filteringCharset = "UTF-8"
    }

    val shadowJar = tasks.named<Jar>("shadowJar") {
        archiveClassifier.set("")
        exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    }

    tasks.named("assemble") {
        dependsOn(shadowJar)
    }

    tasks.register<Copy>("installPlugin") {
        group = "leafmc"
        description = "Builds this plugin and copies the jar to the server plugins directory."
        dependsOn(shadowJar)
        from(shadowJar.flatMap { it.archiveFile })
        into(rootProject.layout.projectDirectory.dir("plugins"))
    }

    afterEvaluate {
        @Suppress("UNCHECKED_CAST")
        val mainClassTests = extensions.extraProperties.properties["mainClassTests"] as? List<String> ?: emptyList()
        if (mainClassTests.isNotEmpty()) {
            val sourceSets = extensions.getByType<SourceSetContainer>()
            val testRuntimeClasspath = sourceSets["test"].runtimeClasspath
            val runMainClassTest = tasks.register("mainClassTest") {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                description = "Runs lightweight main() based tests for ${project.name}."
                dependsOn(tasks.named("testClasses"))
            }
            mainClassTests.forEach { testClass ->
                val simpleName = testClass.substringAfterLast('.')
                val taskName = "run$simpleName"
                val testTask = tasks.register<JavaExec>(taskName) {
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    description = "Runs $testClass."
                    dependsOn(tasks.named("testClasses"))
                    classpath = testRuntimeClasspath
                    mainClass.set(testClass)
                }
                runMainClassTest.configure {
                    dependsOn(testTask)
                }
            }
            tasks.named("check") {
                dependsOn("mainClassTest")
            }
        }
    }
}

tasks.register("buildPlugins") {
    group = "leafmc"
    description = "Builds all local LeafMC plugin jars."
    dependsOn(subprojects.map { it.tasks.named("shadowJar") })
}

tasks.register("installPlugins") {
    group = "leafmc"
    description = "Builds all local LeafMC plugin jars and copies them to plugins/."
    dependsOn(subprojects.map { it.tasks.named("installPlugin") })
}
