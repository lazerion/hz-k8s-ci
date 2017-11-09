pipeline {
    agent {
        label "k8s"
    }

    options {
        timestamps()
    }

    stages {
        stage('Check K8S') {
            steps {
                sh "kubectl version"
                sh "kubectl get services"
            }
        }
    }

    post {
        always {
            cleanWs deleteDirs: true
        }
        failure {
            mail to: 'baris@hazelcast.com',
                    subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                    body: "Something is wrong with ${env.BUILD_URL}"
        }
    }
}
