plugins {
    id("kotlin")
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    implementation(gradleApi())
    implementation(project(":gradle:process"))
    implementation(project(":gradle:ci-logger"))
    implementation(project(":gradle:android"))
    implementation(project(":gradle:git"))
    implementation(project(":gradle:kotlin-dsl-support"))
    implementation(Dependencies.antPattern)
    implementation(Dependencies.Gradle.kotlinPlugin)
    implementation(Dependencies.funktionaleTry)

    testImplementation(project(":gradle:test-project"))
    testImplementation(project(":gradle:logging-test-fixtures"))
    testImplementation(project(":gradle:git-test-fixtures"))
    testImplementation(Dependencies.Test.mockitoKotlin)
}
