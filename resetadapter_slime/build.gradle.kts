dependencies {
    compileOnly(projects.bedwarsApi)
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT"){
        exclude("commons-lang", "commons-lang")
    }
    compileOnly("com.grinderwolf:slimeworldmanager-api:2.2.1")
    compileOnly("commons-io:commons-io:2.11.0")
}

tasks.compileJava {
    options.release.set(11)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.codemc.io/repository/nms/") // Spigot
    maven("https://repo.papermc.io/repository/maven-public/") // bungeecord-chat (dep of spigot-api)
    maven("https://repo.glaremasters.me/repository/concuncan/") // slimeworldmanager-api
}