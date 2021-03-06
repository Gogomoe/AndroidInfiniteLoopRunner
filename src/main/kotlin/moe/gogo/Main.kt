package moe.gogo

import moe.gogo.jfr.MemoryRecordReader
import moe.gogo.report.ReportGenerator
import moe.gogo.report.Result
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
    Main(args).run()
}

class Main(private val args: Array<String>) {

    private val config: Config = Config.from(args, File("config.json"))
    private val root: File = File(config.workingDir)
    private val output: File = outputDir(root, config.outputDir).also { it.mkdirs() }

    fun run() {

        println("========== run ==========")
        val ssacfgExtractProcess = ssacfgExtractProcessBuilder()
        println(ssacfgExtractProcess.command())
        val ssacfgExtractTime = calcTime {
            ssacfgExtractProcess.start().waitFor()
        }

        println("========== summary ==========")
        val result = Result(
            config,
            root,
            output,
            ssacfgExtractTime,
            MemoryRecordReader(output.resolve("recording.jfr")).load()
        )
        ReportGenerator(result).generate()
    }

    private fun outputDir(root: File, outputDir: String): File {
        val formatter = listOf<Pair<Regex, (MatchResult) -> String>>(
            """\$\{Date:(.*?)}""".toRegex() to { result ->
                val pattern = result.groupValues[1]
                DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now())
            },
            """\$\{apk}""".toRegex() to { result ->
                root.resolve(config.apk).name
            }
        )

        var dirname = outputDir
        formatter.forEach { (matcher, transform) ->
            dirname = matcher.replace(dirname, transform)
        }

        return root.resolve(dirname)
    }

    private fun ssacfgExtractProcessBuilder(): ProcessBuilder {
        return with(config) {
            ProcessBuilder()
                .command(
                    java, *vmArgs.toTypedArray(),
                    "-jar", rootTo(jarFile),
                    "-a", rootTo(androidLib), "-i", rootTo(apk),
                    *args.toTypedArray()
                )
                .directory(output)
                .inheritIO()
        }
    }

    private fun calcTime(block: () -> Unit): Duration {
        val start = Instant.now()
        block()
        val end = Instant.now()
        return Duration.between(start, end)
    }

    private fun rootTo(path: String) = root.resolve(path).absolutePath

}