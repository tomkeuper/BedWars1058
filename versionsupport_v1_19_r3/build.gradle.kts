dependencies {
    compileOnly(projects.bedwarsApi)
    implementation(projects.versionsupportCommon)
    compileOnly("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT"){
        exclude("commons-lang", "commons-lang")
    }
}

tasks.compileJava {
    options.release.set(17)
}

repositories {
    // Important Repos
    mavenCentral()
    mavenLocal()
    maven("https://repo.codemc.io/repository/nms/") // Spigot
    maven("https://repo.papermc.io/repository/maven-public/") // com.mojang (dep of Spigot)
}

description = "versionsupport_v1_19_r3"
