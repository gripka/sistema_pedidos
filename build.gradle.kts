plugins {
    kotlin("jvm") version "1.8.0"
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.runtime") version "1.12.7"
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

val appVersion = "0.7.5"
val appName = "Blossom ERP"
val appVendor = "Gripka"
val appDescription = "Sistema completo de gestão de pedidos, estoque e clientes."

dependencies {
    implementation("org.openjfx:javafx-controls:17.0.2")
    implementation("org.openjfx:javafx-fxml:17.0.2")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.jfoenix:jfoenix:9.0.10")
    implementation("com.github.anastaciocintra:escpos-coffee:4.1.0")
    implementation("com.itextpdf:itext7-core:7.2.3")
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.json:json:20230618")
}

application {
    mainClass.set("com.sistema_pedidos.MainKt")
    version = appVersion
}

javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

// Create license file directly at configuration time
val licenseFile = file("installer-resources/license.txt")
licenseFile.parentFile.mkdirs() // Create the directory
if (!licenseFile.exists()) {
    licenseFile.writeText("""
        $appName - License Agreement

        Copyright © 2024 $appVendor
        All rights reserved.

        This software is provided 'as-is', without any express or implied warranty.
    """.trimIndent())
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))

    jpackage {
        // Application details
        imageName = appName
        appVersion = this@Build_gradle.appVersion
        installerName = "${appName}-${this@Build_gradle.appVersion}"

        // Copy additional files to the distribution
        resourceDir = file("installer-resources")

        // Base image options
        imageOptions = listOf(
            "--icon", "src/main/resources/icons/icon.ico",
            "--copyright", "Copyright © 2024 $appVendor",
            "--verbose"
        )

        installerOptions = listOf(
            // Windows-specific options
            "--win-menu",
            "--win-menu-group", appName,
            "--win-shortcut",
            //"--win-dir-chooser",
            "--win-per-user-install",
            "--win-upgrade-uuid", "d5195868-1f73-4223-b45a-328c62b4d7a1",
            "--win-shortcut-prompt",
            "--resource-dir", "installer-resources",

            "--icon", "src/main/resources/icons/icon.ico",

            "--description", appDescription,
            "--vendor", appVendor,

            "--license-file", licenseFile.absolutePath
        )

        installerType = "exe"
    }
}

tasks.register("generateVersionProperties") {
    doLast {
        val propertiesFile = file("${projectDir}/src/main/resources/app.properties")
        propertiesFile.parentFile.mkdirs()
        propertiesFile.writeText("app.version=$appVersion\n")
    }
}

tasks.named("processResources") {
    dependsOn("generateVersionProperties")
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "--module-path", configurations.runtimeClasspath.get().asPath,
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}

// Optional: copy additional installer resources if they exist
tasks.register<Copy>("prepareInstallerResources") {
    from("src/main/resources/installer") {
        include("**/*")
    }
    into("installer-resources")
}

// Make tasks depend on prepareInstallerResources
tasks.named("jpackageImage") {
    dependsOn("prepareInstallerResources")
}

tasks.named("jpackage") {
    dependsOn("verifyJar")
}

tasks.register("verifyJar") {
    dependsOn("jar")
    doLast {
        val jarTask = tasks.jar.get()
        println("Checking JAR contents: ${jarTask.archiveFile.get().asFile}")
        zipTree(jarTask.archiveFile).files.filter {
            it.name == "app.properties"
        }.forEach {
            println("Found ${it.name} in JAR")
        }
    }
}
