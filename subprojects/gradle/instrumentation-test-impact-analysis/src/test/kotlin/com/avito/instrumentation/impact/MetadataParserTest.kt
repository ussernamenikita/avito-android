package com.avito.instrumentation.impact

import com.avito.instrumentation.impact.metadata.MetadataParser
import com.avito.utils.logging.CILogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class MetadataParserTest {

    @Test
    fun test(@TempDir projectDir: File) {
        File(projectDir, "NewFile.kt").apply {
            writeText(
                """
                import com.test.pkg.R
                    
                class NewClass : Screen {
                
                    val rootId: Int = R.id.something_root
                
                }
            """.trimIndent()
            )
        }

        val result = MetadataParser(
            ciLogger = CILogger.allToStdout,
            screenClass = "com.test.Screen",
            fieldName = "rootId"
        ).parseMetadata(setOf(projectDir))

        println(result)
    }
}
