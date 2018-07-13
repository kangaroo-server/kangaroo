/**
 * Java build pipeline for the kangaroo-server project.
 */
@Library('kangaroo-jenkins') _

def dbName = env.BUILD_TAG
        .replaceAll(/\%[0-9A-F]{2}/, "-")
        .replaceAll(/[^_a-zA-Z0-9]/, "_")
        .toLowerCase()
def jdbc_mariadb = "jdbc:mariadb://127.0.0.1:3306/${dbName}?useUnicode=yes"


pipeline {

    agent {
        label 'worker'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    environment {
        KANGAROO_FB_APP = credentials('jenkins_facebook_app')
        KANGAROO_GITHUB_APP = credentials('jenkins_github_app')
        KANGAROO_GITHUB_ACCOUNT = credentials('jenkins_github_account')
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
                        "bootstrap": {
                            sh """
                                mvn clean install -pl kangaroo-common -DskipTests=true
                                mvn dependency:resolve dependency:resolve-plugins
                            """
                        },
                        "stat": {
                            script {
                                sh 'env'
                                sh 'mvn --version'
                            }
                        })
            }
        }

        /**
         * Build all the binaries, make sure it all compiles.
         */
        stage('Build') {
            steps {
                sh """
                    mvn clean install -Dhibernate.connection.url=${jdbc_mariadb}
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
            junit '**/target/surefire-reports/*.xml'

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
                    pattern            : '**/target/checkstyle-result.xml',
                    unHealthy          : '100',
                    unstableTotalAll   : '0',
                    unstableTotalHigh  : '0',
                    unstableTotalLow   : '0',
                    unstableTotalNormal: '0'
            ])

            /**
             * PMD & PMD/CPD
             */
            pmd(pattern: '**/target/pmd.xml', unstableTotalAll: '0')

            /**
             * Delete everything, to keep track of disk size.
             */
            cleanWs(deleteDirs: true)
        }
    }
}
