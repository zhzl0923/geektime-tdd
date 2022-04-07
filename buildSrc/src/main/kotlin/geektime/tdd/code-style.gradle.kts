plugins {
    checkstyle
}

open class StyleExtension {
    var excludeClasses = ArrayList<String>()
    var excludePackages = ArrayList<String>()
}

project.extensions.create("codeStyle", StyleExtension::class)


checkstyle {
    configFile = file(rootDir.path + "/gradle/config/checkstyle/google_checks.xml")
    toolVersion = properties["checkstyleVersion"].toString()
}

tasks.getByName<Checkstyle>("checkstyleMain") {
    doFirst {
        val styleExtension = this.project.extensions["codeStyle"] as StyleExtension
        styleExtension.excludeClasses.forEach {
            this@getByName.exclude("**/" + it.replace('.', '/') + ".java")
        }
        styleExtension.excludePackages.forEach {
            this@getByName.exclude("**/" + it.replace('.', '/') + "/*")
        }
    }
}

tasks.getByName<Checkstyle>("checkstyleTest") {
    enabled = false
}