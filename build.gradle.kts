import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import org.ajoberstar.gradle.git.publish.GitPublishExtension
import org.ajoberstar.gradle.git.publish.tasks.GitPublishReset

buildscript {
    repositories {
        jcenter()
        maven(url = "https://plugins.gradle.org/m2/")
    }
    dependencies {
        //https://github.com/melix/japicmp-gradle-plugin/issues/36
        classpath("com.google.guava:guava:28.2-jre")

        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintray}")
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${Versions.atomicFu}")
        classpath("org.jetbrains.kotlinx:binary-compatibility-validator:${Versions.binaryCompatibilityValidator}")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version Versions.kotlin
    id("org.jetbrains.dokka") version "1.4.0"
    id("org.ajoberstar.git-publish") version "2.1.3"
}

apply(plugin = "binary-compatibility-validator")

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev/")
    mavenCentral()
    jcenter()
    mavenLocal()
}

dependencies {
    api(Dependencies.jdk8)
}

group = Library.group
version = Library.version

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "com.jfrog.bintray")
    apply(plugin = "maven-publish")
    apply(plugin = "kotlinx-atomicfu")
    apply(plugin = "org.jetbrains.dokka")

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://dl.bintray.com/kordlib/Kord")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev/")
    }

    dependencies {
        api(Dependencies.jdk8)
        api(Dependencies.`kotlinx-serialization`)
        implementation(Dependencies.`kotlinx-serialization-json`)
        api(Dependencies.`kotlinx-coroutines`)
        implementation("org.jetbrains.kotlinx:atomicfu-jvm:${Versions.atomicFu}")
        implementation(Dependencies.`kotlin-logging`)

        testImplementation(Dependencies.`kotlin-test`)
        testImplementation(Dependencies.junit5)
        testImplementation(Dependencies.`junit-jupiter-api`)
        testRuntimeOnly(Dependencies.`junit-jupiter-engine`)
        testImplementation(Dependencies.`kotlinx-coroutines-test`)
        testRuntimeOnly(Dependencies.`kotlin-reflect`)
        testRuntimeOnly(Dependencies.sl4j)
    }

    tasks.getByName("apiCheck").onlyIf { Library.stableApi }

    val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
    compileKotlin.kotlinOptions.jvmTarget = Jvm.target


    tasks.withType<Test> {
        useJUnitPlatform {
            includeEngines.plusAssign("junit-jupiter")
        }
    }


    tasks.dokkaHtml.configure {
        this.outputDirectory.set(file("${project.projectDir}/dokka/kord/"))

        dokkaSourceSets {
            configureEach {
                platform.set(org.jetbrains.dokka.Platform.jvm)

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(uri("https://github.com/kordlib/kord/tree/master/${project.name}/src/main/kotlin/").toURL())

                    remoteLineSuffix.set("#L")
                }

                jdkVersion.set(8)
            }
        }
    }


    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    apply<BintrayPlugin>()

    configure<PublishingExtension> {
        publications {
            register("kord", MavenPublication::class) {
                from(components["kotlin"])
                groupId = Library.group
                artifactId = "kord-${project.name}"
                version = Library.version

                artifact(sourcesJar.get())
            }
        }
    }

    configure<BintrayExtension> {
        user = System.getenv("BINTRAY_USER")
        key = System.getenv("BINTRAY_KEY")
        setPublications("kord")
        publish = true

        pkg = PackageConfig().apply {
            repo = "Kord"
            name = "Kord"
            userOrg = "kordlib"
            setLicenses("MIT")
            vcsUrl = "https://gitlab.com/kordlib/kord.git"
            websiteUrl = "https://gitlab.com/kordlib/kord.git"
            issueTrackerUrl = "https://gitlab.com/kordlib/kord/issues"

            version = VersionConfig().apply {
                name = Library.version
                desc = Library.description
                vcsTag = Library.version
            }
        }
    }
}

tasks {
    val dokkaOutputDir = "${rootProject.projectDir}/dokka"

    val clean = getByName("clean", Delete::class) {
        delete(rootProject.buildDir)
        delete(dokkaOutputDir)
    }

    dokkaHtmlMultiModule.configure {
        dependsOn(clean)
        outputDirectory.set(file(dokkaOutputDir))
        documentationFileName.set("DokkaDescription.md")
    }


    val fixIndex by register<DocsTask>("fixIndex") {
        dependsOn(dokkaHtmlMultimodule)
    }

    val gitPublishReset by getting(GitPublishReset::class) {
        dependsOn(fixIndex)
    }

}

configure<GitPublishExtension> {
    repoUri.set("https://github.com/kordlib/kord.git")
    branch.set("gh-pages")

    contents {
        from(file("${project.projectDir}/dokka"))
    }

    commitMessage.set("Update Docs")
}
