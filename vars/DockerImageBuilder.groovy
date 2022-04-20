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
import org.wso2.ie.utils.CommonUtils

def call(product_key) {
    def SCRIPT_FILE_LOCATION = "org/wso2/ie/scripts"
    def multi_jdk_required
    def timestamp
    def docker_resources_git_release_tag
    def build_jobs = [:]
    pipeline {
        agent {
            label 'AWS02'
        }
        environment {
            PATH = "/usr/local/wum/bin:$PATH"
        }
        stages {
            stage('Download_product_packs') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'docker-image-build', passwordVariable: 'WUM_PASSWORD', usernameVariable: 'WUM_USERNAME')]) {
                        sh 'wum init -u $WUM_USERNAME -p $WUM_PASSWORD'
                    }
                    script {
                        wum_update_script = libraryResource "${SCRIPT_FILE_LOCATION}/wum-update.sh"
                        writeFile file: './wum-update.sh', text: wum_update_script
                        sh 'chmod +x ${WORKSPACE}/wum-update.sh'
                        sh '${WORKSPACE}/wum-update.sh $wso2_product $wso2_product_version'
                        stash includes: 'timestamp.properties', name: 'properties'
                        stash includes: 'wum_details.txt', name: 'txt'
                    }
                }
            }
            stage('Build and Push') {
                steps {
                    script {
                        build_script = new CommonUtils()
                        product_profile_docker_homes = build_script.get_product_docker_home(wso2_product, wso2_product_version)
                        build_script.get_docker_release_version(wso2_product, wso2_product_version, product_key, product_profile_docker_homes)
                        timestamp = build_script.get_latest_wum_timestamp(wso2_product, wso2_product_version)
                        if (product_key == "open-banking") {
                            os_platforms = [alpine: '3.10', ubuntu: '18.04']
                        } else {
                            os_platforms = [alpine: '3.10', ubuntu: '18.04', centos: '7']
                        }
                        multi_jdk_required = build_script.get_multi_jdk_required()
                        docker_resources_git_release_tag = build_script.get_docker_resources_git_release_tag()

                        if (multi_jdk_required == true) {
                            jdk_versions = [jdk8:"jdk8", jdk11:"jdk11"]
                        } else {
                            jdk_versions = []
                        }
                        for (os_platform_name in os_platforms.keySet()) {
                            for (product_profile_docker_home in product_profile_docker_homes) {
                                if (multi_jdk_required == true) {
                                    for (jdk_version in jdk_versions.keySet()) {
                                        if (jdk_version == "jdk11") {
                                            build_jobs["${os_platform_name}-${product_profile_docker_home}"] = create_build_job(build_script, wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, "", false)
                                        } else {
                                            build_jobs["${os_platform_name}-${product_profile_docker_home}-${jdk_version}"] = create_build_job(build_script, wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, jdk_version, true)
                                        }
                                    }
                                } else {
                                    build_jobs["${os_platform_name}-${product_profile_docker_home}"] = create_build_job(build_script, wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, "", false)
                                }
                            }
                        }
                        parallel build_jobs
                    }
                }
            }
        stage('Scan') {
            steps {
                script {
                    // create file to add the product and its vulnerability scan report
                    image_scan_script = libraryResource "${SCRIPT_FILE_LOCATION}/image-scan.sh"
                    def release_version = docker_resources_git_release_tag.split(/\./)[3]
                    writeFile file: './image-scan.sh', text: image_scan_script
                    sh """
                        rm -rf scanResult.txt
                        chmod +x ${WORKSPACE}/image-scan.sh
                        ${WORKSPACE}/image-scan.sh $wso2_product $wso2_product_version.$timestamp.$release_version $multi_jdk_required
                    """
                    archiveArtifacts artifacts: 'scanResult.txt', onlyIfSuccessful: true
                }
            }
        }
    }
        post {
            always {
                script {
                    String emailBodyScan = readFile "scanResult.txt"
                    if ( timestamp != 'wum_timestamp=GA' ) {
                          timestamp = readFile "wum_details.txt"
                    }

                    send("[ ${currentBuild.currentResult} ] in Docker Image Build for WUM : ${wso2_product}-${wso2_product_version} - #${env.BUILD_NUMBER}", """
                    <font color="black"><b>--------Build Info--------</b></font></p><br>
                    <b>Product</b> : ${wso2_product}<br>
                    <b>Version</b> : ${wso2_product_version}<br>
                    <b>Build Status</b> : ${currentBuild.currentResult}<br>
                    <b>Docker Registry</b> : https://docker.wso2.com/tags.php?repo=${wso2_product}<br>
                    <b>WUM Details</b> : <pre>${timestamp}</pre>
                    <font color="black"><b>--------Docker Image Vulnerability Scan Report--------</b></font></p><br>
                    <pre>
                    ${emailBodyScan}
                    </pre>
                    </p><br>
                    <p>Check console output at ${BUILD_URL} to view the results.</p>
                """)

                    cleanup_script = libraryResource "${SCRIPT_FILE_LOCATION}/cleanup.sh"
                    writeFile file: './cleanup.sh', text: cleanup_script
                    sh 'chmod +x ${WORKSPACE}/cleanup.sh'
                    sh '${WORKSPACE}/cleanup.sh $wso2_product $wso2_product_version'
                }
            }
        }
            // Uncomment the following block to activate failure email notification
            // failure {
            //     emailext body: 'Check console output at $BUILD_URL to view the results. \n\n -------------------------------------------------- \n${BUILD_LOG, maxLines=100, escapeHtml=false}',
            //     to: "${EMAIL_TO}",
            //     subject: 'Build failed in Docker Image Build Jenkins: $PROJECT_NAME - #$BUILD_NUMBER'
            // }
        }
    }

def create_build_job(build_script, wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, jdk_version, is_multi_jdk_required) {
    return {
        def stage_name = "${os_platform_name}-${product_profile_docker_home}"
        if (is_multi_jdk_required == true) {
            stage_name = "${stage_name}-${jdk_version}"
        }
        stage("${stage_name}"){
            stage("Build ${stage_name}") {
                def image_map = build_script.image_build_handler(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, jdk_version, is_multi_jdk_required)
                stage("Push ${stage_name}") {
                    withCredentials([usernamePassword(credentialsId: 'docker-registry', passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USERNAME')]) {
                        sh 'docker login docker.wso2.com -u $REGISTRY_USERNAME -p $REGISTRY_PASSWORD'
                    }
                        build_script.push_images(image_map)
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
