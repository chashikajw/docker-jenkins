#!/usr/bin/env bash
# ----------------------------------------------------------------------------
#
# Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
readonly docker_image=$4
readonly os=$5
readonly jdk_version=$6
readonly profile=$7
readonly ignorefile_path=$8
readonly severity=$9
readonly wso2_product_version=${10}
readonly timeout=${11}
summary=""
isCriticalVulnarabilityExist = false

if [[ "${severity}" == "CRITICAL" ]]; then
    installer_arr=(0 0)
elif [[ "${severity}" == "HIGH,CRITICAL" ]];then
    installer_arr=(0 0 0)
elif [[ "${severity}" == "MEDIUM,HIGH,CRITICAL" ]]; then 
    installer_arr=(0 0 0 0)
elif [[ "${severity}" == "LOW,MEDIUM,HIGH,CRITICAL" ]]; then
    installer_arr=(0 0 0 0 0)
else
    installer_arr=(0 0 0 0 0)
fi

function getInstallerVulnerableCount(){
    input="$1"

    IFS=' ' read -ra arr <<< "$input"

    if [[ "${severity}" == "CRITICAL" ]]; then
        if [[ "${arr[0]}" == "Total:" ]]; then
            installer_arr[0]="$((${installer_arr[0]}+${arr[1]}))" 
        fi
        if [[ "${arr[2]}" == "(CRITICAL:" ]]; then
            installer_arr[1]="$((${installer_arr[1]}+${arr[3]%?}))" 
        fi
    elif [[ "${severity}" == "HIGH,CRITICAL" ]]; then
        if [[ "${arr[0]}" == "Total:" ]]; then
            installer_arr[0]="$((${installer_arr[0]}+${arr[1]}))" 
        fi
        if [[ "${arr[2]}" == "(HIGH:" ]]; then
            installer_arr[1]="$((${installer_arr[1]}+${arr[3]%?}))" 
        fi
        if [[ "${arr[4]}" == "CRITICAL:" ]]; then
            installer_arr[2]="$((${installer_arr[2]}+${arr[5]%?}))" 
        fi
    elif [[ "${severity}" == "MEDIUM,HIGH,CRITICAL" ]]; then 
        if [[ "${arr[0]}" == "Total:" ]]; then
            installer_arr[0]="$((${installer_arr[0]}+${arr[1]}))" 
        fi
        if [[ "${arr[2]}" == "(MEDIUM:" ]]; then
            installer_arr[1]="$((${installer_arr[1]}+${arr[3]%?}))" 
        fi
        if [[ "${arr[4]}" == "HIGH:" ]]; then
            installer_arr[2]="$((${installer_arr[2]}+${arr[5]%?}))" 
        fi
        if [[ "${arr[6]}" == "CRITICAL:" ]]; then
            installer_arr[3]="$((${installer_arr[3]}+${arr[7]%?}))" 
        fi
    elif [[ "${severity}" == "LOW,MEDIUM,HIGH,CRITICAL" ]]; then
        if [[ "${arr[0]}" == "Total:" ]]; then
            installer_arr[0]="$((${installer_arr[0]}+${arr[1]}))" 
        fi
        if [[ "${arr[2]}" == "(LOW:" ]]; then
            installer_arr[1]="$((${installer_arr[1]}+${arr[3]%?}))" 
        fi
        if [[ "${arr[4]}" == "MEDIUM:" ]]; then
            installer_arr[2]="$((${installer_arr[2]}+${arr[5]%?}))" 
        fi
        if [[ "${arr[6]}" == "HIGH:" ]]; then
            installer_arr[3]="$((${installer_arr[3]}+${arr[7]%?}))" 
        fi
        if [[ "${arr[8]}" == "CRITICAL:" ]]; then
            installer_arr[4]="$((${installer_arr[4]}+${arr[9]%?}))" 
        fi
    fi

}

function getImageVulnerabilitySummary(){
    inp="$1"
    while IFS= read -r j; do 
        getInstallerVulnerableCount "$j"
    done < <(grep "home/wso2carbon/${wso2_product_name}-${wso2_product_version}/bin" -A 3 ${inp} | grep "Total")

    for ((i=0;i<${#installer_arr[@]};i++));
        do
            summary+="${installer_arr[i]} "
        done
}

function getVulnerableCount(){
    inp="$1"
    
    IFS=' ' read -ra arr <<< "$inp"
    
    if [[ "${severity}" == "CRITICAL" ]]; then
        if [[ "${arr[0]}" == "Total:" ]]; then
            summary+="${arr[1]} "
        else
            summary+="0 "
        fi
        if [[ "${arr[2]}" == "(CRITICAL:" ]]; then
            summary+="${arr[3]%?} "
        else
            summary+="0 "
        fi
    elif [[ "${severity}" == "HIGH,CRITICAL" ]]; then
        if [[ "${arr[0]}" == "Total:" ]]; then
            summary+="${arr[1]} "
        else
            summary+="0 "
        fi
        if [[ "${arr[2]}" == "(HIGH:" ]]; then
            summary+="${arr[3]%?} "
        else
            summary+="0 "
        fi
        if [[ "${arr[4]}" == "CRITICAL:" ]]; then
            summary+="${arr[5]%?} "
        else
            summary+="0 "
        fi
    elif [[ "${severity}" == "MEDIUM,HIGH,CRITICAL" ]]; then 
        if [[ "${arr[0]}" == "Total:" ]]; then
            summary+="${arr[1]} "
        else
            summary+="0 "
        fi
        if [[ "${arr[2]}" == "(MEDIUM:" ]]; then
            summary+="${arr[3]%?} "
        else
            summary+="0 "
        fi
        if [[ "${arr[4]}" == "HIGH:" ]]; then
            summary+="${arr[5]%?} "
        else
            summary+="0 "
        fi
        if [[ "${arr[6]}" == "CRITICAL:" ]]; then
            summary+="${arr[7]%?} "
        else
            summary+="0 "
        fi
    elif [[ "${severity}" == "LOW,MEDIUM,HIGH,CRITICAL" ]]; then 
        if [[ "${arr[0]}" == "Total:" ]]; then
            summary+="${arr[1]} "
        else
            summary+="0 "
        fi
        if [[ "${arr[2]}" == "(LOW:" ]]; then
            summary+="${arr[3]%?} "
        else
            summary+="0 "
        fi
        if [[ "${arr[4]}" == "MEDIUM:" ]]; then
            summary+="${arr[5]%?} "
        else
            summary+="0 "
        fi
        if [[ "${arr[6]}" == "HIGH:" ]]; then
            summary+="${arr[7]%?} "
        else
            summary+="0 "
        fi
        if [[ "${arr[8]}" == "CRITICAL:" ]]; then
            summary+="${arr[9]%?} "
        else
            summary+="0 "
        fi
    fi
}

${DOCKER} images
function create_scan_report() {
    trivy image --clear-cache
    trivy image --download-db-only
    # Creating release tag for summary text file
    release_tag="${wso2_product_name}:${wso2_product_release_version}"

    if [[ $multi_jdk_required = true ]]; then
        release_tag+="-${jdk_version}"
    fi

    # Creating summary text
    summary+="$release_tag "
    summary+="$os "
    if [[ $multi_jdk_required = true ]];then
        summary+="$jdk_version "
    else
        summary+="jdk11 "
    fi

    if [[ $wso2_product_name == "wso2am-analytics" || $wso2_product_name == "wso2is-analytics" || $wso2_product_name == "obbi" ]]; then
        if [ $os == 'ubuntu' ]; then
            trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
            check_vulnerabilities_for_profiles $profile $os
        else
            trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version-$os | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
            check_vulnerabilities_for_profiles $profile $os
        fi
    elif [[ $wso2_product_name == "wso2ei" && $wso2_product_release_version == "6."* ]]; then 
        if [ $os == 'ubuntu' ]; then
            trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
            check_vulnerabilities_for_profiles $profile $os
        else
            trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name-$profile:$wso2_product_release_version-$os | tee "$wso2_product_name-$profile-$wso2_product_release_version-$os-scanResult.txt"
            check_vulnerabilities_for_profiles $profile $os
        fi
    elif [[ $wso2_product_name == "wso2am" ]]; then
        if [ $os == 'ubuntu' ]; then
            echo "scanning ubuntu"
            if [[ $multi_jdk_required == true ]]; then
                if [[ $jdk_version == "jdk11" ]]; then
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_product $os
                    OS_result=$(grep "docker.wso2.com/$wso2_product_name:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    ui_result=$(grep "package-lock.json" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")

                    getVulnerableCount "$OS_result" "OS"
                    getVulnerableCount "$jar_result" "Java"
                    getVulnerableCount "$ui_result" "UI"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                else
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$jdk_version | tee "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"
                    check_vulnerabilities_for_product_with_jdk $os $jdk_version
                    OS_result=$(grep "docker.wso2.com/$wso2_product_name:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total") 
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total")
                    ui_result=$(grep "package-lock.json" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    
                    getVulnerableCount "$OS_result" "OS"
                    getVulnerableCount "$jar_result" "Java"
                    getVulnerableCount "$ui_result" "UI"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                fi
            else
                trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                check_vulnerabilities_for_product $os
                OS_result=$(grep "docker.wso2.com/$wso2_product_name:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total") 
                jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                ui_result=$(grep "package-lock.json" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                
                getVulnerableCount "$OS_result" "OS"
                getVulnerableCount "$jar_result" "Java"
                getVulnerableCount "$ui_result" "UI"
                getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                summary+=${#release_tag}
                echo -e $summary >> summaryText.txt
            fi
        else
            echo "scanning ${os}"
            if [[ $multi_jdk_required == true ]]; then
                if [[ $jdk_version == "jdk11" ]]; then
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_product $os
                    OS_result=$(grep "docker.wso2.com/$wso2_product_name:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total") 
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    ui_result=$(grep "package-lock.json" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    
                    getVulnerableCount "$OS_result" "OS"
                    getVulnerableCount "$jar_result" "Java"
                    getVulnerableCount "$ui_result" "UI"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                else
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os-$jdk_version | tee "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"
                    check_vulnerabilities_for_product_with_jdk $os $jdk_version
                    OS_result=$(grep "docker.wso2.com/$wso2_product_name:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total") 
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total")
                    ui_result=$(grep "package-lock.json" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    getVulnerableCount "$OS_result" "OS"
                    getVulnerableCount "$jar_result" "Java"
                    getVulnerableCount "$ui_result" "UI"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                fi
            else
                trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                check_vulnerabilities_for_product $os
                OS_result=$(grep "docker.wso2.com/$wso2_product_name:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total") 
                jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                ui_result=$(grep "package-lock.json" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                getVulnerableCount "$OS_result" "OS"
                    getVulnerableCount "$jar_result" "Java"
                    getVulnerableCount "$ui_result" "UI"
                getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                summary+=${#release_tag}
                echo -e $summary >> summaryText.txt
            fi
        fi  
    elif [[ $wso2_product_name == "wso2is" ]]; then
        if [ $os == 'ubuntu' ]; then
            echo "scanning ubuntu"
            if [[ $multi_jdk_required == true ]]; then
                if [[ $jdk_version == "jdk11" ]]; then
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_product $os
                    OS_result=$(grep "docker.wso2.com/wso2is:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")

                    getVulnerableCount "$OS_result"
                    getVulnerableCount "$jar_result"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                else
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$jdk_version | tee "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"
                    check_vulnerabilities_for_product_with_jdk $os $jdk_version
                    OS_result=$(grep "docker.wso2.com/wso2is:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total") 
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total")
                    
                    getVulnerableCount "$OS_result"
                    getVulnerableCount "$jar_result"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                fi
            else
                trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                check_vulnerabilities_for_product $os
                OS_result=$(grep "docker.wso2.com/wso2is:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total") 
                jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                
                getVulnerableCount "$OS_result"
                getVulnerableCount "$jar_result"
                getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                summary+=${#release_tag}
                echo -e $summary >> summaryText.txt
            fi
        else
            echo "scanning ${os}"
            if [[ $multi_jdk_required == true ]]; then
                if [[ $jdk_version == "jdk11" ]]; then
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                    check_vulnerabilities_for_product $os
                    OS_result=$(grep "docker.wso2.com/wso2is:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total") 
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                    
                    getVulnerableCount "$OS_result"
                    getVulnerableCount "$jar_result"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                else
                    trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os-$jdk_version | tee "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"
                    check_vulnerabilities_for_product_with_jdk $os $jdk_version
                    OS_result=$(grep "docker.wso2.com/wso2is:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total") 
                    jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt | grep "Total")
                    getVulnerableCount "$OS_result"
                    getVulnerableCount "$jar_result"
                    getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt"

                    summary+=${#release_tag}
                    echo -e $summary >> summaryText.txt
                fi
            else
                trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
                check_vulnerabilities_for_product $os
                OS_result=$(grep "docker.wso2.com/wso2is:$wso2_product_release_version" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total") 
                jar_result=$(grep "Java" -A 3 $wso2_product_name-$wso2_product_release_version-$os-scanResult.txt | grep "Total")
                getVulnerableCount "$OS_result"
                getVulnerableCount "$jar_result"
                getImageVulnerabilitySummary "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"

                summary+=${#release_tag}
                echo -e $summary >> summaryText.txt
            fi
        fi  
    else
        if [ $os == 'ubuntu' ]; then
            trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
            check_vulnerabilities_for_product $os
        else
            trivy image --severity "$severity" --ignorefile "$ignorefile_path" --timeout "$timeout" docker.wso2.com/$wso2_product_name:$wso2_product_release_version-$os | tee "$wso2_product_name-$wso2_product_release_version-$os-scanResult.txt"
            check_vulnerabilities_for_product $os
        fi
    fi
    
    if isCriticalVulnarabilityExist == true; then
        return 1
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
    echo " " | tee -a scanResult.txt
    echo "$wso2_product_name:$wso2_product_release_version-$os-$jdk_version" | tee -a scanResult.txt
    echo "--------------------------------------------" | tee -a scanResult.txt

    if [ ! -s $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt ]; then
        echo "No HIGH vulnerabilities detected..." >> $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt
       cat $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt >> scanResult.txt
    else
       cat $wso2_product_name-$wso2_product_release_version-$os-$jdk_version-scanResult.txt >> scanResult.txt
    fi
}

create_scan_report
