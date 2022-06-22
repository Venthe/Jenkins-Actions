@Grapes([@Grab(group = 'com.fasterxml.jackson.core', module = 'jackson-databind', version = '2.13.3'),
        @Grab(group = 'com.fasterxml.jackson.dataformat', module = 'jackson-dataformat-yaml', version = '2.13.3'),
        @Grab(group = 'com.fasterxml.jackson.datatype', module = 'jackson-datatype-jsr310', version = '2.13.3'),
//        @Grab(group = 'com.jayway.jsonpath', module = 'json-path', version = '2.7.0'),
        @Grab(group = 'org.apache.logging.log4j', module = 'log4j-api', version = '2.17.2'),
        @Grab(group = 'org.apache.logging.log4j', module = 'log4j-core', version = '2.17.2'),
        @Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.11')])
@NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import groovy.transform.Field
import org.slf4j.LoggerFactory

//// region MockJenkins
//def sh(command) {
//    log.debug "[SHELL] $command"
//}
//
//def node(Closure c) {
//    c()
//}
//
//def stage(String name, Closure c) {
//    log.debug "[STAGE] $name"
//    c()
//}
//
//class Image {
//    def log
//
//    Image(name) {
//        log = LoggerFactory.getLogger(Docker.class)
//        ((Logger) log).setLevel(Level.DEBUG)
//        log.debug "[MOCK IMAGE] $name"
//    }
//
//    def inside(Closure c) {
//        c()
//    }
//}
//
//class Docker {
//    def log
//
//    Docker() {
//        log = LoggerFactory.getLogger(Docker.class)
//        ((Logger) log).setLevel(Level.DEBUG)
//    }
//
//    def image(val) {
//        log.debug "[MOCK DOCKER]"
//        return new Image(val)
//    }
//}
//
//def docker = new Docker()
//// endregion

@Field def mapper = new ObjectMapper(new YAMLFactory())
//mapper.findAndRegisterModules()
@Field def log = LoggerFactory.getLogger(this.class)
//((Logger) log).setLevel(Level.DEBUG)
@Field def params = [EVENT_NAME: "push", REF: "", REPOSITORY: "", WORKSPACE: ""]

node {
    docker.image("docker.io/library/alpine:latest").inside {

        env.ADDITIONAL_PATH = []
        env.ADDITIONAL_ENV_VARIABLES = [:]


        def intermediatePath = []
        println "intermediatePath " + intermediatePath
        println "env.ADDITIONAL_PATH.getClass() " + env.ADDITIONAL_PATH.getClass()
        println "env.PATH.getClass() " + env.PATH.getClass()
        println "intermediatePath.getClass() " + intermediatePath.getClass()
        println "env.PATH " + env.PATH
        intermediatePath.addAll(env.ADDITIONAL_PATH)
        intermediatePath.add(env.PATH)
        println "intermediatePath " + intermediatePath
        intermediatePath = intermediatePath.join(":")
        println "intermediatePath " + intermediatePath

        def finalResult = []
        finalResult << "PATH=${intermediatePath}"
        println finalResult
        finalResult << env.ADDITIONAL_ENV_VARIABLES.collect { "${it.key}=${it.value}" }
        finalResult = finalResult.flatten()
        println finalResult
        stage("Read script") {
            env.input = sh(script: "cat `pwd`/.jenkins/workflows/example.yml", returnStdout: true)
        }
    }
}

def input = env.input

def shouldHandle(events) {
   return events.collect { it.toLowerCase() }
           .contains(params.event_name)
}

def parseYaml(input) {
   def parsedJson = mapper.readTree(input)
   return mapper.convertValue(parsedJson, Map.class)
}

def provideName(name, uses) {
   if (!name && !uses) {
       return "Shell"
   }

   if (name && uses) {
       return "[$uses] $name"
   }

   if (name) {
       return "$name"
   } else if (uses) {
       return "$uses"
   }
}

def loadAction(name, context) {
//    def shell = new GroovyShell(this.class.getClassLoader(), getBinding())
//    shell.setVariable("context", context)
//    println this.dump()
//    def script = shell.parse(new File("${env.getEnvironment().get('JENKINS_HOME')}/Actions/${name}.groovy"))
//    def ss = script.run()

//    return ss.execute(System.out);
   getBinding().setVariable("actionContext", context)
   return evaluate(new File("${env.getEnvironment().get('JENKINS_HOME')}/Actions/${name}.groovy"))

}

def run(it) {
   def uses = it.get('uses')
   def run = it.get('run')
   if (uses) {
       def result = loadAction(uses, it)
       if (result) {
           println result
       }
   }
   def name = it.get('name')
   def description = it.get('description')

   if (run) {
       sh run
   }
}

def prepareEnvVariables() {
   def intermediatePath = []
   println "intermediatePath " + intermediatePath
   println "env.ADDITIONAL_PATH.getClass() " + env.ADDITIONAL_PATH.getClass()
   println "env.PATH.getClass() " + env.PATH.getClass()
   println "intermediatePath.getClass() " + intermediatePath.getClass()
   println "env.PATH " + env.PATH
   intermediatePath.addAll(env.ADDITIONAL_PATH)
   intermediatePath.add(env.PATH)
   println "intermediatePath " + intermediatePath
   intermediatePath = intermediatePath.join(":")
   println "intermediatePath " + intermediatePath

   def finalResult = []
   finalResult << "PATH=${intermediatePath}"
   println finalResult
   finalResult << env.ADDITIONAL_ENV_VARIABLES.collect { "${it.key}=${it.value}" }
   finalResult = finalResult.flatten()
   println finalResult
   return finalResult
}

def inputContext = parseYaml(input)

def on = inputContext.get('on')
if (!shouldHandle(on)) {
   log.debug("Event not handled {}", on)
}

def name = inputContext.get('name')
println name

inputContext.get('jobs').each { key, val ->
   node {
//        println env.getEnvironment().collect { "$it.key: $it.value" }.join('\n')
//        println params.collect { "$it.key: $it.value" }.join('\n')
       params.each { env.setProperty(it.key, it.value) }

       docker.image(val.get('runs-on')).inside {
           stage("$key") {
               env.TOOL_TEMP = "${env.WORKSPACE_TMP}/${env.BUILD_TAG}"
               val.get('steps').each {
                   withEnv(prepareEnvVariables()) {
                       def nm = provideName(it.get('name'), it.get('uses'))

                       def description = it.get('description')
                       if (description) {
                           nm += " - $description"
                       }
                       println nm
                       run it
                   }
               }
           }
       }
   }
}
