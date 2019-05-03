node {
    try {
        def mvnHome = tool 'Maven 3.5.0'
        def BRANCH_NAME = env.BRANCH_NAME
        def BUILD_VERSION =  BRANCH_NAME.replaceAll("\\W","_")
        def readme = 'README.adoc **/README.adoc'
        def resources = 'pom.xml **/pom.xml appng-archetype-application/src/main/resources/archetype-resources/pom.xml appng-archetype-application/readme.txt appng-documentation/src/main/asciidoc/listing/dependencies.txt'

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
            def SNAPSHOT = 'SNAPSHOT'        
            def CURRENT = sh (
                script: "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\\[|Download\\w+:)'",
                returnStdout: true
            ).trim()
            replaceVersionInResources(SNAPSHOT, BUILD_VERSION + '-' + SNAPSHOT, readme)
            replaceVersionInResources(SNAPSHOT, BUILD_VERSION + '-' + SNAPSHOT, resources)
            sh "'${mvnHome}/bin/mvn' clean deploy -Pci -Djavax.xml.accessExternalSchema=all"
            sh "'${mvnHome}/bin/mvn' javadoc:aggregate"
        }

        currentBuild.result = 'SUCCESS'
    } catch (Exception err) {
        currentBuild.result = 'FAILURE'
    }

    stage('Results') {
        junit '**/target/surefire-reports/*.xml'
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

def replaceVersionInResources(String source_version, String target_version, String resources){
    def sed_source = source_version.replaceAll("\\.", "\\\\.")
    def sed_target = target_version.replaceAll("\\.", "\\\\.")
    sh "sed -i 's/${sed_source}/${sed_target}/g' ${resources}"
}
