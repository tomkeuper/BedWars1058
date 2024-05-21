dependencies {
    compileOnly(projects.bedwarsApi)
    implementation(projects.versionsupportCommon)
    compileOnly("org.spigotmc:spigot:1.20.6-R0.1-SNAPSHOT") {
        exclude("commons-lang", "commons-lang")
    }
    compileOnly("com.saicone.rtag:rtag:1.5.3")
    // Other modules
    compileOnly("com.saicone.rtag:rtag-block:1.5.3")
    compileOnly("com.saicone.rtag:rtag-entity:1.5.3")
    compileOnly("com.saicone.rtag:rtag-item:1.5.3")
}

tasks.compileJava {
    options.release.set(21)
}

repositories {
    // Important Repos
    mavenCentral()
    mavenLocal()
    maven("https://repo.codemc.io/repository/nms/") // Spigot
    maven("https://papermc.io/repo/repository/maven-public/") // com.mojang (dep of Spigot)
    maven("https://jitpack.io") // Jitpack (RTag)
}

description = "versionsupport_v1_20_r5"
