plugins {
    kotlin("jvm") version "2.3.0-Beta2"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dk.marcusrokatis"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://nexus.scarsz.me/content/groups/public/") {
        name = "scarsz-repo"
    }
}

val paperApiVersion = "1.21.8-R0.1-SNAPSHOT"
val discordSRVVersion = "1.30.2"
val configUpdaterVersion = "2.2-SNAPSHOT"
val protocolLibVersion = "5.4.0"
dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("com.discordsrv:discordsrv:$discordSRVVersion")
    compileOnly("net.dmulloy2:ProtocolLib:$protocolLibVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.tchristofferson:ConfigUpdater:$configUpdaterVersion")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
