package moe.gogo

import com.beust.klaxon.Klaxon
import moe.gogo.jfr.MemoryRecordReader
import moe.gogo.report.Result
import java.io.File
import java.time.Duration
import java.time.Instant

fun main() {
    val config = Klaxon()
        .converter(ConfigConverter)
        .parse<Config>(File("config.json"))!!

    val root = File(config.workingDir)
    val moduleCallDir = root.resolve("module-call").also { it.mkdirs() }
    val ssacfgExtractDir = root.resolve("ssa-cfg-extract").also { it.mkdirs() }

    println("========== module-call ==========")
    val moduleCallProcess = moduleCallProcessBuilder(config)
    println(moduleCallProcess.command())
    val moduleCallTime = calcTime {
        moduleCallProcess.start().waitFor()
    }

    println("========== ssa-cfg-extract ==========")
    val ssacfgExtractProcess = ssacfgExtractProcessBuilder(config)
    println(ssacfgExtractProcess.command())
    val ssacfgExtractTime = calcTime {
        ssacfgExtractProcess.start().waitFor()
    }

    println("========== summary ==========")
    val result = Result(
        config,
        root,
        moduleCallDir,
        moduleCallTime,
        MemoryRecordReader(moduleCallDir.resolve("recording.jfr")).load(),
        ssacfgExtractDir,
        ssacfgExtractTime,
        MemoryRecordReader(ssacfgExtractDir.resolve("recording.jfr")).load()
    )

}

private fun calcTime(block: () -> Unit): Duration {
    val start = Instant.now()
    block()
    val end = Instant.now()
    return Duration.between(start, end)
}

private fun moduleCallProcessBuilder(config: Config): ProcessBuilder {
    return with(config) {
        ProcessBuilder()
            .command(
                java, *vmArgs.toTypedArray(),
                "-jar", jarFile.from(workingDir), moduleCallName,
                "-a", androidLib.from(workingDir), "-i", apk.from(workingDir), *moduleCallArgs.toTypedArray()
            )
            .directory(File(workingDir).resolve("module-call"))
            .inheritIO()
    }
}

private fun ssacfgExtractProcessBuilder(config: Config): ProcessBuilder {
    return with(config) {
        ProcessBuilder()
            .command(
                java, *vmArgs.toTypedArray(),
                "-jar", jarFile.from(workingDir), ssacfgExtractName,
                "-a", androidLib.from(workingDir), "-i", apk.from(workingDir), *ssacfgExtractArgs.toTypedArray()
            )
            .directory(File(workingDir).resolve("ssa-cfg-extract"))
            .inheritIO()
    }
}

private fun String.from(dir: String): String = File(dir).resolve(this).absolutePath