/**
 * Java build pipeline for the kangaroo-server project.
 */

def gitCommit = ''
def jdbc_mariadb = "jdbc:mariadb://127.0.0.1:3306/oid?useUnicode=yes"

pipeline {

    agent {
        label 'worker'
    }

    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    environment {
        KANGAROO_FB_APP = credentials('jenkins_facebook_app')
        KANGAROO_GOOGLE_APP = credentials('jenkins_google_app')
        KANGAROO_GOOGLE_ACCOUNT = credentials('jenkins_google_account')
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
                                    -Dtarget-directory=target-mariadb \
                                    -Dhibernate.connection.url=${jdbc_mariadb}
                            """
                        })
            }
        }
    }

    post {

        /**
         * When the build status changed, send the result.
         */
        changed {
            script {
                def buildStatus = currentBuild.currentResult
                def url = env.BUILD_URL, message, color

                if (env.CHANGE_URL && buildStatus == 'SUCCESS') {
                    url = env.CHANGE_URL
                }

                switch (buildStatus) {
                    case 'FAILURE':
                        message = "Build <${url}|${env.BRANCH_NAME}> failed."
                        color = '#AA0000'
                        break
                    case 'SUCCESS':
                        message = "Build <${url}|${env.BRANCH_NAME}> passed."
                        color = '#00AA00'
                        break
                    case 'UNSTABLE':
                    default:
                        message = "Build <${url}|${env.BRANCH_NAME}> unstable."
                        color = '#FFAA00'
                }

                slackSend(
                        channel: '#build-notifications',
                        tokenCredentialId: 'kangaroo-server-slack-id',
                        teamDomain: 'kangaroo-server',
                        color: color,
                        message: message
                )
            }
        }

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
