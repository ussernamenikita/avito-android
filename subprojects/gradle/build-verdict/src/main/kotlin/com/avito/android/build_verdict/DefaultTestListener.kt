package com.avito.android.build_verdict

import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

abstract class DefaultTestListener : TestListener {
    override fun beforeSuite(suite: TestDescriptor) {
        // empty
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        // empty
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {
        // empty
    }

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        // empty
    }
}
