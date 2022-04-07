plugins {
    jacoco
}

jacoco.toolVersion = properties["jacocoVersion"] as String

open class CodeCoverageExtension {
    var enabled: Boolean = true
    var excludeClasses = ArrayList<String>()
    var excludePackages = ArrayList<String>()
}

project.extensions.create("codeCoverage", CodeCoverageExtension::class)


tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.withType<JacocoBase>())
}

tasks.withType<JacocoBase> {
    dependsOn(tasks.withType<Test>())
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(false)
        csv.required.set(false)
    }
}

tasks.withType<JacocoCoverageVerification> {
    afterEvaluate {
        val codeCoverage = this.extensions.getByName("codeCoverage") as CodeCoverageExtension
        val excludes = ArrayList<String>()
        codeCoverage.excludeClasses.forEach {
            excludes.add(it.replace('.', '/') + ".class")

        }
        codeCoverage.excludePackages.forEach {
            excludes.add(it.replace('.', '/') + "/*")

        }
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it).apply {
                this.setExcludes(excludes)
            }
        }))
        this@withType.enabled = codeCoverage.enabled
    }

    violationRules {
        val codeCoverage = project.extensions.getByName("codeCoverage") as CodeCoverageExtension
        rule {
            enabled = codeCoverage.enabled
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal(1.0)
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal(1.0)
            }
            limit {
                counter = "CLASS"
                value = "COVEREDRATIO"
                minimum = BigDecimal(1.0)
            }
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = BigDecimal(1.0)
            }
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = BigDecimal(1.0)
            }
        }
    }
}