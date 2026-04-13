import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    // Append source paths for debugging — don't replace CMP's automatic paths
                    static = (static ?: mutableListOf()).also {
                        it.addAll(listOf(rootDirPath, projectDirPath))
                    }
                }
            }
        }
        binaries.executable()
        compilerOptions {
            // Entire target is WasmJs — opt-in once here rather than annotating every file
            freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
        }
    }
    
    sourceSets {
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation3.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            implementation("io.ktor:ktor-client-core:3.0.2")
            implementation("io.ktor:ktor-client-json:3.0.2")
            implementation("io.ktor:ktor-client-logging:3.0.2")
            implementation("io.ktor:ktor-client-serialization:3.0.2")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.viewModel)
            implementation(libs.koin.compose)
            // Koin Annotations
            api("io.insert-koin:koin-annotations:1.4.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0-beta01")

            // Markdown rendering
            implementation("com.mikepenz:multiplatform-markdown-renderer:0.32.0")
            implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.32.0")

            //Coil
            implementation(libs.coil.compose)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)

            //Kotlinx DateTime
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")

            //Video Player
//            implementation("io.github.khubaibkhan4:mediaplayer-kmp:2.0.4")
//            implementation("network.chaintech:compose-multiplatform-media-player:1.0.25")
//            implementation("com.github.Hamamas:Kotlin-Wasm-Html-Interop:1.0.1")
//            implementation(project(":libraries:Kotlin-Wasm-Html-Interop-master"))

            // Adaptive layout components — uncomment for Phase 3 (ListDetailPaneScaffold etc.)
            // Causes Kotlin/Wasm compiler hangs if added before they're actually imported
            // implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.1.0")
            // implementation("org.jetbrains.compose.material3.adaptive:adaptive-layout:1.1.0")
            // Phase 2 — correct group is material3, not adaptive subgroup
            // implementation("org.jetbrains.compose.material3:material3-adaptive-navigation-suite:1.1.0")

            // File Picker
            // Enables FileKit without Compose dependencies
            implementation("io.github.vinceglb:filekit-core:0.8.8")

            // Enables FileKit with Composable utilities
            implementation("io.github.vinceglb:filekit-compose:0.8.8")


            // Emoji support (Noto images for Wasm)
            implementation("org.kodein.emoji:emoji-compose-m3:2.0.1")

            // Icons Extension
            implementation("org.jetbrains.compose.material:material-icons-extended:1.6.11")
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
        }
    }


    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

// ── Build-time version injection ────────────────────────────────
val generateBuildConfig by tasks.registering {
    val version = project.findProperty("appVersion")?.toString() ?: "0.0.0"
    val outputDir = layout.buildDirectory.dir("generated/buildconfig/kotlin")
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("net/winedownwednesday/web")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package net.winedownwednesday.web
            |
            |/** Auto-generated — do NOT edit. Sourced from gradle.properties `appVersion`. */
            |object BuildConfig {
            |    const val VERSION: String = "$version"
            |}
            """.trimMargin()
        )
    }
}

kotlin.sourceSets.named("wasmJsMain") {
    kotlin.srcDir(generateBuildConfig.map { layout.buildDirectory.dir("generated/buildconfig/kotlin").get() })
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateBuildConfig)
}
