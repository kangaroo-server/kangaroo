pipeline {
  agent {
    docker {
      image '8u141-jdk-slim'
    }
    
  }
  stages {
    stage('Install') {
      steps {
        sh 'mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V'
      }
    }
    stage('Test') {
      steps {
        parallel(
          "kangaroo-common (h2)": {
            sh 'mvn install -Ph2 -pl kangaroo-common'
            
          },
          "kangaroo-authz-server (h2)": {
            sh 'mvn install -Ph2 -pl kangaroo-authz-server'
            
          }
        )
      }
    }
  }
}