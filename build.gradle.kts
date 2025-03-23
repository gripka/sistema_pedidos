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
}

application {
    mainClass.set("com.sistema_pedidos.MainKt")
}

javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    jpackage {
        imageName = "BlossomERP"
        imageOptions = listOf("--icon", "src/main/resources/icons/icon.ico")
        installerOptions = listOf(
            "--win-menu",
            "--win-shortcut",
            "--name", "Blossom ERP",
            "--app-version", "0.6.1",
            "--description", "Otimize a administração da sua floricultura ...",
            "--vendor", "Gripka"
        )
        installerType = "exe"
    }
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