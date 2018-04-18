node {
    try {
        stage('checkout') {
            git branch: 'new_config', credentialsId: 'mmi-github', url: 'http://git-ces.mmih.biz/Money/multiply-money-management.git'
        }
        dir('./') {
            stage('cleaning workspace') {
                sh 'mvn clean -U'
            }
            stage('maven install') {
                sh 'mvn install'
            }
            stage('archiving artifacts') {
                archiveArtifacts "impl/target/*.war"
                archiveArtifacts "impl/target/maven-archiver/pom.properties"
            }
            stage('copy artifacts to files directory') {
                sh 'cp impl/target/maven-archiver/pom.properties docker/files'
            }
            stage('Build docker image') {
                sh 'docker/scripts/buildImage.sh'
            }
            stage('Pushing docker image to docker cloud') {
                sh 'docker/scripts/pushImage.sh'
            }
        }
    } catch (Exception ee) {
        currentBuild.result = 'FAILURE'
        slackSend (channel: '#moneybuilds', color: '#FF0000', message: "FAILURE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
}
