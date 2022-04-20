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
readonly wso2_product_release_version=$2
readonly multi_jdk_required=$3
readonly DOCKER=`which docker`

os_arr=(ubuntu centos alpine)
analytics_profile_arr=(worker dashboard)


ei_profile_arr=(worker dashboard broker business-process integrator msf4j)

if [[ $wso2_product_name == "wso2ei" && $wso2_product_release_version == "6.6"* ]]; then
    # Ignore msf4j for 6.6.x
    ei_profile_arr=(worker dashboard broker business-process integrator)
fi

${DOCKER} images
function create_scan_report() {
    trivy image --clear-cache
    trivy image --download-db-only
    if [[ $wso2_product_name == "wso2am-analytics" || $wso2_product_name == "wso2is-analytics" || $wso2_product_name == "obbi" ]]; then
        for profile in "${analytics_profile_arr[@]}"
        do
            for os in "${os_arr[@]}"
            do
                if [ $os == 'ubuntu' ]; then
                    trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_profiles $profile $os
                else
                    trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version-$os | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_profiles $profile $os
                fi
            done
        done
    elif [[ $wso2_product_name == "wso2ei" && $wso2_product_release_version == "6."* ]]; then
        for profile in "${ei_profile_arr[@]}"
        do
            for os in "${os_arr[@]}"
            do
                if [ $os == 'ubuntu' ]; then
                    trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_profiles $profile $os
                else
                    trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version-$os | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_profiles $profile $os
                fi
            done
        done
    elif [[ $wso2_product_name == "wso2is" || $wso2_product_name == "wso2am" ]]; then
        for os in "${os_arr[@]}"
          do
            if [ $os == 'ubuntu' ]; then
              if [[ $multi_jdk_required == true ]]; then
                 for jdk_version in jdk8 jdk11
                    do
                      echo "scanning ubuntu"
                      if [[ $jdk_version == "jdk11" ]]; then
                        trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                        check_vulnerabilities_for_product $os
                      else
                        trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$jdk_version | tee "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"
                        check_vulnerabilities_for_product_with_jdk $os $jdk_version
                      fi
                    done
              else
                echo "scanning ${os}"
                trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                check_vulnerabilities_for_product $os
              fi
            else
              if [[ $multi_jdk_required == true ]]; then
                for jdk_version in jdk8 jdk11
                  do
                    if [[ $jdk_version == "jdk11" ]]; then
                      trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                      check_vulnerabilities_for_product $os
                    else
                      trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os-$jdk_version | tee "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"
                      check_vulnerabilities_for_product_with_jdk $os $jdk_version
                    fi
                  done
              else
                trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                check_vulnerabilities_for_product $os
              fi
            fi
          done
    elif [[ $wso2_product_name == "choreo-connect"* ]]; then
        trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-alpine-scanResult.txt"
        check_vulnerabilities_for_product alpine
#        trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-ubuntu-scanResult.txt"
#        check_vulnerabilities_for_product ubuntu
    else
        for os in "${os_arr[@]}"
            do
                if [ $os == 'ubuntu' ]; then
                    trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_product $os
                else
                    trivy image --exit-code 1 --severity HIGH,CRITICAL docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_product $os
                fi
            done
    fi
}

function check_vulnerabilities_for_profiles() {
    echo " " | tee -a scanResult.txt
    echo "$wso2_product_name-$1:$wso2_product_release_version-$2" | tee -a scanResult.txt
    echo "--------------------------------------------" | tee -a scanResult.txt

    if [ ! -s $wso2_product_name-$1-$wso2_product_release_version-$2-scanResult.txt ]; then
       echo "No HIGH vulnerabilities detected..." >> $wso2_product_name-$1-$wso2_product_release_version-$2-scanResult.txt
       cat $wso2_product_name-$1-$wso2_product_release_version-$2-scanResult.txt >> scanResult.txt
    else
       cat $wso2_product_name-$1-$wso2_product_release_version-$2-scanResult.txt >> scanResult.txt
    fi
}

function check_vulnerabilities_for_product() {
    echo " " | tee -a scanResult.txt
    echo "$wso2_product_name:$wso2_product_release_version-$1" | tee -a scanResult.txt
    echo "--------------------------------------------" | tee -a scanResult.txt

    if [ ! -s $wso2_product_name-$wso2_product_release_version-$1-scanResult.txt ]; then
       echo "No HIGH vulnerabilities detected..." >> $wso2_product_name-$wso2_product_release_version-$1-scanResult.txt
       cat $wso2_product_name-$wso2_product_release_version-$1-scanResult.txt >> scanResult.txt
    else
       cat $wso2_product_name-$wso2_product_release_version-$1-scanResult.txt >> scanResult.txt
    fi
}

function check_vulnerabilities_for_product_with_jdk() {
    os=$1
    jdk=$2
    echo " " | tee -a scanResult.txt
    echo "$wso2_product_name:$wso2_product_release_version-$os-$jdk" | tee -a scanResult.txt
    echo "--------------------------------------------" | tee -a scanResult.txt

    if [ ! -s $wso2_product_name-$wso2_product_release_version-$os-$jdk-scanResult.txt ]; then
       echo "No HIGH vulnerabilities detected..." >> $wso2_product_name-$wso2_product_release_version-$os-$jdk-scanResult.txt
       cat $wso2_product_name-$wso2_product_release_version-$os-$jdk-scanResult.txt >> scanResult.txt
    else
       cat $wso2_product_name-$wso2_product_release_version-$os-$jdk-scanResult.txt >> scanResult.txt
    fi
}

create_scan_report
