package moe.gogo

data class Config(
    val java: String,
    val workingDir: String,
    val jarFile: String,
    val vmArgs: List<String>,
    val androidLib: String,
    val apk: String,
    val moduleCallName: String,
    val moduleCallArgs: List<String>,
    val ssacfgExtractName: String,
    val ssacfgExtractArgs: List<String>
)