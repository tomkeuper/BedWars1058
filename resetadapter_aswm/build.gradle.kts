plugins {
    id("com.tomkeuper.bedwars.java-conventions")
}

dependencies {
    api("com.flowpowered:flow-nbt:2.0.2")
    implementation(project(":bedwars-api"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.grinderwolf:slimeworldmanager-api:2.8.0-SNAPSHOT")
}

description = "resetadapter_aswm"
