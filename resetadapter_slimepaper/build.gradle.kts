dependencies {
    compileOnly(projects.bedwarsApi)
    api("com.flowpowered:flow-nbt:2.0.2")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT"){
        exclude("commons-lang", "commons-lang")
    }
    compileOnly("com.infernalsuite.aswm:api:1.20-R0.1-SNAPSHOT")
    compileOnly("commons-io:commons-io:2.11.0")
}

tasks.compileJava {
    options.release.set(17)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.codemc.io/repository/nms/") // Spigot
    maven("https://repo.papermc.io/repository/maven-public/") // bungeecord-chat (dep of spigot-api)
    maven("https://repo.rapture.pw/repository/maven-releases/") // Flow-NBT
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/") // ASWM:api
}