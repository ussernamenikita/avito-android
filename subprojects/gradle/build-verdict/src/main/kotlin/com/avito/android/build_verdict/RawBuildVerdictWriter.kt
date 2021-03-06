package com.avito.android.build_verdict

import com.avito.utils.logging.CILogger
import com.google.gson.Gson
import java.io.File

internal class RawBuildVerdictWriter(
    private val gson: Gson,
    private val buildVerdictDir: File,
    private val logger: CILogger
) : BuildVerdictWriter {
    override fun write(buildVerdict: BuildVerdict) {
        val verdict = gson.toJson(
            buildVerdict
        )
        val dir = buildVerdictDir.apply { mkdirs() }
        val file = File(dir, buildVerdictFileName)
        file.createNewFile()
        file.writeText(
            verdict
        )
        logger.warn("Raw build verdict at $file")
    }

    companion object {
        val buildVerdictFileName = "raw-build-verdict.json"
    }
}
