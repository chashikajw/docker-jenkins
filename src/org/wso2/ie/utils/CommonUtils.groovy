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
package org.wso2.ie.utils
def DOCKER_RESOURCES_GIT_RELEASE_TAG
def latest_version
def DOCKER_RESOURCE_GIT_REPO_NAME
def CONF_FILE_LOCATION
def MULTI_JDK_REQUIRED

def get_product_docker_home_update2(wso2_product, wso2_product_version) {
    println "Getting product Docker Homes..."
    def product_profile_docker_homes
    switch(wso2_product) {
        case "wso2am":
            product_profile_docker_homes = ["apim"]
            break
        case "wso2am-analytics":
            product_profile_docker_homes = ["apim-analytics/dashboard", "apim-analytics/worker"]
            break
        case "wso2is-km":
            product_profile_docker_homes = ["is-as-km"]
            break
        case "wso2am-micro-gw":
            product_profile_docker_homes = ["mg"]
            break
        case "wso2is":
            product_profile_docker_homes = ["is"]
            break
        case "wso2is-analytics":
            product_profile_docker_homes = ["is-analytics/dashboard", "is-analytics/worker"]
            break
        case "wso2ei":
            if (wso2_product_version == "6.2.0") {
                product_profile_docker_homes = ["analytics", "broker", "business-process", "integrator"]
            } else {
                product_profile_docker_homes = ["analytics/dashboard", "analytics/worker", "broker", "business-process", "integrator"]
            }
            break
        case "wso2-obam":
            product_profile_docker_homes = ["obam"]
            break
        case "wso2-obkm":
            product_profile_docker_homes = ["obkm"]
            break
        case "wso2-obiam":
            product_profile_docker_homes = ["obiam"]
            break
        case "wso2-obbi":
            product_profile_docker_homes = ["obbi/worker", "obbi/dashboard"]
            break
        case "wso2-obiam-accelerator":
            product_profile_docker_homes = ["obiam"]
            break
        case "wso2-obam-accelerator":
            product_profile_docker_homes = ["obam"]
            break
        case "wso2mi":
            product_profile_docker_homes = ["micro-integrator"]
            break
        case "wso2mi-monitoring-dashboard":
            product_profile_docker_homes = ["monitoring-dashboard"]
            break
        case "wso2mi-dashboard":
            product_profile_docker_homes = ["mi-dashboard"]
            break
        case "wso2si":
            product_profile_docker_homes = ["streaming-integrator"]
            break
        default:
            println "Product is not valid"
            break
    }

    return product_profile_docker_homes
}

def get_product_docker_home(wso2_product, wso2_product_version) {
    println "Getting product Docker Homes..."
    def product_profile_docker_homes
    switch(wso2_product) {
        case "wso2am":
            product_profile_docker_homes = ["apim"]
            break
        case "wso2am-analytics":
            product_profile_docker_homes = ["apim-analytics/dashboard", "apim-analytics/worker"]
            break
        case "wso2is-km":
            product_profile_docker_homes = ["is-as-km"]
            break
        case "wso2am-micro-gw":
            product_profile_docker_homes = ["mg"]
            break
        case "wso2is":
            product_profile_docker_homes = ["is"]
            break
        case "wso2is-analytics":
            product_profile_docker_homes = ["is-analytics/dashboard", "is-analytics/worker"]
            break
        case "wso2ei":
            if (wso2_product_version == "6.2.0") {
                product_profile_docker_homes = ["analytics", "broker", "business-process", "integrator", "msf4j"]
            } else if (wso2_product_version == "6.6.0") {
                product_profile_docker_homes = ["analytics/dashboard", "analytics/worker", "broker", "business-process", "integrator"]
            } else {
                product_profile_docker_homes = ["analytics/dashboard", "analytics/worker", "broker", "business-process", "integrator", "msf4j"]
            }
            break
        case "wso2-obam":
            product_profile_docker_homes = ["obam"]
            break
        case "wso2-obkm":
            product_profile_docker_homes = ["obkm"]
            break
        case "wso2-obiam":
            product_profile_docker_homes = ["obiam"]
            break
        case "wso2-obbi":
            product_profile_docker_homes = ["obbi/worker", "obbi/dashboard"]
            break
        case "wso2mi":
            product_profile_docker_homes = ["micro-integrator"]
            break
        case "wso2mi-monitoring-dashboard":
            product_profile_docker_homes = ["monitoring-dashboard"]
            break
        case "wso2mi-dashboard":
            product_profile_docker_homes = ["mi-dashboard"]
            break
        case "wso2si":
            product_profile_docker_homes = ["streaming-integrator"]
            break
        default:
            println "Product is not valid"
            break
    }

    return product_profile_docker_homes
}

def get_docker_release_version(wso2_product, wso2_product_version, product_key, product_profile_docker_homes) {
    println "Getting Docker Release Version..."
    CONF_FILE_LOCATION = "org/wso2/ie/conf"
    product_data_script = libraryResource "${CONF_FILE_LOCATION}/${product_key}-data.json"
    writeFile file: "./${product_key}-data.json", text: product_data_script
    config_file = readJSON file: "${product_key}-data.json"
    product_profile_docker_home = product_profile_docker_homes[0]
    def result = config_file.profiles.find{ it.product == wso2_product && it.product_profile_docker_home == product_profile_docker_home }?.versions?.find{ it.product_version == wso2_product_version }
    DOCKER_RESOURCES_GIT_RELEASE_TAG = result.docker_release_version
    latest_version = result.latest
    MULTI_JDK_REQUIRED = result.multi_jdk_required
}

def get_docker_release_version_update2(wso2_product, wso2_product_version, product_key, product_profile_docker_homes) {
    println "Getting Docker Release Version..."
    CONF_FILE_LOCATION = "org/wso2/ie/conf/update2"
    product_data_script = libraryResource "${CONF_FILE_LOCATION}/${product_key}-data.json"
    writeFile file: "./${product_key}-data.json", text: product_data_script
    config_file = readJSON file: "${product_key}-data.json"
    product_profile_docker_home = product_profile_docker_homes[0]
    def result = config_file.profiles.find{ it.product == wso2_product && it.product_profile_docker_home == product_profile_docker_home }?.versions?.find{ it.product_version == wso2_product_version }
    DOCKER_RESOURCES_GIT_RELEASE_TAG = result.docker_release_version
    latest_version = result.latest
    MULTI_JDK_REQUIRED = result.multi_jdk_required
}

def get_dependencies_update2(wso2_product, wso2_product_version, product_key, product_profile_docker_homes) {
    println "Getting Docker Release Version..."
    CONF_FILE_LOCATION = "org/wso2/ie/conf/update2"
    product_data_script = libraryResource "${CONF_FILE_LOCATION}/${product_key}-data.json"
    writeFile file: "./${product_key}-data.json", text: product_data_script
    config_file = readJSON file: "${product_key}-data.json"
    product_profile_docker_home = product_profile_docker_homes[0]
    def result = config_file.profiles.find{ it.product == wso2_product && it.product_profile_docker_home == product_profile_docker_home }?.versions?.find{ it.product_version == wso2_product_version }
    return result.dependencies
}

def get_latest_wum_timestamp(wso2_product_profile, wso2_product_version) {
    println "Getting WUM Timestamp from config..."
    UPDATED_PRODUCT_PACK_LOCATION = "${WORKSPACE}/product-packs"
    unstash 'properties'
    def props = readProperties  file:'timestamp.properties'
    def wum_timestamp = props['wum_timestamp']
    if (wum_timestamp == "GA") {
        return ''
    }
    return wum_timestamp
}

def get_multi_jdk_required() {
    return MULTI_JDK_REQUIRED
}

def get_docker_resources_git_release_tag() {
    return DOCKER_RESOURCES_GIT_RELEASE_TAG
}

def build_image(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp,
                image_tags, product_key, jdk_version, is_multi_jdk_required) {
    println "Building image..."
    DOCKER_RESOURCE_GIT_REPO_NAME = "docker-${product_key}"
    UPDATED_PRODUCT_PACK_HOST_LOCATION_URL = "http://172.17.0.1:8888"
    PRIVATE_DOCKER_REGISTRY = "docker.wso2.com"

    def result = config_file.profiles.find{ it.product_profile_docker_home == product_profile_docker_home }
    def profile = result.docker_repo_name
    def image_prefix = "${PRIVATE_DOCKER_REGISTRY}/${profile}"

    def docker_build_context
    if (is_multi_jdk_required == true) {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${jdk_version}/${os_platform_name}/${product_profile_docker_home}"
    } else {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${os_platform_name}/${product_profile_docker_home}"
    }
    def image
    if (wum_timestamp == '') {
        image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip -f Dockerfile ${docker_build_context}")
    }
    else {
        image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip -f Dockerfile ${docker_build_context}")
    }
    return image
}

def build_image_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
                        wum_timestamp, image_tags, product_key, update_level, jdk_version, is_multi_jdk_required) {
    println "Building image..."
    DOCKER_RESOURCE_GIT_REPO_NAME = "docker-${product_key}"
    UPDATED_PRODUCT_PACK_HOST_LOCATION_URL = "http://172.17.0.1:8889"
    PRIVATE_DOCKER_REGISTRY = "docker.wso2.com"

    def result = config_file.profiles.find{ it.product_profile_docker_home == product_profile_docker_home }
    def profile = result.docker_repo_name
    def image_prefix = "${PRIVATE_DOCKER_REGISTRY}/${profile}"

    def docker_build_context
    if (is_multi_jdk_required == true) {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${jdk_version}/${os_platform_name}/${product_profile_docker_home}"
    } else {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${os_platform_name}/${product_profile_docker_home}"
    }
    def image
    if (wum_timestamp == '') {
        if (wso2_product == "wso2-obam-accelerator") {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_OB_Accelerator_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip --build-arg WSO2_OB_CERTS_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/ob-cert.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        } else {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        }
    } else {
        if (wso2_product == "wso2-obam-accelerator") {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_OB_Accelerator_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip --build-arg WSO2_OB_CERTS_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/ob-cert.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        } else {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        }
    }
    return image
}

def build_image_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
                        wum_timestamp, image_tags, product_key, update_level, jdk_version, is_multi_jdk_required, dependencies) {
    println "Building image..."
    DOCKER_RESOURCE_GIT_REPO_NAME = "docker-${product_key}"
    UPDATED_PRODUCT_PACK_HOST_LOCATION_URL = "http://172.17.0.1:8889"
    PRIVATE_DOCKER_REGISTRY = "docker.wso2.com"

    def result = config_file.profiles.find{ it.product_profile_docker_home == product_profile_docker_home }
    def profile = result.docker_repo_name
    def image_prefix = "${PRIVATE_DOCKER_REGISTRY}/${profile}"

    def docker_build_context
    if (is_multi_jdk_required == true) {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${jdk_version}/${os_platform_name}/${product_profile_docker_home}"
    } else {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${os_platform_name}/${product_profile_docker_home}"
    }
    def image
    if (wum_timestamp == '') {
        if (wso2_product == "wso2-obiam-accelerator") {
            def is_extensions = dependencies[0].name
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_OB_Accelerator_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip --build-arg WSO2_OB_KEYMANAGER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${is_extensions}.zip --build-arg WSO2_OB_CERTS_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/ob-cert.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        } else {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        }
    } else {
        if (wso2_product == "wso2-obiam-accelerator") {
            def is_extensions = dependencies[0].name
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_OB_Accelerator_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip --build-arg WSO2_OB_KEYMANAGER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${is_extensions}.zip --build-arg WSO2_OB_CERTS_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/ob-cert.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        } else {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        }
    }
    return image
}

def generate_tags_update_2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
                           update_level, jdk_version, is_multi_jdk_required) {
    println "Generating tags..."
    def image_tags = []
    // add stable image tags
    def stable_version_tag = wso2_product_version + ".0"

    if (wso2_product == "wso2am-micro-gw" || wso2_product == "choreo-connect") {
//        if (latest_version && os_platform_name == "alpine") {
//            image_tags.add("latest")
//        }
        // add OS platform name
        if (os_platform_name != "alpine") {
            stable_version_tag ="${stable_version_tag}-${os_platform_name}"
        }
        image_tags.add(stable_version_tag)
        // add a unique tag
        if (update_level != '') {
            def unique_tag = "${wso2_product_version}.${update_level}"
            if (os_platform_name != "alpine"){
                unique_tag = "${unique_tag}-${os_platform_name}"
            }
            image_tags.add(unique_tag)
        }
    } else {
//        if (latest_version && os_platform_name == "ubuntu") {
//            image_tags.add("latest")
//        }
        if (latest_version && os_platform_name == "ubuntu" && (wso2_product == "wso2is" || wso2_product == "wso2am" || wso2_product == "wso2-obiam-accelerator" || wso2_product == "wso2-obam-accelerator")) {
            if (is_multi_jdk_required != true) {
                image_tags.add("latest")
            }
        }
        // add OS platform name
        if (os_platform_name != "ubuntu") {
            stable_version_tag ="${stable_version_tag}-${os_platform_name}"
        }
        if (is_multi_jdk_required == true) {
            stable_version_tag = "${stable_version_tag}-${jdk_version}"
        }
        image_tags.add(stable_version_tag)

        // add a unique tag
        if (update_level != '') {
            def unique_tag = "${wso2_product_version}.${update_level}"
            if (os_platform_name != "ubuntu"){
                unique_tag = "${unique_tag}-${os_platform_name}"
            }
            if (is_multi_jdk_required == true) {
                unique_tag = "${unique_tag}-${jdk_version}"
            }
            image_tags.add(unique_tag)
        }
    }

    return image_tags
}

def generate_tags(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp, jdk_version, is_multi_jdk_required) {
    println "Generating tags..."
    def image_tags = []
    // add stable image tags
    def stable_version_tag = wso2_product_version
    def release_version = DOCKER_RESOURCES_GIT_RELEASE_TAG.split(/\./)[3]

    if (wso2_product == "wso2am-micro-gw") {
        if (latest_version && os_platform_name == "alpine") {
            image_tags.add("latest")
        }
        // add OS platform name
        if (os_platform_name != "alpine") {
            stable_version_tag ="${stable_version_tag}-${os_platform_name}"
        }
        image_tags.add(stable_version_tag)
        // add a unique tag
        if (wum_timestamp != '') {
            def unique_tag = "${wso2_product_version}.${wum_timestamp}.${release_version}"
            if (os_platform_name != "alpine"){
                unique_tag = "${unique_tag}-${os_platform_name}"
            }
            image_tags.add(unique_tag)
        }
    } else {
        if (latest_version && os_platform_name == "ubuntu" && wso2_product != "wso2is" && wso2_product != "wso2am") {
            if (is_multi_jdk_required != true) {
                image_tags.add("latest")
            }
        }
        // add OS platform name
        if (os_platform_name != "ubuntu") {
            stable_version_tag ="${stable_version_tag}-${os_platform_name}"
        }
        if (is_multi_jdk_required == true) {
            stable_version_tag = "${stable_version_tag}-${jdk_version}"
        }
        image_tags.add(stable_version_tag)

        // add a unique tag
        if (wum_timestamp != '') {
            def unique_tag = "${wso2_product_version}.${wum_timestamp}.${release_version}"
            if (os_platform_name != "ubuntu"){
                unique_tag = "${unique_tag}-${os_platform_name}"
            }
            if (is_multi_jdk_required == true) {
                unique_tag = "${unique_tag}-${jdk_version}"
            }
            image_tags.add(unique_tag)
        }
    }

    return image_tags
}

def get_update_level(wso2_product, wso2_product_version) {
    def product_full_name = wso2_product + "-" + wso2_product_version
    def filepath = product_full_name + "/updates/config.json"
    def configProperties = readJSON file: filepath
    def updateLevel = configProperties['update-level']
    return updateLevel
}

def image_build_handler_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
                                product_key, jdk_version, is_multi_jdk_required) {
    def image_map = [:]
    def wum_timestamp = ''
    def update_level = get_update_level(wso2_product, wso2_product_version)
    if (update_level.toInteger() < 0) {
        update_level = '';
    }
    def image_tags = generate_tags_update_2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, update_level, jdk_version, is_multi_jdk_required)
    for (def image_tag = 0; image_tag <image_tags.size(); image_tag++){
        println(image_tags[image_tag])
    }

    def image = build_image_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
            wum_timestamp, image_tags, product_key, update_level, jdk_version, is_multi_jdk_required)
    image_map.put(image, image_tags)
    tag_images(image, image_tags)

    return image_map
}

def image_build_handler_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
                                product_key, jdk_version, is_multi_jdk_required, dependencies) {
    def image_map = [:]
    def wum_timestamp = ''
    def update_level = get_update_level(wso2_product, wso2_product_version)
    if (update_level.toInteger() < 0) {
        update_level = '';
    }
    def image_tags = generate_tags_update_2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, update_level, jdk_version, is_multi_jdk_required)
    for (def image_tag = 0; image_tag <image_tags.size(); image_tag++){
        println(image_tags[image_tag])
    }

    def image = build_image_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
            wum_timestamp, image_tags, product_key, update_level, jdk_version, is_multi_jdk_required, dependencies)
    image_map.put(image, image_tags)
    tag_images(image, image_tags)

    return image_map
}

def image_build_handler(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, jdk_version, is_multi_jdk_required) {
    def image_map = [:]
    def wum_timestamp = get_latest_wum_timestamp(wso2_product, wso2_product_version)
    def image_tags = generate_tags(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp, jdk_version, is_multi_jdk_required)
    for (def image_tag = 0; image_tag <image_tags.size(); image_tag++){
        println(image_tags[image_tag])
    }

    def image = build_image(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp, image_tags, product_key, jdk_version, is_multi_jdk_required)
    image_map.put(image, image_tags)
    tag_images(image, image_tags)

    return image_map
}

def tag_images(image, image_tags) {
    println "Tagging image..."
    for (def image_tag = 1; image_tag <image_tags.size(); image_tag++){
        image.tag(image_tags[image_tag])
    }
}
def push_images(image_map) {
    println "Pushing tagged images..."
    image_map.collectMany { image, image_name -> image_name.collect { [object: image, param: it] } }
            .each { println it.object.push(it.param) }
}

def create_trivyignore_file(wso2_product_name,wso2_product_version,os_platform_name, TRIVYIGNORE_FILE_LOCATION) {

    String os_trivyignore = libraryResource "${TRIVYIGNORE_FILE_LOCATION}/os/${os_platform_name}/.trivyignore"
    if (wso2_product_version == "5.9.0" || wso2_product_version == "5.10.0" || wso2_product_version == "5.11.0" ){
        product_trivyignore = libraryResource "${TRIVYIGNORE_FILE_LOCATION}/product/${wso2_product_name}/${wso2_product_version}/.trivyignore"
    } else {
        product_trivyignore = libraryResource "${TRIVYIGNORE_FILE_LOCATION}/product/${wso2_product_name}/default/.trivyignore"
    }
    
    String final_trivyignore = os_trivyignore + "\n" + product_trivyignore
    writeFile file: "${TRIVYIGNORE_FILE_LOCATION}/temp/.trivyignore", text: final_trivyignore
    
}

synchronized void add_images(map, wso2_product_name, os, profile, wso2_product_release_version, multi_jdk_required, jdk_version) {
    
    if (wso2_product_name == "wso2am-analytics" || wso2_product_name == "wso2is-analytics" || wso2_product_name == "obbi" ){
        if ( os == 'ubuntu'){
            map[String.format("docker.wso2.com/%s-%s:%s", wso2_product_name, profile, wso2_product_release_version)] = String.format("%s-%s-%s-%s-scanResult.txt", wso2_product_name, profile, wso2_product_release_version, os)
        } else {
            map[String.format("docker.wso2.com/%s-%s:%s-%s", wso2_product_name, profile, wso2_product_release_version, os)] = String.format("%s-%s-%s-%s-scanResult.txt", wso2_product_name, profile, wso2_product_release_version, os)
        }
    } else if (wso2_product_name == "wso2ei" && wso2_product_release_version.substring(0,2) == "6." ){
        if (os == 'ubuntu' ){
            map[String.format("docker.wso2.com/%s-%s:%s", wso2_product_name, profile, wso2_product_release_version)] = String.format("%s-%s-%s-%s-scanResult.txt", wso2_product_name, profile, wso2_product_release_version, os)
        } else {
            map[String.format("docker.wso2.com/%s-%s:%s-%s", wso2_product_name, profile, wso2_product_release_version, os)] = String.format("%s-%s-%s-%s-scanResult.txt", wso2_product_name, profile, wso2_product_release_version, os)
        }
    } else if(wso2_product_name == "wso2is" || wso2_product_name == "wso2am" ){
        if ( os == 'ubuntu' ){
            if (multi_jdk_required == true ){
                if ( jdk_version == "jdk11" ){
                    map[String.format("docker.wso2.com/%s:%s", wso2_product_name, wso2_product_release_version)] = String.format("%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os)
                } else {
                    map[String.format("docker.wso2.com/%s:%s-%s", wso2_product_name, wso2_product_release_version, jdk_version)] = String.format("%s-%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os, jdk_version)
                }
            } else {
                map[String.format("docker.wso2.com/%s:%s", wso2_product_name, wso2_product_release_version)] = String.format("%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os)
            }
        } else {
            if (multi_jdk_required == true ){
                if (jdk_version == "jdk11" ){
                    map[String.format("docker.wso2.com/%s:%s-%s", wso2_product_name, wso2_product_release_version,os)] = String.format("%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os)
                } else {
                map[String.format("docker.wso2.com/%s:%s-%s-%s", wso2_product_name, wso2_product_release_version, os, jdk_version)] = String.format("%s-%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os, jdk_version)
                }
            } else {
                map[String.format("docker.wso2.com/%s:%s-%s", wso2_product_name, wso2_product_release_version, os)] = String.format("%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os)
            }
        }  
    } else {
        if (os == 'ubuntu' ){
            map[String.format("docker.wso2.com/%s:%s", wso2_product_name, wso2_product_release_version)] = String.format("%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os)
        } else {
            map[String.format("docker.wso2.com/%s:%s-%s", wso2_product_name, wso2_product_release_version, os)] = String.format("%s-%s-%s-scanResult.txt", wso2_product_name, wso2_product_release_version, os)
        }
    }
}

def get_severity(wso2_product, wso2_product_version){
    if(wso2_product == "wso2is" ){
        if (wso2_product_version == "5.11.0"){
            return "MEDIUM,HIGH,CRITICAL"
        } else if (wso2_product_version == "5.10.0"){
            return "MEDIUM,HIGH,CRITICAL"    
        } else if (wso2_product_version == "5.9.0"){
            return "MEDIUM,HIGH,CRITICAL"        
        } else {
            return "MEDIUM,HIGH,CRITICAL"        
        }
    } else {
        return "MEDIUM,HIGH,CRITICAL"
    }
}

def generateSummary(severity){

    final_arr = []
    var = readFile("summaryText.txt")
    String outFile = ""

    arr = var.split("\n")
    max_len = 0
    for (int i in 0..arr.size()-1){
        final_arr.push(arr[i].split())
        if(final_arr[-1][-1].toInteger()>max_len){
            max_len = final_arr[-1][-1].toInteger()
        }
    }

    outFile = generateTitleRow(max_len, severity, outFile)
    for (int i in 0..final_arr.size()-1){
        outFile = generateRows(final_arr[i], max_len, severity, outFile)
    }
    writeFile(file: 'summaryOut.txt', text: outFile)
}

def generateTitleRow(max_len, severity, outFile){

    if (severity=="CRITICAL"){
        outFile+=("+" + "-".multiply(max_len+2) + "+" + "-".multiply(8) + "+" + "-".multiply(12) + "+" + "-".multiply(7) + "+" + "-".multiply(10) + "+\n")
        outFile+=("|" + " "*(max_len-5).intdiv(2) + "Release" + " "*(max_len-5-(max_len-5).intdiv(2)) + "|" + " ".multiply(3) + "OS" + " ".multiply(3) + "|" + " " + " " + "VUL. TYPE" + " ".multiply(2) + "|" + "TOTAL" + " " + "|" + " " + "CRITICAL" + " " + "|\n")
        outFile+=("+" + "-".multiply(max_len+2) + "+" + "-".multiply(8) + "+" + "-".multiply(12) + "+" + "-".multiply(7) + "+" + "-".multiply(10) + "+\n")
    } else if (severity == "HIGH,CRITICAL"){
        outFile+=("+" + "-".multiply(max_len+2) + "+" + "-".multiply(8) + "+" + "-".multiply(12) + "+" + "-".multiply(7) + "+" + "-".multiply(10) + "+" + "-".multiply(6) + "+\n")
        outFile+=("|" + " "*(max_len-5).intdiv(2) + "Release" + " "*(max_len-5-(max_len-5).intdiv(2)) + "|" + " ".multiply(3) + "OS" + " ".multiply(3) + "|" + " " + "VUL. TYPE" + " ".multiply(2) + "|" + " " + "TOTAL" + " " + "|" + " " + "CRITICAL" + " " + "|" + " " + "HIGH" + " " + "|\n")
        outFile+=("+" + "-".multiply(max_len+2) + "+" + "-".multiply(8) + "+" + "-".multiply(12) + "+" + "-".multiply(7) + "+" + "-".multiply(10) + "+" + "-".multiply(6) + "+\n")
    } else if(severity == "MEDIUM,HIGH,CRITICAL"){
        outFile+=("+" + "-".multiply(max_len+2) + "+" + "-".multiply(8) + "+" + "-".multiply(12) + "+" + "-".multiply(7) + "+" + "-".multiply(10) + "+" + "-".multiply(6) + "+" + "-".multiply(8) + "+\n")
        outFile+=("|" + " "*(max_len-5).intdiv(2) + "Release" + " "*(max_len-5-(max_len-5).intdiv(2))+"|" + " ".multiply(3)+"OS" + " ".multiply(3)+"|" + " " + "VUL. TYPE" + " ".multiply(2) + "|" + " " + "TOTAL" + " " + "|" + " " + "CRITICAL" +" " + "|" + " " + "HIGH" + " " + "|" + " " + "MEDIUM" + " " + "|\n")
        outFile+=("+" + "-".multiply(max_len+2) + "+" + "-".multiply(8) + "+" + "-".multiply(12) + "+" + "-".multiply(7) + "+" + "-".multiply(10) + "+" + "-".multiply(6) + "+" + "-".multiply(8) + "+\n")
    }

    return outFile
}        

def generateCell(length,data){
    arr = []
    spacing = (length-data.length()).intdiv(2)

    arr.push(" ".multiply(spacing) + data + " ".multiply(length-spacing-data.length()) + "|")
    arr.push("-".multiply(length) + "+")

    return arr
}

def generateReleaseCell(tag, os, length, max_len){

    arr = []
    arr.push("|" + " ".multiply(max_len+2) + "|" + " ".multiply(8) + "|" + " ".multiply(5) + "OS" + " ".multiply(5) + "|")
    arr.push("|" + " ".multiply(max_len+2) + "|" + " ".multiply(8) + "+" + "-".multiply(12) + "+")
    arr.push("|" + " ".multiply(((max_len-length.toInteger()).intdiv(2))+1) + tag + " ".multiply(1 + max_len - ((max_len - length.toInteger()).intdiv(2)) - length.toInteger()) + "|" + " "+os.toUpperCase() + " " + "|" + " ".multiply(4) + "APP" + " ".multiply(5) + "|")
    arr.push("|" + " ".multiply(max_len+2) + "|" + " ".multiply(8) + "+" + "-".multiply(12) + "+")
    arr.push("|" + " ".multiply(max_len+2) + "|" + " ".multiply(8) + "|" + " " + "INSTALLERS" + " " + "|")
    arr.push("+" + "-".multiply(max_len+2) + "+" + "-".multiply(8) + "+" + "-".multiply(12) + "+")
    return arr
}


def generateRows(row_data, max_len, severity, outFile){

    release_cell = generateReleaseCell(row_data[0], row_data[1], row_data[-1], max_len)

    if (severity == "CRITICAL"){

        os_total = generateCell(7, row_data[3])
        os_critical = generateCell(10, row_data[4])

        app_total = generateCell(7, row_data[5])
        app_critical = generateCell(10, row_data[6])

        installer_total = generateCell(7, row_data[7])
        installer_critical = generateCell(10,row_data[8])

        outFile += (release_cell[0] + os_total[0] + os_critical[0] + "\n")
        outFile += (release_cell[1] + os_total[1] + os_critical[1] + "\n")
        outFile += (release_cell[2] + app_total[0] + app_critical[0] + "\n")
        outFile += (release_cell[3] + app_total[1] + app_critical[1] + "\n")
        outFile += (release_cell[4] + installer_total[0] + installer_critical[0] + "\n")
        outFile += (release_cell[5] + installer_total[1] + installer_critical[1] + "\n")
    }
    else if (severity == "HIGH,CRITICAL"){

        os_total = generateCell(7, row_data[3])
        os_critical = generateCell(10, row_data[5])
        os_high  = generateCell(6, row_data[4])

        app_total = generateCell(7, row_data[6])
        app_critical = generateCell(10, row_data[8])
        app_high = generateCell(6, row_data[7])

        installer_total = generateCell(7, row_data[9])
        installer_critical = generateCell(10, row_data[11])
        installer_high = generateCell(6, row_data[10])

        outFile += (release_cell[0] + os_total[0] + os_critical[0] + os_high[0] + "\n")
        outFile += (release_cell[1] + os_total[1] + os_critical[1] + os_high[1] + "\n")
        outFile += (release_cell[2] + app_total[0] + app_critical[0] + app_high[0] + "\n")
        outFile += (release_cell[3] + app_total[1] + app_critical[1] + app_high[1] + "\n")
        outFile += (release_cell[4] + installer_total[0] + installer_critical[0] + installer_high[0] + "\n")
        outFile += (release_cell[5] + installer_total[1] + installer_critical[1] + installer_high[1] + "\n")
    }
    else if (severity == "MEDIUM,HIGH,CRITICAL"){

        os_total = generateCell(7, row_data[3])
        os_critical = generateCell(10, row_data[6])
        os_high  = generateCell(6, row_data[5])
        os_medium = generateCell(8, row_data[4])

        app_total = generateCell(7, row_data[7])
        app_critical = generateCell(10, row_data[10])
        app_high = generateCell(6, row_data[9])
        app_medium = generateCell(8, row_data[8])

        installer_total = generateCell(7, row_data[11])
        installer_critical = generateCell(10, row_data[14])
        installer_high = generateCell(6, row_data[13])
        installer_medium = generateCell(8, row_data[12])

        outFile += (release_cell[0] + os_total[0] + os_critical[0] + os_high[0] + os_medium[0] + "\n")
        outFile += (release_cell[1] + os_total[1] + os_critical[1] + os_high[1] + os_medium[1] + "\n")
        outFile += (release_cell[2] + app_total[0] + app_critical[0] + app_high[0] + app_medium[0] + "\n")
        outFile += (release_cell[3] + app_total[1] + app_critical[1] + app_high[1] + app_medium[1] + "\n")
        outFile += (release_cell[4] + installer_total[0] + installer_critical[0] + installer_high[0] + installer_medium[0] + "\n")
        outFile += (release_cell[5] + installer_total[1] + installer_critical[1] + installer_high[1] + installer_medium[1] + "\n")
    }

    return outFile
}

def build_image_with_tag(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp,
                image_tags, product_key, jdk_version, is_multi_jdk_required) {
    println "Building image..."
    DOCKER_RESOURCE_GIT_REPO_NAME = "docker-${product_key}"
    UPDATED_PRODUCT_PACK_HOST_LOCATION_URL = "http://172.17.0.1:8888"
    PRIVATE_DOCKER_REGISTRY = "docker.wso2.com"

    def result = config_file.profiles.find{ it.product_profile_docker_home == product_profile_docker_home }
    def profile = result.docker_repo_name
    def image_prefix = "${PRIVATE_DOCKER_REGISTRY}/${profile}"
    def docker_tag = "${image_prefix}:${image_tags[0]}"
    def docker_build_context = "${image_prefix}:${image_tags[0]}"
    def return_profile = result.profile
    if (is_multi_jdk_required == true) {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${jdk_version}/${os_platform_name}/${product_profile_docker_home}"
    } else {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${os_platform_name}/${product_profile_docker_home}"
    }
    def image
    if (wum_timestamp == '') {
        image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip -f Dockerfile ${docker_build_context}")
    }
    else {
        image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip -f Dockerfile ${docker_build_context}")
    }
    return [image,docker_tag,return_profile]
}

def build_image_with_tag_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
                        wum_timestamp, image_tags, product_key, update_level, jdk_version, is_multi_jdk_required) {
    println "Building image..."
    DOCKER_RESOURCE_GIT_REPO_NAME = "docker-${product_key}"
    UPDATED_PRODUCT_PACK_HOST_LOCATION_URL = "http://172.17.0.1:8889"
    PRIVATE_DOCKER_REGISTRY = "docker.wso2.com"

    def result = config_file.profiles.find{ it.product_profile_docker_home == product_profile_docker_home }
    def profile = result.docker_repo_name
    def image_prefix = "${PRIVATE_DOCKER_REGISTRY}/${profile}"
    def docker_tag = "${image_prefix}:${image_tags[0]}"
    def docker_build_context
    def return_profile = result.profile
    if (is_multi_jdk_required == true) {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${jdk_version}/${os_platform_name}/${product_profile_docker_home}"
    } else {
        docker_build_context = "https://github.com/wso2/${DOCKER_RESOURCE_GIT_REPO_NAME}.git#v${DOCKER_RESOURCES_GIT_RELEASE_TAG}:/dockerfiles/${os_platform_name}/${product_profile_docker_home}"
    }
    def image
    if (wum_timestamp == '') {
        if (wso2_product == "wso2-obam-accelerator") {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_OB_Accelerator_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip --build-arg WSO2_OB_CERTS_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/ob-cert.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        } else {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        }
    } else {
        if (wso2_product == "wso2-obam-accelerator") {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_OB_Accelerator_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip --build-arg WSO2_OB_CERTS_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/ob-cert.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        } else {
            image = docker.build("${image_prefix}:${image_tags[0]}", "--build-arg WSO2_SERVER_DIST_URL=${UPDATED_PRODUCT_PACK_HOST_LOCATION_URL}/${wso2_product}-${wso2_product_version}+${wum_timestamp}.full.zip --label update_level=${update_level} -f Dockerfile ${docker_build_context}")
        }
    }
    return [image,docker_tag,return_profile]
}

def image_build_handler_with_tag_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home,
                                product_key, jdk_version, is_multi_jdk_required) {
    def image_map = [:]
    def wum_timestamp = ''
    def update_level = get_update_level(wso2_product, wso2_product_version)
    if (update_level.toInteger() < 0) {
        update_level = '';
    }
    def image_tags = generate_tags_update_2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, update_level, jdk_version, is_multi_jdk_required)
    for (def image_tag = 0; image_tag <image_tags.size(); image_tag++){
        println(image_tags[image_tag])
    }
    def image
    def docker_tag
    def return_profile
    (image,docker_tag,profile) = build_image_with_tag_update2(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp, image_tags, product_key, update_level, jdk_version, is_multi_jdk_required)
    image_map.put(image, image_tags)
    tag_images(image, image_tags)

    return [image_map,docker_tag,return_profile]
}

def image_build_handler_with_tag(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, product_key, jdk_version, is_multi_jdk_required) {
    def image_map = [:]
    def wum_timestamp = get_latest_wum_timestamp(wso2_product, wso2_product_version)
    def image_tags = generate_tags(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp, jdk_version, is_multi_jdk_required)
    def image
    def docker_tag
    def return_profile
    for (def image_tag = 0; image_tag <image_tags.size(); image_tag++){
        println(image_tags[image_tag])
    }

    (image,docker_tag,profile) = build_image_with_tag(wso2_product, wso2_product_version, os_platform_name, product_profile_docker_home, wum_timestamp, image_tags, product_key, jdk_version, is_multi_jdk_required)
    image_map.put(image, image_tags)
    tag_images(image, image_tags)

    return [image_map,docker_tag,return_profile]
}
