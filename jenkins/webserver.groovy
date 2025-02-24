pipeline {
    // Явное определение метки агента
    agent {
        label "agent-1"
    }

    // Сохранение только последних 10 билдов
    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
    }

    // Указание желаемой ветки, из которой мы хотим собрать проект (опционально, по умолчанию это main)
    parameters {
        string(name: 'branch', defaultValue: 'main', trim: true, description: 'Default branch for project:')
    }

    // Переменные с нечувствительной информацией, чтобы отпала необходимость во множественном изменении походу кода 
    environment {
        WORKDIR = "project"
        PROJECT_DIR = "/opt/team-panda/"
        IMAGE = "webserver"
        TAG = "local"
    }

    // Чекаут репозитория на агенте
    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                dir("$WORKDIR") {
                    git(branch: '$branch', credentialsId: 'github', url: 'https://github.com/exitfound/test-panda-team.git')
                }
            }
        }

        // Этап, относящийся к сборке Docker образа (самый простой вариант реализации)
        stage ('Build') {
            steps {
                sh('''
                    cd $WORKDIR
                    docker build -t "$IMAGE:$TAG" -f Dockerfile .
                ''')
            }
        }

        // Этап, относящийся к запуску проекта на базе собранного образа (самый простой вариант реализации)
        stage('Deploy') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'builder', usernameVariable: 'USER', keyFileVariable: 'SSH_KEY'),
                    string(credentialsId: 'remote_host', variable: 'REMOTE_HOST')]) {
                        sh '''
                            cd $WORKDIR
                            rsync -avz -e "ssh -i $SSH_KEY -o StrictHostKeyChecking=no" docker-compose.yaml $USER@$REMOTE_HOST:$PROJECT_DIR
                            ssh -i $SSH_KEY -o StrictHostKeyChecking=no $USER@$REMOTE_HOST "
                                cd $PROJECT_DIR \
                                && docker compose up -d --force-recreate"
                        '''
                }
            }
        }
    }

    // Пост деятельность (очистка старых контейнеров и образов в случае успешного исполнения)
    post {
        success {
            sh '''
                docker container prune --force
                docker image prune --force --all
            '''
        }
    }
}
