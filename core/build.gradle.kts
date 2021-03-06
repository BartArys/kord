import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(Plugins.kapt)
}

dependencies {
    api(common)
    api(rest)
    api(gateway)

    api(Dependencies.`cache-api`) {
        version {
            strictly("0.1.4")
        }
    }

    api(Dependencies.`cache-map`) {
        version {
            strictly("0.1.4")
        }
    }

    testImplementation(Dependencies.mockk)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Jvm.target
        freeCompilerArgs = listOf(
                CompilerArguments.inlineClasses,
                CompilerArguments.coroutines,
                CompilerArguments.time,
                CompilerArguments.stdLib,
                CompilerArguments.optIn
        )
    }
}
