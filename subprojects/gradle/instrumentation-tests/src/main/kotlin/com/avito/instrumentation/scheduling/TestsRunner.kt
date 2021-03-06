package com.avito.instrumentation.scheduling

import com.avito.instrumentation.configuration.InstrumentationConfiguration
import com.avito.instrumentation.executing.ExecutionParameters
import com.avito.instrumentation.executing.TestExecutor
import com.avito.instrumentation.executing.TestExecutorFactory
import com.avito.instrumentation.report.Report
import com.avito.instrumentation.report.listener.TestReporter
import com.avito.instrumentation.reservation.devices.provider.DevicesProviderFactory
import com.avito.instrumentation.suite.model.TestWithTarget
import com.avito.instrumentation.suite.model.transformTestsWithNewJobSlug
import com.avito.report.model.ReportCoordinates
import com.avito.report.model.SimpleRunTest
import com.avito.report.model.TestStaticData
import com.avito.runner.service.model.TestCase
import com.avito.utils.gradle.KubernetesCredentials
import com.avito.utils.logging.CILogger
import org.funktionale.tries.Try
import java.io.File
import java.nio.file.Files

interface TestsRunner {

    fun runTests(
        mainApk: File?,
        testApk: File,
        runType: TestExecutor.RunType,
        reportCoordinates: ReportCoordinates,
        report: Report,
        testsToRun: List<TestWithTarget>
    ): Try<List<SimpleRunTest>>
}

class TestsRunnerImplementation(
    private val testExecutorFactory: TestExecutorFactory,
    private val kubernetesCredentials: KubernetesCredentials,
    private val testReporterFactory: (Map<TestCase, TestStaticData>, File, Report) -> TestReporter,
    private val logger: CILogger,
    private val buildId: String,
    private val buildType: String,
    private val projectName: String,
    private val executionParameters: ExecutionParameters,
    private val outputDirectory: File,
    private val instrumentationConfiguration: InstrumentationConfiguration.Data,
    private val registry: String
) : TestsRunner {

    override fun runTests(
        mainApk: File?,
        testApk: File,
        runType: TestExecutor.RunType, // todo delete runtype
        reportCoordinates: ReportCoordinates,
        report: Report,
        testsToRun: List<TestWithTarget>
    ): Try<List<SimpleRunTest>> {
        return if (testsToRun.isEmpty()) {
            Try.Success(emptyList())
        } else {

            val output = File(outputDirectory, runType.id).apply { mkdirs() }
            val logcatDir = Files.createTempDirectory(null).toFile()

            val testReporter = testReporterFactory.invoke(
                testsToRun.associate {
                    TestCase(
                        className = it.test.name.className,
                        methodName = it.test.name.methodName,
                        deviceName = it.target.deviceName
                    ) to it.test
                },
                logcatDir,
                report
            )
            val logger = logger.child(runType.id)
            // TODO: pass through constructor
            val initialRunConfiguration =
                instrumentationConfiguration.copy(name = "${instrumentationConfiguration.name}-${runType.id}")
            val executor = testExecutorFactory.createExecutor(
                devicesProviderFactory = DevicesProviderFactory.Impl(
                    kubernetesCredentials = kubernetesCredentials,
                    buildId = buildId,
                    buildType = buildType,
                    projectName = projectName,
                    registry = registry,
                    output = output,
                    logcatDir = logcatDir,
                    logger = logger
                ),
                configuration = initialRunConfiguration,
                executionParameters = executionParameters,
                testReporter = testReporter,
                logger = logger
            )

            executor.execute(
                application = mainApk,
                testApplication = testApk,
                testsToRun = testsToRun.transformTestsWithNewJobSlug(reportCoordinates.jobSlug),
                executionParameters = executionParameters,
                output = output
            )

            //todo через Report
            val raw = report.getTests()

            log("test results: $raw")

            val filtered = raw.map { runs ->
                runs.filterNotRelatedRunsToThisInstrumentation(testsToRun)
            }

            log("filtered results: $filtered")

            filtered
        }
    }

    private fun List<SimpleRunTest>.filterNotRelatedRunsToThisInstrumentation(
        testsToRun: List<TestWithTarget>
    ): List<SimpleRunTest> {
        return filter { run -> run.isRelatedTo(testsToRun) }
    }

    private fun SimpleRunTest.isRelatedTo(testsToRun: List<TestWithTarget>): Boolean {
        return testsToRun.any { testWithTarget ->
            testWithTarget.test.name.name == name && testWithTarget.target.deviceName == deviceName
        }
    }

    private fun log(message: String) {
        logger.debug("TestsRunner: $message")
    }
}
