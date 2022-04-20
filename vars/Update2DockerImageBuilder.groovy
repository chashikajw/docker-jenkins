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
    def update_level
    def dependencies

    def build_jobs = [:]
    pipeline {
        agent {
            label 'AWS02'
        }
        environment {
            PATH = "/usr/local/wum/bin:$PATH"
            WSO2_UPDATES_SKIP_CONFLICTS = "true"
            WSO2_UPDATES_SKIP_MIGRATIONS = "true"
        }
        stages {
            stage('clean-workspace') {
                steps {
                    deleteDir()
                }
            }
            stage('download-product-packs-from-s3') {
                steps {
                    script {
                        //withCredentials([usernamePassword(credentialsId: 'aws-s3-wso2-installers-resources',passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
                            sh """
                            export WSO2_PRODUCT='$wso2_product'
                            export WSO2_PRODUCT_VERSION='$wso2_product_version'
                            aws s3 cp --quiet s3://wso2-installers-resources/updates2.0/${WSO2_PRODUCT}/${WSO2_PRODUCT_VERSION}/${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}.zip .
                            unzip -q ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}.zip
                            rm -rf ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}.zip
                            """
//                        }
                    }
                }
            }
            stage('download-ob-certs-from-s3') {
                when {
                    // Download OB certs for OB accelerators
                    expression { wso2_product == 'wso2-obiam-accelerator' || wso2_product == 'wso2-obam-accelerator'}
                }
                steps {
                    script {
                        sh """
                            aws s3 cp --quiet s3://wso2-installers-resources/updates2.0/ob-cert.zip .
                            """
                    }
                }
            }
            stage('download-dependencies') {
                when {
                    // Download required dependencies for OB accelerators
                    expression { wso2_product == 'wso2-obiam-accelerator' }
                }
                steps {
                    script {
                        common_script = new CommonUtils()
                        product_profile_docker_homes = common_script.get_product_docker_home_update2(wso2_product, wso2_product_version)
                        dependencies = common_script.get_dependencies_update2(wso2_product, wso2_product_version, product_key, product_profile_docker_homes)
                        for (dependency in dependencies) {
                            def download_url = dependency.download_url
                            sh "echo $download_url"
                            sh "wget $download_url"
                        }
                    }
                }
            }
            stage('update-product-pack') {
                steps {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'docker-image-build', passwordVariable: 'WUM_PASSWORD', usernameVariable: 'WUM_USERNAME')]) {
                            def statusCode = sh(
                                    script: """
                                    chmod +x $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux
                                    $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux version
                                    export UPDATE_LEVEL='$update_level'
                                    if [ $UPDATE_LEVEL -gt 0 ];
                                    then
                                        $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --level '$UPDATE_LEVEL' --backup ./
                                    else
                                        $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --backup ./
                                    fi
                                    """,
                                    returnStatus: true)
                            if (statusCode == 0) {
                                echo 'exit-code(0): Operation successful'
                                currentBuild.result = 'SUCCESS'
                            } else if (statusCode == 1) {
                                echo 'exit-code(1): Default error'
                                currentBuild.result = 'FAILURE'
                                sh "exit 1"
                            } else if (statusCode == 2) {
                                echo 'exit-code(2): Self update'
                                statusCode = sh(
                                        script: """
                                    chmod +x $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux
                                    $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux version
                                    export UPDATE_LEVEL='$update_level'
                                    if [ $UPDATE_LEVEL -gt 0 ];
                                    then
                                        $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --level '$UPDATE_LEVEL' --no-backup
                                    else
                                        $WSO2_PRODUCT-$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --no-backup
                                    fi
                                    """,
                                        returnStatus: true)
                                echo 'Retrying'
                                if (statusCode == 0) {
                                    echo 'exit-code(0): Operation successful'
                                    currentBuild.result = 'SUCCESS'
                                } else {
                                    currentBuild.result = 'FAILURE'
                                    sh "exit 1"
                                }
                            } else if (statusCode == 3) {
                                echo 'exit-code(3): Conflict(s) encountered'
                                currentBuild.result = 'FAILURE'
                                sh "exit 1"
                            } else if (statusCode == 4) {
                                echo 'exit-code(4): Reverted'
                                currentBuild.result = 'FAILURE'
                                sh "exit 1"
                            } else {
                                echo 'Unknown exit code'
                                currentBuild.result = 'FAILURE'
                                sh "exit 1"
                            }
                        }
                    }
                }
            }
            stage('host-packs-locally') {
                steps {
                    script {
                        sh """
                            export WSO2_PRODUCT='$wso2_product'
                            export WSO2_PRODUCT_VERSION='$wso2_product_version'

                            rm -rf ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}/bin/update_darwin ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}/bin/update_linux ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}/bin/update_windows.exe
                            rm -rf ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}/updates/wum

                            zip -rq ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}.zip ${WSO2_PRODUCT}-${WSO2_PRODUCT_VERSION}
                            
                            python -m SimpleHTTPServer 8889 &
                        """
                    }
                }
            }
            stage('build-and-push-image') {
                steps {
                    script {
                        build_script = new CommonUtils()
                        product_profile_docker_homes = build_script.get_product_docker_home_update2(wso2_product, wso2_product_version)
                        build_script.get_docker_release_version_update2(wso2_product, wso2_product_version, product_key, product_profile_docker_homes)
                        update_level = build_script.get_update_level(wso2_product, wso2_product_version)
                        if (product_key == "open-banking") {
                            os_platforms = [alpine: '3.10', ubuntu: '18.04']
                        } else {
                            os_platforms = [alpine: '3.10', ubuntu: '18.04', centos: '7']
                        }
                        multi_jdk_required = build_script.get_multi_jdk_required()

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
                        writeFile file: './image-scan.sh', text: image_scan_script
                        sh """
                        rm -rf scanResult.txt
                        chmod +x ${WORKSPACE}/image-scan.sh
                        ${WORKSPACE}/image-scan.sh $wso2_product $wso2_product_version.$update_level $multi_jdk_required
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

                    send("[ ${currentBuild.currentResult} ] in Docker Image Build for U2 : ${wso2_product}-${wso2_product_version} - #${env.BUILD_NUMBER}", """
                    <font color="black"><b>--------Build Info--------</b></font></p><br>
                    <b>Product</b> : ${wso2_product}<br>
                    <b>Version</b> : ${wso2_product_version}<br>
                    <b>Build Status</b> : ${currentBuild.currentResult}<br>
                    <b>Docker Registry</b> : https://docker.wso2.com/tags.php?repo=${wso2_product}<br>
                    <font color="black"><b>--------Docker Image Vulnerability Scan Report--------</b></font></p><br>
                    <pre>
                    ${emailBodyScan}
                    </pre>
                    </p><br>
                    <p>Check console output at ${BUILD_URL} to view the results.</p>
                    """)

                    cleanup_script = libraryResource "${SCRIPT_FILE_LOCATION}/u2-cleanup.sh"
                    writeFile file: './cleanup.sh', text: cleanup_script
                    sh 'chmod +x ${WORKSPACE}/cleanup.sh'
                    sh '${WORKSPACE}/cleanup.sh $wso2_product $wso2_product_version'
                }
            }
        }
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
                def image_map
                if (wso2_product == "wso2-obiam-accelerator") {
                    dependencies = build_script.get_dependencies_update2(wso2_product, wso2_product_version, product_key, product_profile_docker_homes)
                    image_map = build_script.image_build_handler_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, jdk_version, is_multi_jdk_required, dependencies)
                } else {
                    image_map = build_script.image_build_handler_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, jdk_version, is_multi_jdk_required)
                }
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
