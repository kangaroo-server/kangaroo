/**
 * Java build pipeline for the kangaroo-server project.
 */
@Library('kangaroo-jenkins') _

def gitCommit = ''
def jdbc_mariadb = "jdbc:mariadb://127.0.0.1:3306/oid?useUnicode=yes"

pipeline {

    agent {
        label 'worker'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    environment {
        KANGAROO_FB_APP = credentials('jenkins_facebook_app')
        KANGAROO_GOOGLE_APP = credentials('jenkins_google_app')
        KANGAROO_GOOGLE_ACCOUNT = credentials('jenkins_google_account')
    }

    stages {

        /**
         * Build all the binaries, make sure it all compiles.
         */
        stage('init') {
            steps {
                parallel(
                        "install": {
                            sh '''
                                mvn clean install \
                                    -DskipTests=true \
                                    -Dcheckstyle.skip=true \
                                    -Dpmd.skip=true \
                                    -Dcpdskip=true
                            '''
                        },
                        "stat": {
                            script {
                                sh 'env'
                                sh 'mvn --version'
                            }
                        },
                        "vars": {
                            script {
                                gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                                jdbc_mariadb = "jdbc:mariadb://127.0.0.1:3306/" +
                                        "test_${gitCommit.substring(0, 16)}" +
                                        "?useUnicode=yes"
                            }
                        })
            }
        }

        /**
         * Kangaroo-common.
         */
        stage('kangaroo-common') {
            steps {
                parallel(
                        "pmd": {
                            sh "mvn pmd:check -pl kangaroo-common "
                        },
                        "checkstyle": {
                            sh "mvn checkstyle:check -pl kangaroo-common"
                        },
                        "unit-h2": {
                            sh """
                            mvn test \
                                -Dcheckstyle.skip=true \
                                -Dpmd.skip=true \
                                -Dcpdskip=true \
                                -DskipTests.integration=true \
                                -pl kangaroo-common \
                                -Ph2 \
                                -Dtarget-directory=target-h2
                        """
                        },
                        "unit-mariadb": {
                            sh """
                            mvn test \
                                -Dcheckstyle.skip=true \
                                -Dpmd.skip=true \
                                -Dcpdskip=true \
                                -DskipTests.integration=true \
                                -pl kangaroo-common \
                                -Pmariadb \
                                -Dtarget-directory=target-mariadb \
                                -Dhibernate.connection.url=${jdbc_mariadb}
                        """
                        })
            }
        }

        /**
         * Kangaroo-server-authz.
         */
        stage('kangaroo-server-authz') {
            steps {
                parallel(
                        "pmd": {
                            sh "mvn pmd:check -pl kangaroo-server-authz "
                        },
                        "checkstyle": {
                            sh "mvn checkstyle:check -pl kangaroo-server-authz"
                        },
                        "unit-h2": {
                            sh """
                            mvn test \
                                -Dcheckstyle.skip=true \
                                -Dpmd.skip=true \
                                -Dcpdskip=true \
                                -DskipTests.integration=true \
                                -pl kangaroo-server-authz \
                                -Ph2 \
                                -Dtarget-directory=target-h2
                        """
                        },
                        "unit-mariadb": {
                            sh """
                            mvn test \
                                -Dcheckstyle.skip=true \
                                -Dpmd.skip=true \
                                -Dcpdskip=true \
                                -DskipTests.integration=true \
                                -pl kangaroo-server-authz \
                                -Pmariadb \
                                -Dtarget-directory=target-mariadb \
                                -Dhibernate.connection.url=${jdbc_mariadb}
                        """
                        })
            }
        }

        /**
         * Integration tests.
         */
        stage('integration') {
            steps {
                sh """
                    mvn integration-test verify \
                        -Dcheckstyle.skip=true \
                        -Dpmd.skip=true \
                        -Dcpdskip=true \
                        -DskipTests.unit=true \
                        -Ph2
                """
            }
        }
    }

    post {

        /**
         * When the build status changed, send the result.
         */
        changed {
            script {
                notifySlack(currentBuild.currentResult)
            }
        }

        /**
         * Actions always to run at the end of a pipeline.
         */
        always {
            /**
             * Screenshots
             */
            archive '**/target/screenshots/*.png'

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
