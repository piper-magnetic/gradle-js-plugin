package com.eriwen.gradle.js

import org.gradle.api.Project
import org.gradle.process.internal.ExecException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class JsHintTaskTest extends Specification {
    @Rule TemporaryFolder dir = new TemporaryFolder()

    Project project = ProjectBuilder.builder().build()
    def task
    def src
    def dest

    def setup() {
        dir.create()
        project.apply(plugin: JsPlugin)
        project.repositories.mavenCentral()
        task = project.tasks.jshint
        src = dir.newFolder()
        dest = dir.newFile()
        task.source = src
        task.dest = dest
    }

    def "build ignores result by default"() {
        given:
        addValidFile()
        addInvalidFile()

        when:
        task.run()

        then:
        notThrown ExecException
    }

    def "build passes with only valid files"() {
        given:
        task.ignoreExitCode = false
        addValidFile()

        when:
        task.run()

        then:
        notThrown ExecException
    }

    def "build fails with invalid files"() {
        given:
        task.ignoreExitCode = false
        project.jshint.options = [asi: "true"]
        addValidFile()
        addInvalidFile()

        when:
        task.run()

        then:
        ExecException e = thrown()
    }

    def "build writes to stdout and accepts options"() {
        given:
        task.ignoreExitCode = false
        task.outputToStdOut = true
        project.jshint.options = [scripturl: "true", laxcomma: "true"]

        addValidFile()

        when:
        task.run()

        then:
        notThrown ExecException
    }

    def "jshint processes many files"() {
        given:
        task.ignoreExitCode = false
        addFile("valid.js", "var a = 5;")
        addFile("valid2.js", "var b = 5;")
        addFile("valid3.js", "var c = 5;")
        addFile("valid4.js", "var d = 5;")

        when:
        task.run()

        then:
        notThrown ExecException
    }

    def "fails without predef option to jshint"() {
        given:
        task.ignoreExitCode = false
        project.jshint.options = [ undef: "true" ]
        project.jshint.predef = [ someGlobalTwo: 5 ]
        addFile("invalidWithGlobal.js", "var b = someGlobal;")

        when:
        task.run()

        then:
        thrown ExecException
    }

    def "passes with predef option to jshint"() {
        given:
        task.ignoreExitCode = false
        project.jshint.options = [ undef: "true" ]
        project.jshint.predef = [ someGlobal: 5 ]
        addFile("validWithGlobal.js", "var b = someGlobal;")

        when:
        task.run()

        then:
        notThrown ExecException
    }


    def "fails without ES6 option and template literals"() {
        given:
        task.ignoreExitCode = false
        task.outputToStdOut = true
        project.jshint.options = [ undef: "true", "esversion": 3]
        project.jshint.predef = [ someGlobalTwo: 5 ]
        addFile("invalidLiterals.js", "let b = `test`;")

        when:
        task.run()

        then:
        thrown ExecException
    }

    def "passes with ES6 option and template literals"() {
        given:
        task.ignoreExitCode = false
        project.jshint.options = [ undef: "true", "esversion": 6 ]
        addFile("validLiterals.js", "let b = `test`;")

        when:
        task.run()

        then:
        notThrown ExecException
    }

    def addValidFile() {
        addFile("valid.js", "var a = 5;")
    }

    def addInvalidFile() {
        // no semicolon, jshint should fail
        addFile("invalid.js", "var adfdesf9()0009)000(;ldfsd9f) = 5")
    }

    def addFile(name,contents) {
        def file = new File(src as String, name)
        file << contents
    }
}


