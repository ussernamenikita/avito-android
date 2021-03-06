package com.avito.instrumentation.executing

import com.avito.instrumentation.report.listener.TestReporter
import com.avito.instrumentation.reservation.devices.provider.DevicesProvider
import com.avito.instrumentation.reservation.request.Reservation
import com.avito.instrumentation.suite.model.TestWithTarget
import com.avito.runner.scheduler.TestsRunnerClient
import com.avito.runner.scheduler.args.Arguments
import com.avito.runner.scheduler.runner.model.TestRunRequest
import com.avito.runner.service.model.TestCase
import com.avito.runner.service.worker.device.Device
import com.avito.runner.service.worker.device.model.DeviceConfiguration
import com.avito.utils.logging.CILogger
import com.avito.utils.logging.commonLogger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import java.io.File

interface TestExecutor {

    fun execute(
        application: File?,
        testApplication: File,
        testsToRun: List<TestWithTarget>,
        executionParameters: ExecutionParameters,
        output: File
    )

    data class RunType(val id: String)

    class Impl(
        private val devicesProvider: DevicesProvider,
        private val testReporter: TestReporter,
        private val configurationName: String,
        private val logger: CILogger
    ) : TestExecutor {

        private val runner = TestsRunnerClient()

        //todo hi Juno!
        private val outputDirectoryName = "composer"

        override fun execute(
            application: File?,
            testApplication: File,
            testsToRun: List<TestWithTarget>,
            executionParameters: ExecutionParameters,
            output: File
        ) {
            withDevices(
                reservations = reservations(testsToRun)
            ) { devices ->
                val testRequests = testsToRun
                    .map { targetTestRun ->
                        val reservation = targetTestRun.target.reservation

                        val quota = reservation.quota

                        TestRunRequest(
                            testCase = TestCase(
                                className = targetTestRun.test.name.className,
                                methodName = targetTestRun.test.name.methodName,
                                deviceName = targetTestRun.target.deviceName
                            ),
                            configuration = DeviceConfiguration(
                                api = reservation.device.api,
                                model = reservation.device.model
                            ),
                            scheduling = TestRunRequest.Scheduling(
                                retryCount = quota.retryCount,
                                minimumFailedCount = quota.minimumFailedCount,
                                minimumSuccessCount = quota.minimumSuccessCount
                            ),
                            application = application?.absolutePath,
                            applicationPackage = executionParameters.applicationPackageName,
                            testApplication = testApplication.absolutePath,
                            testPackage = executionParameters.applicationTestPackageName,
                            testRunner = executionParameters.testRunner,
                            timeoutMinutes = TEST_TIMEOUT_MINUTES,
                            instrumentationParameters = targetTestRun.target.instrumentationParams,
                            enableDeviceDebug = executionParameters.enableDeviceDebug
                        )
                    }

                val runnerArguments = Arguments(
                    outputDirectory = outputFolder(output),
                    devices = devices,
                    logger = commonLogger(logger),
                    listener = testReporter,
                    requests = testRequests,
                    reservation = devicesProvider
                )

                logger.debug("Arguments: $runnerArguments")

                runner.run(arguments = runnerArguments)
            }

            logger.debug("Worker completed")
        }

        private fun outputFolder(output: File): File = File(
            output,
            outputDirectoryName
        ).apply { mkdirs() }

        private fun reservations(
            tests: List<TestWithTarget>
        ): Collection<Reservation.Data> {

            val testsGroupedByTargets: Map<TargetGroup, List<TestWithTarget>> = tests.groupBy {
                TargetGroup(
                    name = it.target.name,
                    reservation = it.target.reservation
                )
            }

            return testsGroupedByTargets
                .map { (target, tests) ->
                    val reservation = target.reservation.data(
                        tests = tests.map { it.test.name }
                    )

                    logger.info(
                        "Devices: ${reservation.count} devices will be allocated for " +
                            "target: ${target.name} inside configuration: $configurationName"
                    )

                    reservation
                }
        }

        // TODO: extract and delegate this channels orchestration.
        // It's overcomplicated for local client
        private fun withDevices(
            reservations: Collection<Reservation.Data>,
            action: (devices: ReceiveChannel<Device>) -> Unit
        ) {
            runBlocking {
                try {
                    logger.info("Devices: Starting action job for configuration: $configurationName...")
                    action(devicesProvider.provideFor(reservations, this))
                    logger.info("Devices: Action completed for configuration: $configurationName")
                } catch (e: Throwable) {
                    logger.critical("Error during action in $configurationName job", e)
                } finally {
                    logger.info("Devices: Starting releasing devices for configuration: $configurationName...")
                    devicesProvider.releaseDevices()
                    logger.info("Devices: Devices released for configuration: $configurationName")
                }
            }
        }

        data class TargetGroup(val name: String, val reservation: Reservation)
    }
}

private const val TEST_TIMEOUT_MINUTES = 5L
