properties([
    pipelineTriggers([cron('H 0 * * *')]),
])

tcmsHost = "https://tcms.pingcap.net/"

triggerCI = httpRequest url: tcmsHost + "api/v1/dailyci/trigger", httpMode: 'POST'
ciResp = readJSON text: triggerCI.content
id = ciResp["id"].toString()
ciFinished = false
ciDuration = 0

stage("Wait for completion") {
    while(!ciFinished) {
        sleep(300)
        ciDuration = ciDuration +300
        // ci breaks when timeout(23 hours)
        if (ciDuration > 82800) {
            break
        }
        statusCI = httpRequest tcmsHost + "api/v1/dailyci/trigger/" + id
        statusResp = readJSON text: statusCI.content
        ciFinished = statusResp["finished"].toBoolean()
    }
}

def response = httpRequest tcmsHost + "api/v1/plans/branchs"
def branches = readJSON text: response.content
for (b in branches) {
    branch= b.toString()
    node(GO_BUILD_SLAVE){
        container("golang"){
            stage("branch: "+ branch + " daily ci result") {
                sh """
                curl -o ${branch}.xml ${tcmsHost}api/v1/dailyci?started_at=${id}&branch=${branch}
                """
                junit testResults: "${branch}.xml"
            }
        }
    }
}
