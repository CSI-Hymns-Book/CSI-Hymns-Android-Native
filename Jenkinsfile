pipeline {

    agent any

    options {
        ansiColor('xterm')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '25'))
        disableConcurrentBuilds()
    }

    parameters {

        choice(
            name: 'BRANCH',
            choices: ['main', 'dev'],
            description: 'Git branch (CI runs only for main and dev)'
        )

        booleanParam(
            name: 'DEPLOY_PLAY',
            defaultValue: false,
            description: 'Upload to Google Play via Fastlane'
        )

        choice(
            name: 'PLAY_TRACK',
            choices: ['open', 'production'],
            description: 'Play track — open = Open Testing (beta), production = Production (draft upload)'
        )

        booleanParam(
            name: 'CONFIRM_PRODUCTION',
            defaultValue: false,
            description: 'Required when PLAY_TRACK=production'
        )
    }

    environment {
        // Prefer agent env; fall back to known macbook SDK path.
        ANDROID_HOME = "${System.getenv('ANDROID_HOME') ?: '/Users/reyzie29/Library/Android/sdk'}"
        ANDROID_SDK_ROOT = "${System.getenv('ANDROID_SDK_ROOT') ?: System.getenv('ANDROID_HOME') ?: '/Users/reyzie29/Library/Android/sdk'}"
        KEYSTORE_FILE = 'release-keystore.jks'
        DEPLOY_STATUS = 'skipped'
        BUILD_STATUS = 'unknown'
        PLAY_TRACK_LABEL = "${params.PLAY_TRACK}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[
                        url: 'https://github.com/CSI-Hymns-Book/CSI-Hymns-Android-Native.git'
                    ]]
                ])
            }
        }

        stage('Collect Metadata') {
            steps {
                script {
                    sh "BRANCH='${params.BRANCH}' bash ci/collect_metadata.sh ci/build_metadata.env"
                    loadDotenv('ci/build_metadata.env')
                    env.BRANCH = params.BRANCH
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.VERSION_NAME}"
                    echo "Version ${env.VERSION_NAME} (${env.VERSION_CODE}) · ${env.GIT_COMMIT} by ${env.GIT_AUTHOR}"
                    echo "Java: ${env.JAVA_VERSION}"
                    echo "Gradle: ${env.GRADLE_VERSION} · AGP: ${env.AGP_VERSION} · Kotlin: ${env.KOTLIN_VERSION}"
                }
            }
        }

        stage('Read Changelog') {
            steps {
                script {
                    sh 'python3 ci/read_changelog.py > /dev/null'
                    loadDotenv('ci/changelog.env')
                    echo "Changelog: ${env.CHANGELOG_TITLE} (${env.CHANGELOG_DATE})"
                }
            }
        }

        stage('Inject Signing') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'android-keystore',
                        variable: 'KEYSTORE'
                    ),
                    string(
                        credentialsId: 'android-keystore-password',
                        variable: 'STORE_PASSWORD'
                    ),
                    string(
                        credentialsId: 'android-key-alias',
                        variable: 'KEY_ALIAS'
                    ),
                    string(
                        credentialsId: 'android-key-password',
                        variable: 'KEY_PASSWORD'
                    )
                ]) {
                    // printf %s avoids shell expansion of $, `, \\ in passwords/aliases.
                    sh '''
                    cp "$KEYSTORE" "$KEYSTORE_FILE"

                    printf 'storeFile=%s\n' "$KEYSTORE_FILE" > keystore.properties
                    printf 'storePassword=%s\n' "$STORE_PASSWORD" >> keystore.properties
                    printf 'keyAlias=%s\n' "$KEY_ALIAS" >> keystore.properties
                    printf 'keyPassword=%s\n' "$KEY_PASSWORD" >> keystore.properties
                    '''
                }
            }
        }

        stage('Clean') {
            steps {
                sh './gradlew clean --no-daemon'
            }
        }

        stage('Build APK') {
            steps {
                sh './gradlew assembleRelease --no-daemon'
            }
        }

        stage('Build AAB') {
            steps {
                sh './gradlew bundleRelease --no-daemon'
            }
        }

        stage('Verify Signing') {
            steps {
                sh '''
                set -e
                APK="$(find app/build/outputs/apk/release -name '*.apk' -type f | head -1)"
                AAB="$(find app/build/outputs/bundle/release -name '*.aab' -type f | head -1)"

                if [ -z "$APK" ] || [ -z "$AAB" ]; then
                    echo "Missing signed artifacts"
                    exit 1
                fi

                APKSIGNER="$(ls -1 "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)"
                if [ -z "$APKSIGNER" ] || [ ! -x "$APKSIGNER" ]; then
                    echo "apksigner not found under $ANDROID_HOME/build-tools"
                    exit 1
                fi

                # APK Signature Scheme v2/v3 — use apksigner, not jarsigner.
                "$APKSIGNER" verify --print-certs "$APK"
                # AAB uses JAR signing; jarsigner is appropriate here.
                jarsigner -verify "$AAB"
                '''
            }
        }

        stage('Resolve Artifacts') {
            steps {
                script {
                    env.APK_PATH = sh(
                        script: 'find app/build/outputs/apk/release -name "*.apk" -type f | head -1',
                        returnStdout: true
                    ).trim()

                    env.AAB_PATH = sh(
                        script: 'find app/build/outputs/bundle/release -name "*.aab" -type f | head -1',
                        returnStdout: true
                    ).trim()

                    if (!env.APK_PATH) {
                        error('Release APK not found after build')
                    }
                    if (!env.AAB_PATH) {
                        error('Release AAB not found after build')
                    }

                    echo "APK: ${env.APK_PATH}"
                    echo "AAB: ${env.AAB_PATH}"
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: '''
app/build/outputs/**/*.apk,
app/build/outputs/**/*.aab,
app/build/outputs/mapping/release/mapping.txt,
app/build/intermediates/native_debug_metadata/release/*.zip
''', fingerprint: true, allowEmptyArchive: true
            }
        }

        stage('Deploy to Google Play') {
            when {
                expression { return params.DEPLOY_PLAY }
            }
            steps {
                script {
                    if (params.PLAY_TRACK == 'production' && !params.CONFIRM_PRODUCTION) {
                        error('Production upload requires CONFIRM_PRODUCTION=true')
                    }

                    def fastlaneTrack = params.PLAY_TRACK == 'production' ? 'production' : 'beta'
                    env.PLAY_TRACK_LABEL = params.PLAY_TRACK

                    try {
                        withCredentials([
                            file(
                                credentialsId: 'google-play-service-account',
                                variable: 'PLAY_JSON_KEY'
                            )
                        ]) {
                            sh """
                            export PATH="/opt/homebrew/opt/ruby/bin:/opt/homebrew/lib/ruby/gems/4.0.0/bin:\$PATH"
                            export PLAY_JSON_KEY_PATH="\$PLAY_JSON_KEY"
                            export PLAY_TRACK="${fastlaneTrack}"
                            export AAB_PATH="${env.AAB_PATH}"
                            export VERSION_NAME="${env.VERSION_NAME}"
                            bash ci/setup_bundler.sh
                            bundle exec fastlane android deploy
                            """
                        }
                        env.DEPLOY_STATUS = 'deployed'
                        echo "Deployed to Google Play (${params.PLAY_TRACK} → ${fastlaneTrack}; production uploads as draft)"
                    } catch (Exception deployError) {
                        env.DEPLOY_STATUS = 'failed'
                        throw deployError
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                notifySlack()
                notifyTelegram()
            }
            sh '''
            rm -f release-keystore.jks
            rm -f keystore.properties
            rm -f ci/build_metadata.env
            rm -f ci/changelog.env
            rm -f ci/changelog_release.txt
            '''
            cleanWs()
        }
    }
}

// Load KEY=VALUE lines (supports escaped newlines in values).
def loadDotenv(String path) {
    def text = readFile(path)
    text.split('\n').each { line ->
        if (!line.trim() || line.trim().startsWith('#')) {
            return
        }
        def eq = line.indexOf('=')
        if (eq <= 0) {
            return
        }
        def key = line.substring(0, eq)
        def value = line.substring(eq + 1)
        // Strip optional surrounding quotes from older env writers.
        if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1)
        }
        value = value.replace('\\n', '\n').replace('\\"', '"').replace('\\\\', '\\')
        env[key] = value
    }
}

def notifySlack() {
    env.BUILD_STATUS = currentBuild.currentResult == 'SUCCESS' ? 'success' : 'failure'

    try {
        withCredentials([
            string(credentialsId: 'slack-bot-token', variable: 'SLACK_BOT_TOKEN'),
            string(credentialsId: 'slack-build-channel', variable: 'SLACK_CHANNEL')
        ]) {
            sh 'python3 ci/notify_slack.py'
        }
    } catch (Exception slackError) {
        echo "Slack notification failed: ${slackError}"
    }
}

def notifyTelegram() {
    env.BUILD_STATUS = currentBuild.currentResult == 'SUCCESS' ? 'success' : 'failure'

    try {
        withCredentials([
            string(credentialsId: 'telegram-bot-token', variable: 'TELEGRAM_BOT_TOKEN'),
            string(credentialsId: 'telegram-chat-id', variable: 'TELEGRAM_CHAT_ID')
        ]) {
            sh 'python3 ci/notify_telegram.py'
        }
    } catch (Exception telegramError) {
        echo "Telegram notification failed: ${telegramError}"
    }
}
