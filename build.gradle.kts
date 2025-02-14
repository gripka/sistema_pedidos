plugins {
    kotlin("jvm") version "1.8.0"
    application
    id("org.openjfx.javafxplugin") version "0.0.13" // Plugin JavaFX
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjfx:javafx-controls:17.0.2") // Or a later version if needed
    implementation("org.openjfx:javafx-fxml:17.0.2") // If you're using FXML
}

application {
    mainClass.set("com.sistema_pedidos.MainKt")
}

javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
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
    // Ensure JavaFX modules are added to the runtime classpath
    jvmArgs = listOf(
        "--module-path", configurations.runtimeClasspath.get().asPath,
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}