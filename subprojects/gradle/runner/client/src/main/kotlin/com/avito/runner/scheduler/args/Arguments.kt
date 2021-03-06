package com.avito.runner.scheduler.args

import com.avito.logger.Logger
import com.avito.runner.reservation.DeviceReservation
import com.avito.runner.scheduler.listener.TestLifecycleListener
import com.avito.runner.scheduler.runner.model.TestRunRequest
import com.avito.runner.service.worker.device.Device
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.File

class Arguments(
    val outputDirectory: File,
    val requests: List<TestRunRequest>,
    val devices: ReceiveChannel<Device>,
    val logger: Logger,
    val listener: TestLifecycleListener,
    val reservation: DeviceReservation
) {

    override fun toString(): String {
        return buildString {
            appendln(requests)
        }
    }
}
