package com.avito.android.test.report.model

import com.avito.filestorage.FutureValue
import com.avito.filestorage.RemoteStorage
import com.avito.report.model.Entry

/**
 * @param number step number; must be same as in test case
 * @param title in http://links.k.avito.ru/9O: "Step Action"; value must be the same (can be trimmed)
 */
data class StepResult(
    /**
     * Synthetic step is created by the framework. It has simpilier lifecycle then Step created by users
     * So we need that flag in the framework for making decisions:
     * - What we should do when we start a new step or precondition and current step isn't NULL?
     * If current step is Synthetic we will overwrite it.
     * If current step is Real we will fail because we try to create INTERNAL step
     */
    val isSynthetic: Boolean,
    var timestamp: Long? = null,
    var number: Int? = null,
    var title: String? = null,
    var entryList: MutableList<Entry> = mutableListOf(),
    val futureUploads: MutableList<FutureValue<RemoteStorage.Result>> = mutableListOf()
)
