/*
* Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

def call() {
    def SCRIPT_FILE_LOCATION = "org/wso2/ie/scripts"

    pipeline {
        agent {
            label 'PRODUCT-DOCKER'
        }
        environment {
            WSO2_UPDATES_SKIP_CONFLICTS = "true"
            WSO2_UPDATES_SKIP_MIGRATIONS = "true"
        }

        stages {
            stage('clean-workspace') {
                steps {
                    deleteDir()
                }
            }

            stage('download-cc-u2-dist-from-s3') {
                steps {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'aws-s3-wso2-installers-resources',passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
                            sh """
                                export WSO2_PRODUCT=$wso2_product
                                export WSO2_PRODUCT_VERSION='$cc_version'
                                export DIST_NAME=\${WSO2_PRODUCT}-\${WSO2_PRODUCT_VERSION}
                                aws s3 cp --quiet s3://wso2-installers-resources/updates2.0/\${WSO2_PRODUCT}/\${WSO2_PRODUCT_VERSION}/\${DIST_NAME}.zip .
                                unzip -q \$DIST_NAME.zip
                                if [ \$WSO2_PRODUCT_VERSION = "0.9.0" ]; then
                                    mkdir -p \$DIST_NAME/updates
                                    echo \$DIST_NAME > \$DIST_NAME/updates/product.txt
                                fi
                                rm -rf \${WSO2_PRODUCT}-\${WSO2_PRODUCT_VERSION}.zip
                            """
                        }
                    }
                }
            }

            stage('update-dist') {
                steps {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'docker-image-build', passwordVariable: 'WUM_PASSWORD', usernameVariable: 'WUM_USERNAME')]) {
                            def statusCode = sh(
                                script: """
                                    export WSO2_PRODUCT=$wso2_product
                                    export WSO2_PRODUCT_VERSION=$cc_version
                                    chmod +x \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux
                                    \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux version
                                    export UPDATE_LEVEL='$update_level'
                                    if [ $UPDATE_LEVEL -gt 0 ]; then
                                        \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --level '$UPDATE_LEVEL' --backup ./
                                    else
                                        \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --backup ./
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
                                    script:"""
                                        export WSO2_PRODUCT=$wso2_product
                                        export WSO2_PRODUCT_VERSION=$cc_version
                                        chmod +x \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux
                                        \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux version
                                        export UPDATE_LEVEL='$update_level'
                                        if [ $UPDATE_LEVEL -gt 0 ];  then
                                            \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --level '$UPDATE_LEVEL' --no-backup
                                        else
                                            \$WSO2_PRODUCT-\$WSO2_PRODUCT_VERSION/bin/wso2update_linux --username '$WUM_USERNAME' --password '$WUM_PASSWORD' --no-backup
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

            stage('download-dependencies') {
                when {
                    expression { cc_version == '0.9.0'}
                }
                steps {
                    script {
                        sh '''
                            echo "Downloading grpc health probe tool"
                            curl https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v0.3.6/grpc_health_probe-linux-amd64 --output grpc_health_probe-linux-amd64
                        '''
                    }
                }
            }

            stage('prepare-build-context') {
                steps {
                    sh '''
                        export PRODUCT_NAME=''' + wso2_product + '''
                        export PRODUCT_VERSION=''' + cc_version + '''
                        PRODUCT_NAME_WITH_VERSION=$PRODUCT_NAME-$PRODUCT_VERSION
                        cd $PRODUCT_NAME_WITH_VERSION
        
                        echo "Preparing Adapter Context"
                        mkdir -p ../adapter/maven
                        if [ $PRODUCT_VERSION = "0.9.0" ]; then
                            cp -r ADAPTER_HOME/* ../adapter/maven
                            mv ../adapter/maven/adapter ../adapter/maven/adapter-ubuntu
                            mv ../adapter/maven/check_health.sh ../adapter
                            cp ../grpc_health_probe-linux-amd64 ../adapter/maven
                            cp Dockerfile/adapter/Dockerfile* ../adapter/maven
                        elif [ $PRODUCT_VERSION = "1.0.0" ]; then
                            cp -r ADAPTER/\\\$HOME/* ../adapter/maven
                            cp ADAPTER/bin/grpc_health_probe-linux-amd64 ../adapter/maven
                            cp ADAPTER/Dockerfile* ../adapter/maven
                        fi
                        chmod +x ../adapter/maven/grpc_health_probe-linux*
                        chmod +x ../adapter/maven/adapter*
        
                        echo "Preparing Enforcer Context"
                        mkdir -p ../enforcer/maven
                        if [ $PRODUCT_VERSION = "0.9.0" ]; then
                            cp -r ENFORCER_HOME/* ../enforcer/maven
                            mv ../enforcer/maven/lib/* ../enforcer/maven/
                            mv ../enforcer/maven/check_health.sh ../enforcer
                            cp ../grpc_health_probe-linux-amd64 ../enforcer/maven
                            cp Dockerfile/enforcer/Dockerfile* ../enforcer/maven
                        elif [ $PRODUCT_VERSION = "1.0.0" ]; then
                            cp -r ENFORCER/\\\$HOME/* ../enforcer/maven
                            cp ENFORCER/bin/grpc_health_probe-linux-amd64 ../enforcer/maven
                            cp ENFORCER/Dockerfile* ../enforcer/maven
                        fi
                        chmod +x ../enforcer/maven/grpc_health_probe-linux*
                        
                        echo "Preparing Router Context"
                        mkdir -p ../router/maven/security/truststore/
                        if [ $PRODUCT_VERSION = "0.9.0" ]; then
                            cp -r ROUTER_HOME/* ../router/maven
                            cp Dockerfile/router/Dockerfile* ../router/maven
                        elif [ $PRODUCT_VERSION = "1.0.0" ]; then
                            cp -r ROUTER/\\\$HOME/* ../router/maven
                            cp ROUTER/LICENSE.txt ../router/maven
                            cp ROUTER/etc/ssl/certs/ca-certificates.crt ../router/maven/security/truststore/ca-certificates.crt
                            cp ROUTER/etc/envoy/envoy.yaml ../router/maven/envoy.yaml
                            cp ROUTER/Dockerfile* ../router/maven
                        fi
                        
                        cd -
                    '''
                }
            }

            stage('build-and-push-images') {
                steps {
                    script {
                        common_script = new CommonUtils()
                        def build_jobs = [:]
                        // uncomment ubuntu when pushing arm64 updates to customers is required
                        def os_platforms = ["alpine"]//, "ubuntu"]
                        def cc_components = ["adapter", "enforcer", "router"]
                        def update_level = common_script.get_update_level(wso2_product, cc_version)
                        if (update_level.toInteger() < 0) {
                            update_level = '';
                        }

                        for (os_platform in os_platforms) {
                            for (component in cc_components) {
                                build_jobs["$component-$cc_version:$os_platform"] = create_build_job(component, update_level, os_platform, "$component")
                            }
                        }
                        parallel build_jobs
                    }
                }
            }

            stage('Scan') {
                steps {
                    script {
                        common_script = new CommonUtils()
                        def update_level = common_script.get_update_level(wso2_product, cc_version)
                        if (update_level.toInteger() < 0) {
                            update_level = 0;
                        }
                        // create file to add the product and its vulnerability scan report
                        image_scan_script = libraryResource "${SCRIPT_FILE_LOCATION}/image-scan.sh"
                        writeFile file: './image-scan.sh', text: image_scan_script
                        sh """
                        rm -rf scanResult.txt
                        chmod +x ${WORKSPACE}/image-scan.sh
                        ${WORKSPACE}/image-scan.sh $wso2_product-adapter $cc_version.$update_level false
                        ${WORKSPACE}/image-scan.sh $wso2_product-enforcer $cc_version.$update_level false
                        ${WORKSPACE}/image-scan.sh $wso2_product-router $cc_version.$update_level false
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

                    send("[ ${currentBuild.currentResult} ] in Docker Image Build for U2 : ${wso2_product}-${cc_version} - #${env.BUILD_NUMBER}", """
                    <font color="black"><b>--------Build Info--------</b></font></p><br>
                    <b>Product</b> : ${wso2_product}<br>
                    <b>Version</b> : ${cc_version}<br>
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
                    sh "chmod +x ${WORKSPACE}/cleanup.sh"
                    sh "${WORKSPACE}/cleanup.sh $wso2_product $cc_version"
                }
            }
        }
    }
}

def create_build_job(cc_component, update_level, platform, build_context) {
    return {
        def PRIVATE_DOCKER_REGISTRY = "docker.wso2.com"
        def image_map = [:]
        def repository = "$wso2_product-$cc_component"
        def product_with_version = "$wso2_product-$cc_version"
        def image_prefix = "${PRIVATE_DOCKER_REGISTRY}/${repository}"
        def stage_name = "choreo-connect-$cc_component-$cc_version.$update_level:$platform"
        def common_script = new CommonUtils()

        stage("Build ${stage_name}") {
            def image
            def image_tags = common_script.generate_tags_update_2(wso2_product, cc_version, platform, null, update_level, null, false)
            for (def image_tag in image_tags) {
                println("Tag: $image_tag")
            }
            if (platform == "alpine") {
                image = docker.build("${image_prefix}:${image_tags[0]}", "--label update_level=${update_level} -f $build_context/maven/Dockerfile ${build_context}")
            }
            // currently disabled since ubuntu arm64 image is published just for try out purposes
//            else {
//                image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg TARGETARCH=arm64 --label update_level=${update_level} -f $build_context/Dockerfile.ubuntu ${build_context}")
//            }
            image_map.put(image, image_tags)

            stage("Push ${stage_name}") {
                withCredentials([usernamePassword(credentialsId: 'docker-registry', passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USERNAME')]) {
                    sh 'docker login docker.wso2.com -u $REGISTRY_USERNAME -p $REGISTRY_PASSWORD'
                }
                common_script.push_images(image_map)
            }
        }
    }
}
