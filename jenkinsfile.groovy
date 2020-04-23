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
    shouldBuild = "${WORKSPACE}/vars/thing"
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
    booleanParam(
      name: 'FAKE_STASH',
      defaultValue: false,
      description: 'stash no file'
    )
  }

  stages {
    stage('stash a real thing') {
      when {
        expression {
          environment name: 'REAL_STASH', value: 'true'
        }
      }

      steps {
        dir("${WORKSPACE}") {
          // don't think this actually does things where it's inside a container.
          container(dindContainer) {
            sh "mkdir -p ./vars"
            sh "chmod 777 -R ./vars"
            sh "rm -f ./vars/shouldbuild"
          }
          sh "mkdir -p ./vars"
          sh "touch ./vars/thing"
          sh "ls -lah ./vars"

          stash name: 'shouldbuild', includes: 'vars/*'
        }
      }
    }

    stage('stash a fake thing') {
      when {
        expression {
          environment name: 'FAKE_STASH', value: 'true'
        }
      }

      steps {
        dir("${WORKSPACE}") {
          // don't think this actually does things where it's inside a container.
          container(dindContainer) {
            sh "mkdir -p ./vars"
            sh "chmod 777 -R ./vars"
            sh "rm -f ./vars/shouldbuild"
          }
          sh "mkdir -p ./vars"
          sh "ls -lah ./vars"

          stash name: 'shouldbuild', includes: 'vars/*'
        }
      }
    }

    stage('unstash a thing') {
      when {
        expression {
          unstash name: 'shouldbuild'
          return fileExists("${shouldBuild}")
        }
      }

      steps {
        script {
          sh "hola gato"
        }
      }

      post {
        success {
          sh "yay"
        }

        failure {
          sh "bummer"
        }
      }
    }
  }
}
