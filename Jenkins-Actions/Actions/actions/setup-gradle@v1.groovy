#!groovy
def uses = actionContext.get('uses')
def version = actionContext.get('with').get('version')

dir("${env.TOOL_TEMP}/${uses}") {
    sh """\
apt-get update -qq
apt-get install --assume-yes -qqq unzip
curl --location --remote-name "https://downloads.gradle-dn.com/distributions/gradle-${version}-bin.zip"
mkdir -p "${env.TOOL_TEMP}/lib"
unzip -q "gradle-${version}-bin.zip" -d "${env.TOOL_TEMP}/lib"
rm gradle-${version}-bin.zip
"""
    env.ADDITIONAL_PATH + "${env.TOOL_TEMP}/lib/gradle-7.4.2/bin"
}

return [defaultRun: "", status: "ok"]
