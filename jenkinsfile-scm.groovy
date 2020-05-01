/*

This jenkinsfile checks out influxdb at a particular commit sha using the generic
scm step.

 */

podTemplate = "dind-1-12"
dindContainer = "dind"

def checkout() {
  checkout scm: [$class: 'GitSCM', 
    branches: [[name: "${SHA}"]],
    userRemoteConfigs: [[url: 'https://github.com/influxdata/influxdb.git']],
    quietOperation: true,
  ]
}

pipeline {
  agent {
    node {
      label "${podTemplate}"
    }
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '2'))
  }

  parameters {
    string(
      name: 'SHA',
      description: 'GIT SHA from InfluxDB'
    )
  }

  stages {
    stage('stash a real thing') {
      steps {
        container(dindContainer) {
          script {
            checkout()
          }

          sh 'git log -n 1'
        }
      }
    }
  }
}
