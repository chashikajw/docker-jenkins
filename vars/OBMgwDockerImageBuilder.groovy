/*
* Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/

def call() {

    pipeline {
        agent {
            label 'AWS02'
        }

        environment {
            PATH = "/usr/local/wum/bin:$PATH"
        }

        stages {
            stage('Download_MGW_Toolkit') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'docker-image-build', passwordVariable: 'WUM_PASSWORD', usernameVariable: 'WUM_USERNAME')]) {
                        sh 'wum init -u $WUM_USERNAME -p $WUM_PASSWORD'
                        sh """
                        if [ -d "${JENKINS_WUM_HOME}/${MGW_TOOLKIT}/${MGW_TOOLKIT_VERSION}/full" ]; 
                            then rm -Rf ${JENKINS_WUM_HOME}/${MGW_TOOLKIT}/${MGW_TOOLKIT_VERSION}/full; 
                        fi
                        wum update ${MGW_TOOLKIT}-${MGW_TOOLKIT_VERSION}
                    """
                    }
                }
            }
            stage('Download_&_build_OB') {
                steps {
                    sh """ 
                    if [ -d "financial-open-banking" ]; then rm -Rf financial-open-banking; fi
                    git clone https://wso2-support-user:$GIT_SCM_TOKEN@github.com/${OB_REPO}.git
                    truncate -s 0 scanResult.txt
                    cd financial-open-banking
                    git checkout ${OB_SUPPORT_BRANCH_PREFIX}-${WSO2_OB_SOLUTION_VERSION}
                    mvn clean install || {
                            cd ..
                            echo " " | tee -a scanResult.txt
                            echo "<font color="black"><b>--------Source Code Build Status--------</b></font></p><br>" | tee -a scanResult.txt
                            echo "${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}" | tee -a scanResult.txt
                            echo "--------------------------------------------" | tee -a scanResult.txt
                            echo "Building financial-open-banking/${OB_SUPPORT_BRANCH_PREFIX}-${WSO2_OB_SOLUTION_VERSION} failed..." >> scanResult.txt
                            exit 1
                            }
                """
                }
            }
            stage('Build_docker_images') {
                steps {
                    sh """
                    cd financial-open-banking
                    mkdir packs
                    cp ${JENKINS_WUM_HOME}/${MGW_TOOLKIT}/${MGW_TOOLKIT_VERSION}/full/${MGW_TOOLKIT}-${MGW_TOOLKIT_VERSION}*full.zip ./packs/${MGW_TOOLKIT}-${MGW_TOOLKIT_VERSION}.zip
                    if [ "${PROJECT_NAME}" = "cds" ]
                        then
                            PROJECT_DIRECTORY="cds-au"
                    elif [ "${PROJECT_NAME}" = "cds-admin" ]
                        then
                            PROJECT_DIRECTORY="cds-au-admin"
                    elif [ "${PROJECT_NAME}" = "cdr-arrangement" ]
                        then
                            PROJECT_DIRECTORY="cdr-arrangement-api"
                    elif [ "${PROJECT_NAME}" = "cds-dcr" ]
                        then
                            PROJECT_DIRECTORY="dcrapi"
                    fi
                    cd micro-gateway/cds/\${PROJECT_DIRECTORY}
                    echo A | ./micro-gw-image.sh build 2>&1 | tee ../toolkit_log.txt
                    if [ "${PROJECT_NAME}" = "cds" ]
                        then
                            for PROJECT_CATEGORY in customer discovery direct-debits payees products payments accounts balances transactions
                                do
                                    echo A | ./micro-gw-image.sh build \${PROJECT_CATEGORY} 2>&1 | tee dockerTag.txt
                                done
                    fi
                """
                }
            }
            stage('Scan_docker_image') {
                steps {
                    script {
                        sh """
                            DOCKER_TAG=\$(tail -1 financial-open-banking/micro-gateway/cds/toolkit_log.txt)
                            trivy image --output "${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}"-scanResult.txt --severity HIGH docker.wso2.com/"${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}":"\${DOCKER_TAG}"
                            echo " " | tee -a scanResult.txt
                            echo "${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}" | tee -a scanResult.txt
                            echo "--------------------------------------------" | tee -a scanResult.txt
                            if [ ! -s "${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}"-scanResult.txt ]; then
                                echo "No HIGH vulnerabilities detected..." >> "${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}"-scanResult.txt
                                cat "${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}"-scanResult.txt >> scanResult.txt                                
                            else
                                echo "HIGH vulnerabilities detected...\nBuild failed..." >> scanResult.txt
                                cat "${MGW_PROJECT_PREFIX}"-"${PROJECT_NAME}"-scanResult.txt >> scanResult.txt
                                exit 1
                            fi
                        """
                    }
                }
            }
            stage('Tag_and_Push_images') {
                steps {
                    script {
                        sh 'echo "Tagging docker images.."'
                        TIMESTAMP = new Date().getTime()
                        DOCKER_TAG = sh(script: 'tail -1 financial-open-banking/micro-gateway/cds/toolkit_log.txt', returnStdout: true).trim()
                        withCredentials([usernamePassword(credentialsId: 'docker-registry', passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USERNAME')]) {
                            sh 'docker login docker.wso2.com -u $REGISTRY_USERNAME -p $REGISTRY_PASSWORD'
                        }
                        tag_images(MGW_PROJECT_PREFIX + "-" + PROJECT_NAME, DOCKER_TAG, TIMESTAMP)
                        push_images(MGW_PROJECT_PREFIX + "-" + PROJECT_NAME, DOCKER_TAG, TIMESTAMP)
                        if (PROJECT_NAME == 'cds') {
                            def list = ["customer", "discovery", "direct-debits", "payees", "products", "payments", "accounts", "balances", "transactions"]
                            for (category in list) {
                                tag_images(MGW_PROJECT_PREFIX + "-" + PROJECT_NAME + "-" + category, DOCKER_TAG, TIMESTAMP)
                                push_images(MGW_PROJECT_PREFIX + "-" + PROJECT_NAME + "-" + category, DOCKER_TAG, TIMESTAMP)
                            }
                        }
                    }
                }
            }
        }
        post {
            always {
                script {
                    String emailBodyScan = readFile "scanResult.txt"
                    String PRODUCT_VERSION = sh(script: 'tail -1 financial-open-banking/micro-gateway/cds/toolkit_log.txt', returnStdout: true).trim()
                    send("Build ${currentBuild.currentResult} in Docker Image Build Jenkins: ${env.JOB_NAME} - #${env.BUILD_NUMBER}", """
                    <font color="black"><b>--------Build Info--------</b></font></p><br>
                    <b>Product</b> : ${MGW_PROJECT_PREFIX}-${PROJECT_NAME}<br>
                    <b>Version</b> : ${PRODUCT_VERSION}<br>
                    <b>Build Status</b> : ${currentBuild.currentResult}<br>
                    <b>Docker Registry</b> : https://docker.wso2.com/tags.php?repo=${MGW_PROJECT_PREFIX}-${PROJECT_NAME}<br>
                    <font color="black"><b>--------Docker Image Vulnerability Scan Report--------</b></font></p><br>
                    <pre>
                    ${emailBodyScan}
                    </pre>
                    </p><br>
                    <p>Check console output at ${BUILD_URL} to view the results.</p>
                """)
                    sh """
                        echo "Cleaning Docker Container, networks, images, and build cache..."
                        docker system prune -a -f
                        echo "Cleaning wum updates in temp directory..."
                        cd /tmp && find . -name 'wum-*' -exec rm -rv {} + && find . -name 'docker-*' -exec rm -rv {} +
                    """
                }
            }
        }
    }
}

def send(subject, content) {
    emailext(to: "${EMAIL_TO}",
            subject: subject,
            body: content, mimeType: 'text/html')
}

def tag_images(image_name, tag, timestamp) {
    dockerImage = docker.image("docker.wso2.com/" + image_name + ":" + tag)
    dockerImage.tag(tag + "." + timestamp)
    if (LATEST.toBoolean()) {
        dockerImage.tag("latest")
    }
}

def push_images(image_name, tag, timestamp){
    dockerImage = docker.image("docker.wso2.com/" + image_name + ":" + tag)
    dockerImage.push()
    taggedImage = docker.image("docker.wso2.com/" + image_name + ":" + tag + "." + timestamp)
    taggedImage.push()
    if (LATEST.toBoolean()) {
        latestImage = docker.image("docker.wso2.com/" + image_name + ":latest")
        latestImage.push()
    }
}
