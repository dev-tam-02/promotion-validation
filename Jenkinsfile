pipeline {
   agent any

   environment {
        SPRING_PROFILE = "prod"
        SERVICE = "validation"
        PROJECT_PATH = "promix/promix-service/${SERVICE}"
        IMAGE_NAME = "harleyhoang/${SERVICE}"
        IMAGE_TAG = "v1.${env.GIT_COMMIT[0..6]}"
        DOCKER_IMAGE = "${IMAGE_NAME}:${IMAGE_TAG}"
        REGISTRY_CREDENTIALS_ID = "gitlab-credential"
        NETWORK = "promotion-network"
   }

    stages {
        stage('Build') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main' }
            }
            steps {
                script {
                    echo "Building application with Maven..."
                    sh "mvn clean package -DskipTests"
                    echo "Building Docker image..."
                    sh "docker build -t ${DOCKER_IMAGE} ."
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh "docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}"
                        sh "docker push ${DOCKER_IMAGE}"
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main' }
            }
            steps {
                script {
                    sh """
                        echo "Deploying application..."
                        echo "Stopping old container if exists..."
                        docker stop ${SERVICE} || true
                        docker rm ${SERVICE} || true
                        echo "Running new container..."
                        docker run -d --name ${SERVICE} \
                        --restart=always \
                        --network ${NETWORK} \
                        -v /var/log/promotion/${SERVICE}:/app/logs \
                        --label com.docker.compose.service=${SERVICE} \
                        -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILE} \
                        ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }
    }
}
