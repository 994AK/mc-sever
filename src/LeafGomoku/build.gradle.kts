import org.gradle.jvm.tasks.Jar

version = "0.1.0"

dependencies {
    compileOnly(files(rootProject.layout.projectDirectory.file("plugins/PlaceholderAPI-2.12.2.jar")))
    compileOnly(files(rootProject.layout.projectDirectory.file("plugins/worldedit-bukkit-7.4.2.jar")))

    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    implementation("org.slf4j:slf4j-api:2.0.18")
}

extra["mainClassTests"] = listOf(
    "net.leafmc.gomoku.GomokuRulesTest",
    "net.leafmc.gomoku.MatchControllerTest",
    "net.leafmc.gomoku.UndoRequestTest",
    "net.leafmc.gomoku.InviteRequestTest",
    "net.leafmc.gomoku.BoardGeometryTest",
    "net.leafmc.gomoku.RoomLayoutFactoryTest",
    "net.leafmc.gomoku.AppearanceCatalogTest",
    "net.leafmc.gomoku.EnvironmentCatalogTest",
    "net.leafmc.gomoku.RoomEnvironmentLayoutTest",
    "net.leafmc.gomoku.SqliteDataStoreTest",
    "net.leafmc.gomoku.AppearanceUnlockServiceTest",
    "net.leafmc.gomoku.StatsServiceTest",
)

tasks.named<Jar>("shadowJar") {
    exclude(
        "org/sqlite/native/FreeBSD/**",
        "org/sqlite/native/Linux-Musl/**",
        "org/sqlite/native/Linux/arm/**",
        "org/sqlite/native/Linux/armv6/**",
        "org/sqlite/native/Linux/armv7/**",
        "org/sqlite/native/Linux/ppc64/**",
        "org/sqlite/native/Linux/riscv64/**",
        "org/sqlite/native/Linux/x86/**",
        "org/sqlite/native/Windows/armv7/**",
        "org/sqlite/native/Windows/x86/**",
    )
}
