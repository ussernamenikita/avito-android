package com.avito.runner.service

import com.avito.coroutines.extensions.Dispatchers
import com.avito.logger.Logger
import com.avito.runner.service.listener.TestListener
import com.avito.runner.service.model.intention.Intention
import com.avito.runner.service.model.intention.IntentionResult
import com.avito.runner.service.worker.DeviceWorker
import com.avito.runner.service.worker.DeviceWorkerMessage
import com.avito.runner.service.worker.device.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import java.io.File

class IntentionExecutionServiceImplementation(
    private val outputDirectory: File,
    private val logger: Logger,
    private val devices: ReceiveChannel<Device>,
    private val intentionsRouter: IntentionsRouter = IntentionsRouter(logger = logger),
    private val listener: TestListener,
    private val deviceWorkersDispatcher: Dispatchers = Dispatchers.SingleThread
) : IntentionExecutionService {

    private val intentions: Channel<Intention> =
        Channel(Channel.UNLIMITED)
    private val results: Channel<IntentionResult> =
        Channel(Channel.UNLIMITED)
    private val messages: Channel<DeviceWorkerMessage> =
        Channel(Channel.UNLIMITED)
    private val deviceSignals: Channel<Device.Signal> =
        Channel(Channel.UNLIMITED)

    override fun start(scope: CoroutineScope): IntentionExecutionService.Communication {
        scope.launch {
            launch {
                for (device in devices) {
                    DeviceWorker(
                        intentionsRouter = intentionsRouter,
                        device = device,
                        outputDirectory = outputDirectory,
                        logger = logger,
                        messagesChannel = messages,
                        listener = listener,
                        dispatchers = deviceWorkersDispatcher
                    ).run(scope)
                }
            }

            launch {
                for (intention in intentions) {
                    log("received intention: $intention")
                    intentionsRouter.sendIntention(intention = intention)
                }
            }

            launch {
                for (message in messages) {
                    log("received message: $message")
                    when (message) {
                        is DeviceWorkerMessage.ApplicationInstalled -> {
                            log("Application: ${message.installation.installation.application} installed")
                        }
                        is DeviceWorkerMessage.FailedIntentionProcessing -> {
                            log(
                                "Received worker failed message during executing intention:" +
                                    " ${message.intention}. Rescheduling..."
                            )

                            intentionsRouter.sendIntention(intention = message.intention)
                        }
                        is DeviceWorkerMessage.Result -> {
                            results.send(message.intentionResult)
                        }
                        is DeviceWorkerMessage.WorkerDied -> {
                            deviceSignals.send(Device.Signal.Died(message.coordinate))
                        }
                    }
                }
            }
        }

        return IntentionExecutionService.Communication(
            intentions = intentions,
            results = results,
            deviceSignals = deviceSignals
        )
    }

    override fun stop() {
        intentionsRouter.close()
        intentions.close()
        results.close()
        messages.close()
        devices.cancel()
        deviceSignals.close()
    }

    private fun log(message: String) {
        logger.debug("IntentionExecutionService: $message")
    }
}
