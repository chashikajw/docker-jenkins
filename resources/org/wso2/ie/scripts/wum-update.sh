#!/usr/bin/env bash

# ----------------------------------------------------------------------------
#
# Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
#
# WSO2 Inc. licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file except
# in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ----------------------------------------------------------------------------

readonly wso2_product_name=$1
readonly wso2_product_version=$2
readonly wso2_product_host_location="${WORKSPACE}/product-packs"
readonly WUM_HOME="${HOME}/.wum3"
readonly properties_file_name="timestamp.properties"
readonly wum_detail_file_name="wum_details.txt"
product_pack_name=""

# capture the location of executables of command line utility tools used for the WSO2 product update process
readonly COPY=`which cp`
readonly MOVE=`which mv`
readonly AWK=`which awk`
readonly GREP=`which grep`
readonly ECHO=`which echo`
readonly TEST=`which test`
readonly REMOVE=`which rm`
readonly WUM=`which wum`
readonly CUT=`which cut`

function download_product() {
    ${ECHO} "Adding ${wso2_product_name}-${wso2_product_version}"
    ${WUM} add ${wso2_product_name}-${wso2_product_version} -y &
    local pid=$!
    wait $pid
    ${ECHO} "Updating ${wso2_product_name}-${wso2_product_version}"
    ${WUM} update ${wso2_product_name}-${wso2_product_version} full &
    pid=$!
    wait $pid
}

function move_pack_to_destination() {
    local make_directory=$(which mkdir)
    local wum_product_home="${WUM_HOME}/products/${wso2_product_name}/${wso2_product_version}"
    local product_pack_zip_path="${wum_product_home}/${wso2_product_name}-${wso2_product_version}.zip"

    [[ ${make_directory} ]] && ${TEST} ! -d ${wso2_product_host_location} && ${make_directory} ${wso2_product_host_location}
    ${ECHO} "Moving ${wso2_product} to $wso2_product_host_location"

    if [ -d ${wum_product_home}/full ]; then
        local pack_name_with_timestamp=$(ls ${wum_product_home}/full |  ${GREP} -e "${product_pack_name}.*full\.zip" )
        local pack_name=${wso2_product_name}-${wso2_product_version}
        local product_pack_updated_path="${wum_product_home}/full/${pack_name_with_timestamp}"

        get_update_numbers ${product_pack_updated_path} ${pack_name}

        ${TEST} -f ${product_pack_updated_path} && ${TEST} -d ${wso2_product_host_location} && ${MOVE} ${product_pack_updated_path} ${wso2_product_host_location}
        write_config_file
    else
        ${TEST} -f ${product_pack_zip_path} && ${TEST} -d ${wso2_product_host_location} && ${MOVE} ${product_pack_zip_path} ${wso2_product_host_location}
        local string_value="wum_timestamp=GA"
        ${ECHO} ${string_value} > $properties_file_name
    fi
}

function write_config_file(){
    ${ECHO} "Writing timestamp to properties file"
    local timestamp=$(ls ${wso2_product_host_location} | ${GREP} -e "${wso2_product_name}-${wso2_product_version}.*full.zip" | ${CUT} -d'+' -f2 | ${CUT} -d'.' -f1)
    local string_value="wum_timestamp=${timestamp}"
    ${ECHO} ${string_value} > $properties_file_name
}

function host_products(){
    ${ECHO} "Hosting product pack in localhost:8888"
    pushd ${wso2_product_host_location}
    python3 -m http.server 8888 &
    sleep 5
    popd
}

function get_update_numbers(){
    unzip -q -o $1 -d .
    cat $2/updates/wum/* > $wum_detail_file_name
    cat $wum_detail_file_name
    rm -rf $2
}

download_product
move_pack_to_destination
host_products
