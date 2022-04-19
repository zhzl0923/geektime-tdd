plugins {
    java
    id("code-coverage")
    id("code-style")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.puppycrawl.tools:checkstyle:10.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

group = "geektime.tdd"
version = "1.0-SNAPSHOT"

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}