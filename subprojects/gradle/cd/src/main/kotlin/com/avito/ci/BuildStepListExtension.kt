package com.avito.ci

import com.avito.ci.steps.ArtifactsConfiguration
import com.avito.ci.steps.BuildStep
import com.avito.ci.steps.CompileUiTests
import com.avito.ci.steps.ConfigurationCheck
import com.avito.ci.steps.DeployStep
import com.avito.ci.steps.ImpactAnalysisAwareBuildStep
import com.avito.ci.steps.LintCheck
import com.avito.ci.steps.PerformanceTestCheck
import com.avito.ci.steps.UiTestCheck
import com.avito.ci.steps.UnitTestCheck
import com.avito.ci.steps.UploadBuildResult
import com.avito.ci.steps.UploadToArtifactory
import com.avito.ci.steps.UploadToProsector
import com.avito.ci.steps.UploadToQapps
import com.avito.ci.steps.VerifyArtifactsStep
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty

open class BuildStepListExtension(private val name: String, objects: ObjectFactory) {

    private val artifactsConfig = ArtifactsConfiguration()

    internal val steps: ListProperty<BuildStep> = objects.listProperty(BuildStep::class.java).empty()

    var useImpactAnalysis: Boolean = false

    fun configuration(action: Action<ConfigurationCheck>) {
        configureAndAdd(ConfigurationCheck(name), action)
    }

    fun uiTests(action: Action<UiTestCheck>) {
        configureAndAdd(UiTestCheck(name), action)
    }

    fun performanceTests(action: Action<PerformanceTestCheck>) {
        configureAndAdd(PerformanceTestCheck(name), action)
    }

    fun compileUiTests(action: Action<CompileUiTests>) {
        configureAndAdd(CompileUiTests(name), action)
    }

    fun unitTests(action: Action<UnitTestCheck>) {
        configureAndAdd(UnitTestCheck(name), action)
    }

    fun lint(action: Action<LintCheck>) {
        configureAndAdd(LintCheck(name), action)
    }

    fun uploadToQapps(action: Action<UploadToQapps>) {
        configureAndAdd(UploadToQapps(name, artifactsConfig), action)
    }

    fun uploadToArtifactory(action: Action<UploadToArtifactory>) {
        configureAndAdd(UploadToArtifactory(name, artifactsConfig), action)
    }

    fun uploadToProsector(action: Action<UploadToProsector>) {
        configureAndAdd(UploadToProsector(name, artifactsConfig), action)
    }

    fun uploadBuildResult(action: Action<UploadBuildResult>) {
        configureAndAdd(UploadBuildResult(name), action)
    }

    fun deploy(action: Action<DeployStep>) {
        configureAndAdd(DeployStep(name, artifactsConfig), action)
    }

    fun artifacts(action: Action<ArtifactsConfiguration>) {
        val step = VerifyArtifactsStep(name, artifactsConfig)
        steps.add(step)
        action.execute(artifactsConfig)
        step.useImpactAnalysis = this.useImpactAnalysis
    }

    private fun <T : BuildStep> configureAndAdd(step: T, action: Action<T>) {
        action.execute(step)
        if (step is ImpactAnalysisAwareBuildStep) {
            step.useImpactAnalysis = this.useImpactAnalysis
        }
        steps.add(step)
    }
}
