#!/usr/bin/groovy
def call(Map parameters = [:], body) {

    def defaultLabel = "clients.${env.JOB_NAME}.${env.BUILD_NUMBER}".replace('-', '_').replace('/', '_')
    def label = parameters.get('label', defaultLabel)

    def clientsImage = parameters.get('clientsImage', 'docker-repo.gaojihealth.cn:80/service-base-image/jenkins-slave-nodejs-mobile:v2')
    def inheritFrom = parameters.get('inheritFrom', 'base')

    def flow = new io.fabric8.Fabric8Commands()

    if (flow.isOpenShift()) {
        echo 'Runnning on openshift so using S2I binary source and Docker strategy'
        podTemplate(label: label, serviceAccount: 'jenkins', inheritFrom: "${inheritFrom}",
                containers: [[name: 'clients', image: "${clientsImage}", command: 'cat', ttyEnabled: true, envVars: [[key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/']]]],
                volumes: [
                        secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                        secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken')]) {
            body()
        }
    } else {
        echo 'Mounting docker socket to build docker images'
        podTemplate(label: label, serviceAccount: 'jenkins', inheritFrom: "${inheritFrom}",
                containers: [[name: 'clients', image: "${clientsImage}", command: 'cat', privileged: true, ttyEnabled: true, envVars: [envVar(key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/')]]],
                volumes: [
                        //secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                        secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                        persistentVolumeClaim(claimName: 'git-ssh-key', mountPath: '/root/.ssh', readOnly: true),
                        persistentVolumeClaim(claimName: 'docker-config', mountPath: '/home/jenkins/.docker', readOnly: true),
                        hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')],
                envVars: [envVar(key: 'DOCKER_HOST', value: 'unix:///var/run/docker.sock'), envVar(key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/')],
                imagePullSecrets: ['registry-key-secret-1']) {
            body()
        }
    }

}
