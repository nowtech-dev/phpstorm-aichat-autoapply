plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.2.20"
  id("org.jetbrains.intellij.platform") version "2.4.0"
}

group = "dev.nowtech"
version = "1.0.5"

kotlin {
  jvmToolchain(21)
}

val phpstormHome = providers.gradleProperty("phpstorm.home").get()
val mlLlmLib = providers.gradleProperty("ml.llm.lib").get()

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    local(phpstormHome)
  }

  val mlLib = file(mlLlmLib)
  val phpstormLib = file(phpstormHome).resolve("lib")
  compileOnly(
    files(
      mlLib.resolve("modules/intellij.ml.llm.chat.shared.jar"),
      mlLib.resolve("modules/intellij.ml.llm.chat.jar"),
      mlLib.resolve("modules/intellij.ml.llm.aui.events.jar"),
      phpstormLib.resolve("fleet.rpc.jar"),
    ),
  )
}

intellijPlatform {
  pluginConfiguration {
    id = "dev.nowtech.aichat.autoapply"
    name = "AI Chat Auto Always Allow"
    version = project.version.toString()
    ideaVersion {
      sinceBuild = "262.10968"
      untilBuild = "262.*"
    }
    vendor {
      name = "NOW Tech"
      url = "https://nowtech.dev"
    }
  }
}

tasks {
  buildSearchableOptions {
    enabled = false
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
  }
}
