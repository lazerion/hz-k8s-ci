pipeline {
    agent {
        label "k8s"
    }

    parameters {
        string(name: 'NAME', defaultValue: 'hz-k8s', description: 'Image name')
        string(name: 'VERSION', defaultValue: '3.9', description: 'Image version')
        string(name: 'SLEEP', defaultValue: '10', description: 'Wait time for Hazelcast STARTED')
        string(name: 'CLIENT', defaultValue: 'client', description: 'client image for acceptance')
        string(name: 'CLIENT_VERSION', defaultValue: '1', description: 'client image version')
    }

    options {
        timestamps()
    }

    stages {
        stage('Check k8s') {
            steps {
                sh "kubectl version"
            }
        }

        stage('Build') {
            steps {
                git branch: 'master', changelog: false, poll: false, url: 'https://github.com/hazelcast/hazelcast-docker.git'
                dir('hazelcast-kubernetes') {
                    script {
                        oss = docker.build("${params.NAME}:${params.VERSION}")
                    }
                }

                git changelog: false, poll: false, url: 'https://github.com/lazerion/hz-k8s-ci.git'
                script{
                    client = docker.build("${params.CLIENT}:${params.CLIENT_VERSION}")
                }

            }
        }

        stage('Deploy') {
            steps {
                git changelog: false, poll: false, url: 'https://github.com/lazerion/hz-k8s-ci.git'

                sh "kubectl apply -f config.yaml"
                sh "kubectl apply -f fabric8.yaml"
                sh "kubectl create -f deployment.yaml"
            }
        }

        stage('Acceptance'){
            steps{
                sh "kubectl run -i --image=${params.CLIENT}:${params.CLIENT_VERSION} client-app --port=5701 --quiet=true --restart=Never --rm=true --namespace=default"
            }
        }
    }

    post {
        always {
            sh "kubectl delete -f deployment.yaml || true"
            sh "kubectl delete -f config.yaml || true"
            sh "kubectl delete -f fabric8.yaml || true"

            cleanWs deleteDirs: true
            retry(3){
                script {
                    sleep 5
                    sh "docker rmi ${oss.id}"
                }
            }
            retry(3){
                script {
                    sleep 5
                    sh "docker rmi ${client.id}"
                }
            }
        }
        failure {
            mail to: 'baris@hazelcast.com',
                    subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                    body: "Something is wrong with ${env.BUILD_URL}"
        }
    }
}
