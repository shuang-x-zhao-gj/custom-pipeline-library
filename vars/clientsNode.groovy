#!/usr/bin/groovy
def call(Map parameters = [:], body) {

    def defaultLabel = "clients.${env.JOB_NAME}.${env.BUILD_NUMBER}".replace('-', '_').replace('/', '_')
    def label = parameters.get('label', defaultLabel)

    clientsTemplate(parameters) {
        node(label) {
            body()
        }
    }
}
