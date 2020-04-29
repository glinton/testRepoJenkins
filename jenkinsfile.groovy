/*

This jenkinsfile builds a selenium container with bonitoo's
tests inside for use in testing the ui.

 */

podTemplate = "dind-1-12"
dindContainer = "dind"
opsDir = "ops"
repoTag = "quay.io/influxdb/"

pipeline {
  environment {
    testStashed = "${WORKSPACE}/vars/thing"
  }

  agent {
    node {
      label "${podTemplate}"
    }
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '2'))
  }

  parameters {
    booleanParam(
      name: 'REAL_STASH',
      defaultValue: true,
      description: 'stash a real file'
    )
  }

  stages {
    stage('stash a real thing') {
      when {
        environment name: 'REAL_STASH', value: 'true'
      }

      steps {
        dir("${WORKSPACE}") {
          // don't think this actually does things where it's inside a container.
          container(dindContainer) {
            sh "mkdir -p ./vars"
            sh "chmod 777 -R ./vars"
            sh "rm -f ./vars/thing"
          }
          sh "mkdir -p ./vars"
          sh "echo hi > ./vars/thing"
          sh "ls -lah ./vars"

          stash name: 'testStash', includes: 'vars/*'
        }
      }
    }

    stage('stash a fake thing') {
      when {
        environment name: 'REAL_STASH', value: 'false'
      }

      steps {
        dir("${WORKSPACE}") {
          // don't think this actually does things where it's inside a container.
          container(dindContainer) {
            sh "mkdir -p ./vars"
            sh "chmod 777 -R ./vars"
            sh "rm -f ./vars/thing"
          }
          sh "mkdir -p ./vars"
          sh "touch ./vars/thing"
          sh "ls -lah ./vars"

          stash name: 'testStash', includes: 'vars/*'
        }
      }
    }

    stage('unstash a thing') {
      when {
        expression {
          unstash name: 'testStash'
          return fileExists("${testStashed}") && readFile("${testStashed}").contains("hi")
        }
      }

      agent {
        node {
          label "${podTemplate}"
        }
      }

      steps {
        script {
          echo "hola gato"
        }
      }

      post {
        success {
          echo "yay"
        }

        failure {
          echo "bummer"
        }
      }
    }
  }
}
