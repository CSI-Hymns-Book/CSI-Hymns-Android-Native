pipeline {

    // Built-in "Jenkins" node is the Mac Mini controller — Android SDK is on the MacBook agent.
    agent {
        label 'MacBook Pro M1 Pro — Builder'
    }

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
        // Literal paths only — System.getenv() is blocked by Jenkins script sandbox.
        ANDROID_HOME = '/Users/reyzie29/Library/Android/sdk'
        ANDROID_SDK_ROOT = '/Users/reyzie29/Library/Android/sdk'
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
                script {
                    def patterns = [
                        'app/build/outputs/**/*.apk',
                        'app/build/outputs/**/*.aab',
                    ]

                    if (fileExists('app/build/outputs/mapping/release/mapping.txt')) {
                        patterns << 'app/build/outputs/mapping/release/mapping.txt'
                        echo 'Archiving ProGuard/R8 mapping.txt'
                    } else {
                        echo 'Skipping mapping.txt (not present — minify may be off)'
                    }

                    def nativeStatus = sh(
                        script: 'ls app/build/intermediates/native_debug_metadata/release/*.zip >/dev/null 2>&1',
                        returnStatus: true
                    )
                    if (nativeStatus == 0) {
                        patterns << 'app/build/intermediates/native_debug_metadata/release/*.zip'
                        echo 'Archiving native_debug_metadata'
                    } else {
                        echo 'Skipping native_debug_metadata (not generated for this build)'
                    }

                    archiveArtifacts artifacts: patterns.join(','), fingerprint: true, allowEmptyArchive: false
                }
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
        // Success: rich notify + artifact uploads (scripts only upload on success).
        success {
            script {
                notifySlack('success')
                notifyTelegram('success')
            }
        }
        // Failure / abort: failure-only message — no deploy, no artifact upload.
        failure {
            script {
                notifySlack('failure')
                notifyTelegram('failure')
            }
        }
        aborted {
            script {
                notifySlack('failure')
                notifyTelegram('failure')
            }
        }
        // Always: wipe secrets/workspace after notify.
        always {
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

/**
 * Load KEY=value lines into env without dynamic env[key] (sandbox putAt).
 * Only known CI keys are assigned via env.NAME = value.
 */
def loadDotenv(String path) {
    def text = readFile(path)
    text.split('\n').each { String line ->
        def trimmed = line.trim()
        if (!trimmed || trimmed.startsWith('#')) {
            return
        }
        def eq = trimmed.indexOf('=')
        if (eq <= 0) {
            return
        }
        def key = trimmed.substring(0, eq).trim()
        def value = trimmed.substring(eq + 1)
        if ((value.startsWith('"') && value.endsWith('"')) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1)
        }
        value = value.replace('\\n', '\n').replace('\\"', '"').replace('\\\\', '\\')
        setKnownEnv(key, value)
    }
}

def setKnownEnv(String key, String value) {
    switch (key) {
        case 'VERSION_NAME':
            env.VERSION_NAME = value
            break
        case 'VERSION_CODE':
            env.VERSION_CODE = value
            break
        case 'AGP_VERSION':
            env.AGP_VERSION = value
            break
        case 'KOTLIN_VERSION':
            env.KOTLIN_VERSION = value
            break
        case 'GRADLE_VERSION':
            env.GRADLE_VERSION = value
            break
        case 'GIT_COMMIT':
            env.GIT_COMMIT = value
            break
        case 'GIT_COMMIT_FULL':
            env.GIT_COMMIT_FULL = value
            break
        case 'GIT_AUTHOR':
            env.GIT_AUTHOR = value
            break
        case 'GIT_COMMIT_MESSAGE':
            env.GIT_COMMIT_MESSAGE = value
            break
        case 'JAVA_VERSION':
            env.JAVA_VERSION = value
            break
        case 'ANDROID_STUDIO_VERSION':
            env.ANDROID_STUDIO_VERSION = value
            break
        case 'GITHUB_COMMIT_URL':
            env.GITHUB_COMMIT_URL = value
            break
        case 'PLAY_CONSOLE_URL':
            env.PLAY_CONSOLE_URL = value
            break
        case 'BRANCH':
            env.BRANCH = value
            break
        case 'CHANGELOG_TITLE':
            env.CHANGELOG_TITLE = value
            break
        case 'CHANGELOG_VERSION':
            env.CHANGELOG_VERSION = value
            break
        case 'CHANGELOG_DATE':
            env.CHANGELOG_DATE = value
            break
        case 'CHANGELOG_CHANGES':
            env.CHANGELOG_CHANGES = value
            break
        case 'CHANGELOG_CHANGES_SLACK':
            env.CHANGELOG_CHANGES_SLACK = value
            break
        default:
            echo "loadDotenv: ignoring unknown key ${key}"
            break
    }
}

def notifySlack(String status) {
    env.BUILD_STATUS = status

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

def notifyTelegram(String status) {
    env.BUILD_STATUS = status

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
