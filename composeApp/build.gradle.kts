
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.devtools.ksp")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
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
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0-RC")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
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
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

            //Coil
            implementation(libs.coil.compose)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)

            //Kotlinx DateTime
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

            //Video Player
//            implementation("io.github.khubaibkhan4:mediaplayer-kmp:2.0.4")
//            implementation("network.chaintech:compose-multiplatform-media-player:1.0.25")
//            implementation("com.github.Hamamas:Kotlin-Wasm-Html-Interop:1.0.1")
//            implementation(project(":libraries:Kotlin-Wasm-Html-Interop-master"))

            implementation("dev.chrisbanes.material3:material3-window-size-class-multiplatform:0.5.0")
        }
    }

    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}




