plugins {
    java
    `maven-publish`
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT"){
        exclude("commons-lang", "commons-lang")
    }
    compileOnly("com.iridium:IridiumColorAPI:1.0.9")
    compileOnly("org.apache.commons:commons-lang3:3.14.0") // Used by UridiumColorAPI
    compileOnly("com.github.NEZNAMY:TAB-API:4.1.9")
}

tasks.javadoc {
    enabled = true
}

repositories{
    mavenCentral()
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/") // bungecord-chat (dep of spigot-api (md-5:bungeecord-chat:1.8-SNAPSHOT))
    maven("https://nexus.iridiumdevelopment.net/repository/maven-releases/") // IridiumColorAPI
    maven("https://jitpack.io") // TAB
    maven("https://repo.codemc.io/repository/nms/") // Spigot
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = rootProject.group as String
            version = rootProject.version as String
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = if (project.version.toString().endsWith("SNAPSHOT")) {
                uri("https://repo.tomkeuper.com/repository/snapshots/")
            } else {
                uri("https://repo.tomkeuper.com/repository/releases/")
            }

            credentials {
                username = project.findProperty("deployUsername").toString()
                password = project.findProperty("deployPassword").toString()
            }
        }
    }
}