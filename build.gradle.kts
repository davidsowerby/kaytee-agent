import ratpack.gradle.*
import com.github.jengelman.gradle.plugins.shadow.*
import org.gradle.api.*
import org.gradle.plugins.ide.idea.*
import org.gradle.plugins.ide.idea.model.*
import org.gradle.api.tasks.wrapper.Wrapper

buildscript {
    repositories {
        jcenter()
        gradleScriptKotlin()
    }

    dependencies {
        classpath("io.ratpack:ratpack-gradle:1.4.1")
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(kotlinModule("gradle-plugin"))
    }
}

if (!JavaVersion.current().isJava8Compatible()) {
    throw IllegalStateException("Must be built with Java 8 or higher")
}

val toolingApiVersion = gradle.gradleVersion

apply {
    plugin<RatpackPlugin>()
    plugin<ShadowPlugin>()
    plugin("groovy")
    plugin("kotlin")
    plugin<IdeaPlugin>()
    from("$rootDir/gradle/idea.gradle")
}

repositories {
    mavenLocal()
    jcenter()
    gradleScriptKotlin()
}

dependencies {
    compile(kotlinModule("stdlib"))
    project.extensions.getByType(RatpackExtension::class.java).let { ratpack ->
        compile(ratpack.dependency("guice"))
    }
    project.extensions.getByType(RatpackExtension::class.java).let { ratpack ->
        testCompile(ratpack.dependency("test"))
    }
    //krail for I18N only.  Reduce when I18N moved out of Krail
    compile("uk.q3c.krail:krail:0.9.9")
    compile("uk.q3c.simplycd:simplycd-lifecycle:0.7.3.19")
    //gradle tooling api
    compile("org.gradle:gradle-tooling-api:" + toolingApiVersion)
    compile("uk.q3c.rest:ion-json:0.0.1.5")
    compile("me.drmaas:ratpack-kotlin-dsl:0.5.0")


    runtime("org.apache.logging.log4j:log4j-slf4j-impl:2.6.1")
    runtime("org.apache.logging.log4j:log4j-api:2.6.1")
    runtime("org.apache.logging.log4j:log4j-core:2.6.1")
    runtime("com.lmax:disruptor:3.3.4")
    testCompile("junit:junit:4.+")
    testCompile("org.jetbrains.spek:spek:1.0.+")
    testCompile("org.spockframework:spock-core:1.0-groovy-2.4")
    testCompile(kotlinModule("test"))
    testCompile("uk.q3c:q3c-testUtil:0.8.2")
}

configure<ApplicationPluginConvention> {
    mainClassName = "uk.q3c.simplycd.agent.app.Main"
}

task<Wrapper>("wrapper") {
    gradleVersion = "3.2.1"
}
