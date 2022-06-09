plugins {
    common
}

codeCoverage {
    enabled = false
}

dependencies {
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    testImplementation("org.mockito:mockito-core:4.6.1")
}
