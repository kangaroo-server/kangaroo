/**
 * Java build pipeline for the kangaroo-server project.
 */

def gitCommit = ''
def jdbc_mariadb = "jdbc:mariadb://127.0.0.1:3306/oid?useUnicode=yes"

pipeline {

    agent {
        label 'worker'
    }

    environment {
        DB_ROOT = credentials('mysql')
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {

        /**
         * Get environment statistics.
         */
        stage('Stat') {
            steps {
                script {
                    sh 'env'
                    sh 'mvn --version'
                    gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    jdbc_mariadb = "jdbc:mariadb://127.0.0.1:3306/" +
                            "test_${gitCommit.substring(0, 16)}" +
                            "?useUnicode=yes"
                }
            }
        }

        /**
         * Test.
         */
        stage('Test') {
            steps {
                parallel(
                        "h2": {
                            sh """
                                mvn install \
                                    -Ph2 \
                                    -Dtarget-directory=target-h2
                            """
                        },
                        "mariadb": {
                            sh """
                                mvn install \
                                    -Pmariadb \
                                    -Dhibernate.root.password=${DB_ROOT_PSW} \
                                    -Dtarget-directory=target-mariadb \
                                    -Dhibernate.connection.url=${jdbc_mariadb}
                            """
                        })
            }
        }
    }

    post {
        /**
         * Actions always to run at the end of a pipeline.
         */
        always {

            /**
             * Code coverage reports.
             */
            step([
                    $class                    : 'JacocoPublisher',
                    minimumInstructionCoverage: '100',
                    minimumBranchCoverage     : '100',
                    minimumComplexityCoverage : '100',
                    minimumLineCoverage       : '100',
                    minimumMethodCoverage     : '100',
                    minimumClassCoverage      : '100',
                    changeBuildStatus         : true
            ])

            /**
             * JUnit reports
             */
            junit '**/target-*/surefire-reports/*.xml'

            /**
             * Checkstyle tests.
             */
            checkstyle([
                    canComputeNew      : true,
                    canRunOnFailed     : true,
                    defaultEncoding    : '',
                    failedTotalHigh    : '0',
                    failedTotalLow     : '0',
                    failedTotalNormal  : '0',
                    healthy            : '100',
                    pattern            : '**/target-*/checkstyle-result.xml',
                    unHealthy          : '100',
                    unstableTotalAll   : '0',
                    unstableTotalHigh  : '0',
                    unstableTotalLow   : '0',
                    unstableTotalNormal: '0'
            ])

            //   - bash <(curl -s https://codecov.io/bash)

            /**
             * PMD & PMD/CPD
             */
            pmd(pattern: '**/target-*/pmd.xml', unstableTotalAll: '0')

            /**
             * Delete everything, to keep track of disk size.
             */
            cleanWs(deleteDirs: true)
        }
    }
}