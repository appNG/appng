node {
    try {
        // parameters:
        // maven_opts- additional maven opts, e.g -Pjavadocs
    
        def mvnHome = tool 'Maven 3.5.0'
        def BRANCH_NAME = env.BRANCH_NAME
        
        stage ('notifyStart'){
            emailext (
                subject: "[appNG Jenkins] STARTED: Job ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                body: """<p>STARTED: Job <strong>${env.JOB_NAME} [${env.BUILD_NUMBER}]</strong>:</p>
                    <p>Check console output at <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>""",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
                mimeType: 'text/html'
            )
        }

        stage('clone') {
          git([url: 'git@github.com:appNG/appng.git', branch: '$BRANCH_NAME'])
        }

        stage('Maven Build') {
       		sh "pwd"
            sh "ls -ll"
        	sh "'${mvnHome}/bin/mvn' -version"
            sh "'${mvnHome}/bin/mvn' clean install -Djavax.xml.accessExternalSchema=all -Pci -Dbuild.version=${GIT_BRANCH}"
            sh "'${mvnHome}/bin/mvn' javadoc:aggregate"
        }

        stage('Results') {
            junit '**/target/surefire-reports/*.xml'
        }

        currentBuild.result = 'SUCCESS'
    } catch (Exception err) {
        currentBuild.result = 'FAILURE'
    }

    stage ('notifyFinish'){
        // send to email
        emailext (
            subject: "[appNG Jenkins] FINISHED: Job ${env.JOB_NAME} [${env.BUILD_NUMBER}] with status: ${currentBuild.result}",
            body: """<p>FINISHED: Job <strong>${env.JOB_NAME} [${env.BUILD_NUMBER}]</strong> with status: <strong>${currentBuild.result}</strong></p>
                <p>Check console output at <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>""",
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
            mimeType: 'text/html'
        )
    }
}
