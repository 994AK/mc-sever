version = "0.1.0"

dependencies {
    compileOnly(rootProject.files("plugins/Residence6.0.1.8.jar"))
}

extra["mainClassTests"] = listOf(
    "net.leafmc.chainharvest.KotlinToolchainSmokeTest",
    "net.leafmc.chainharvest.MaterialCatalogTest",
    "net.leafmc.chainharvest.ChainSettingsTest",
    "net.leafmc.chainharvest.ProtectionGateTest",
    "net.leafmc.chainharvest.ChainPlannerTest",
    "net.leafmc.chainharvest.FarmPlannerTest",
    "net.leafmc.chainharvest.AdminMenuModelTest",
    "net.leafmc.chainharvest.ChainCommandFormatTest",
)
