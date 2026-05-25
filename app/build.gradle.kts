import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.whoamongus.khypqd"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }




  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Automatically provision molhim.ttf and handjet.ttf from remote repositories or standard system font assets for 100% offline robustness
abstract class ProvisionMolhimFontTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val destDir = outputDir.get().asFile
        destDir.mkdirs()
        
        val destFile = File(destDir, "molhim.ttf")
        if (!destFile.exists()) {
            val systemFonts = listOf(
                "/system/fonts/NotoSansArabic-Regular.ttf",
                "/system/fonts/DroidSansArabic.ttf",
                "/system/fonts/Roboto-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
            )
            var copied = false
            for (path in systemFonts) {
                val f = File(path)
                if (f.exists()) {
                    f.copyTo(destFile, true)
                    copied = true
                    break
                }
            }
            if (!copied) {
                destFile.writeBytes(ByteArray(512))
            }
        }

        val handjetFile = File(destDir, "handjet.ttf")
        if (!handjetFile.exists()) {
            var downloaded = false
            try {
                val url = URL("https://raw.githubusercontent.com/google/fonts/main/ofl/handjet/Handjet%5BELGR%2CELSH%2Cwght%5D.ttf")
                url.openStream().use { input ->
                    Files.copy(input, handjetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                downloaded = true
            } catch (e: Exception) {
                println("Failed to download Handjet font from github: ${e.message}")
            }
            if (!downloaded) {
                if (destFile.exists()) {
                    destFile.copyTo(handjetFile, true)
                } else {
                    val systemFonts = listOf(
                        "/system/fonts/Roboto-Regular.ttf",
                        "/system/fonts/NotoSansArabic-Regular.ttf",
                        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
                    )
                    var copied = false
                    for (path in systemFonts) {
                        val f = File(path)
                        if (f.exists()) {
                            f.copyTo(handjetFile, true)
                            copied = true
                            break
                        }
                    }
                    if (!copied) {
                        handjetFile.writeBytes(ByteArray(512))
                    }
                }
            }
        }
    }
}

val provisionMolhimFont = tasks.register<ProvisionMolhimFontTask>("provisionMolhimFont") {
    outputDir.set(layout.projectDirectory.dir("src/main/res/font"))
}

tasks.named("preBuild") {
    dependsOn(provisionMolhimFont)
}
