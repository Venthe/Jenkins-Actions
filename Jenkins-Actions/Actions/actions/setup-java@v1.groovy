#!groovy
def uses = actionContext.get('uses')
def distribution = actionContext.get('with').get('distribution')
def javaVersion = actionContext.get('with').get('java-version')

dir("${env.TOOL_TEMP}/${uses}") {
    if (!distribution.equalsIgnoreCase("temurin")) {
        return [status: "not_ok"]
    }

    if (!javaVersion.equalsIgnoreCase("17")) {
        return [status: "not_ok"]
    }

    def JAVA_HOME = "${env.TOOL_TEMP}/lib/java"
    script {
        sh """\
curl -s --location --remote-name https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.3%2B7/OpenJDK17U-jre_x64_linux_hotspot_17.0.3_7.tar.gz
mkdir -p ${env.TOOL_TEMP}/lib/java
tar -xzf OpenJDK17U-jre_x64_linux_hotspot_17.0.3_7.tar.gz --directory ${env.TOOL_TEMP}/lib/java --strip-components=1
rm OpenJDK17U-jre_x64_linux_hotspot_17.0.3_7.tar.gz
echo "PATH=${JAVA_HOME}/bin:${env.PATH}" >> /etc/environment
echo "JAVA_HOME=${JAVA_HOME}" >> /etc/environment
export PATH=":${env.PATH}"
"""
        env.ADDITIONAL_PATH + "${JAVA_HOME}/bin"
        env.ADDITIONAL_ENV_VARIABLES["JAVA_HOME"] = "${JAVA_HOME}/bin"
    }
}

return [defaultRun: "", status: "ok"]
