plugins {
    id("com.tomkeuper.bedwars.java-conventions")
}

dependencies {
    implementation(project(":bedwars-api"))
    implementation(project(":versionsupport_common"))
    compileOnly("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT")
}

description = "versionsupport_v1_19_r3"
