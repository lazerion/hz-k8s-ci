pipeline {
    agent {
        label "k8s"
    }

    parameters {
        string(name: 'NAME', defaultValue: 'hz-k8s', description: 'Image name')
        string(name: 'VERSION', defaultValue: '3.9', description: 'Image version')
        string(name: 'SLEEP', defaultValue: '10', description: 'Wait time for Hazelcast STARTED')
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
                git branch: 'kubernetes-support', changelog: false, poll: false, url: 'https://github.com/googlielmo/hazelcast-docker.git'
                dir('hazelcast-kubernetes') {
                    script {
                        oss = docker.build("${params.NAME}:${params.VERSION}")
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                git changelog: false, poll: false, url: 'https://github.com/lazerion/hz-k8s-ci.git'
                sh "kubectl apply -f config.yaml"
                sh "kubectl apply -f fabric8.yaml"
                sh "kubectl create -f deployment.yaml"
                // TODO find a proper way to wait
                sleep 10
                sh "kubectl get deployments"
                sh "kubectl get pods --show-labels"
                sh "kubectl delete -f deployment.yaml"
            }
        }
    }

    post {
        always {
            cleanWs deleteDirs: true
            retry(3){
                script {
                    sleep 5
                    sh "docker rmi ${oss.id}"
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
