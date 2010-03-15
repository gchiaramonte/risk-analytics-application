includeTargets << grailsScript("_GrailsWar")

stagingDir = null

target(jarMain: '''
Compiles a grails application and moves the output to the specified directory and optionally creates
a JAR file or a runnable .cmd file.
The directory is structured in a way that a grails application can locally be run without
the need for a grails executable. Web Content (i.e. views) is not completely included.

Options:
-destination (required): target directory for the compiled output
-buildJar (optional): Creates a jar file of the project
-buildClasses (optional): Creates a directory which contains all output and a .cmd file to start the project
-mainClass (required for buildJar and buildClasses): main class to start
''') {
    depends(parseArguments, packageApp)

    stagingDir = argsMap.destination
    if (!stagingDir) {
        throw new RuntimeException("no destination dir set")
    }

    ant.mkdir(dir: stagingDir)

    //copy applicationContext.xml from the project to the target directory
    ant.copy(todir: stagingDir, overwrite: true) {
        fileset(dir: "${basedir}/web-app/WEB-INF", includes: "applicationContext.xml")
    }

    //copy the compiler output to the target directory
    ant.copy(todir: "${stagingDir}") {
        fileset(dir: classesDirPath) {
            exclude(name: "hibernate")
            exclude(name: "spring")
            exclude(name: "hibernate/*")
            exclude(name: "spring/*")
        }
        fileset(dir: resourcesDirPath, includes: "**/*")
    }

    //Create a grails.xml file which is necessary to create a plugin manager (using _GrailsWar.groovy)
    ant.mkdir(dir: "${stagingDir}/WEB-INF")
    createDescriptor()

    //Project ready to run in stagingDir, optionally create a jar or cmd file to run the application
    if (argsMap.buildJar) {
        buildJar()
    }
    if (argsMap.buildClasses) {
        createRunnableFiles()
    }

}

target(createStarterFile: '''
Creates starter files
''') {
    String mainClass = argsMap.mainClass
    String env = argsMap.env

    String target = "${projectTargetDir}/runner"
    List classPath = getRelativeClassPaths("$target/lib")
    String jvmArgs = "-Xmx1024m -XX:MaxPermSize=512m -Dgrails.env=$env"

    String cmd = "java $jvmArgs -classpath \"./classes;${classPath.join(";")}\" $mainClass"
    ant.echo(file: "${target}/Run${grailsAppName}${env}.cmd", message: cmd.toString())
    ant.echo(file: "${target}/Run${grailsAppName}${env}.sh", message: cmd.toString())
}

//Creates an executable jar file including manifest
private void buildJar() {
    String mainClass = argsMap.mainClass
    if (!mainClass) throw new RuntimeException("no main class set")

    String jarTarget = "${projectTargetDir}/jar"
    ant.mkdir(dir: jarTarget)

    ant.delete(includeemptydirs: "true") {
        fileset(dir: jarTarget, includes: "**/*")
    }

    copyLibraries(jarTarget)
    List classPath = getRelativeClassPaths("$jarTarget/lib")

    String manifestTarget = "${jarTarget}/MANIFEST.MF"
    ant.manifest(file: manifestTarget) {
        attribute(name: "Main-Class", value: mainClass)
        attribute(name: "Class-Path", value: classPath.join(" "))
        section(name: "Grails Application") {
            attribute(name: "Implementation-Title", value: "${grailsAppName}")
            attribute(name: "Implementation-Version", value: "${metadata.getApplicationVersion()}")
            attribute(name: "Grails-Version", value: "${metadata.getGrailsVersion()}")
        }
    }
    ant.jar(destfile: "${jarTarget}/${grailsAppName}.jar", basedir: stagingDir, manifest: manifestTarget)
    ant.delete(file: manifestTarget)
}

//Copies all classes & resources to a directory and creates a .cmd file which starts the appplication
private void createRunnableFiles() {
    String mainClass = argsMap.mainClass
    if (!mainClass) throw new RuntimeException("no main class set")

    String target = "${projectTargetDir}/runner"
    ant.mkdir(dir: target)

    ant.delete(includeemptydirs: "true") {
        fileset(dir: target, includes: "**/*")
    }

    copyLibraries(target)
    ant.copy(todir: "${target}/classes") {
        fileset(dir: stagingDir)
    }
}

//Copies all external libraries required by this project (inclusive plugin & grails libs) to the target dir
private void copyLibraries(String target) {

    def externalLibsTarget = "${target}/lib"
    ant.mkdir(dir: externalLibsTarget)
    ant.copy(todir: externalLibsTarget, flatten: true) {
        fileset(dir: './lib', includes: '*.jar')
        fileset(dir: pluginsDirPath, includes: '**/lib/*.jar')
        fileset(dir: grailsHome, includes: 'lib/*.jar dist/*.jar')
    }
}

//Returns a list of all relative paths (may be used for building classpaths
private List getRelativeClassPaths(String externalLibsTarget) {
    List classPath = []
    new File(externalLibsTarget).eachFileRecurse { File file ->
        classPath << "lib/${file.name}"
    }
    return classPath
}

setDefaultTarget('jarMain')