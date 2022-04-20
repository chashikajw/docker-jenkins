import org.apache.commons.lang3.StringUtils

def result(jobResult,jobName, wso2_product_version) {
    if (jobResult == 'SUCCESS') {
        def output = jobName + " - " + wso2_product_version + "<br/>"
        sh """
            { set +x; } 2>/dev/null
            echo '$output' >> successResult.html
            echo "$jobName" | tee -a scan_product_list.txt
         """
    } else if (jobResult == 'FAILURE' || jobResult == 'UNSTABLE') {
        def output = jobName + " - " + wso2_product_version + "<br/>"
        sh """
            { set +x; } 2>/dev/null
            echo '$output' >> failResult.html
        """
    }
}

def send(subject, content) {
    emailext(to: "${EMAIL_TO}",
            subject: subject,
            body: content, mimeType: 'text/html')
}

def triggerProductBuild(wso2_product, wso2_product_version) {
    try {
        if (wso2_product == "wso2is-km" || wso2_product == "wso2am" || wso2_product == "wso2am-analytics" || wso2_product == "wso2am-micro-gw") {
            def jobBuild = build job: 'product-apim', parameters: [
                    string(name: 'wso2_product', value: wso2_product),
                    string(name: 'wso2_product_version', value: wso2_product_version)], wait: true

            def jobResult = jobBuild.getResult()
            def jobName = "product-apim"
            result(jobResult, jobName, wso2_product_version)

        } else if (wso2_product == "wso2ei-analytics-dashboard" || wso2_product == "wso2ei-analytics-worker" || wso2_product == "wso2ei-broker" || wso2_product == "wso2ei-business-process" || wso2_product == "wso2ei-integrator" || wso2_product == "wso2ei-msf4j" || wso2_product == "wso2mi" || wso2_product == "wso2si" || wso2_product == "wso2ei") {
            def jobBuild = build job: 'product-ei', parameters: [
                    string(name: 'wso2_product', value: wso2_product),
                    string(name: 'wso2_product_version', value: wso2_product_version)], wait: true

            def jobResult = jobBuild.getResult()
            def jobName = "product-ei"
            result(jobResult, jobName, wso2_product_version)

        } else if (wso2_product == "wso2is" || wso2_product == "wso2is-analytics-dashboard" || wso2_product == "wso2is-analytics-worker") {
            def jobBuild = build job: 'product-is', parameters: [
                    string(name: 'wso2_product', value: wso2_product),
                    string(name: 'wso2_product_version', value: wso2_product_version)], wait: true

            def jobResult = jobBuild.getResult()
            def jobName = "product-is"
            result(jobResult, jobName, wso2_product_version)

        } else if (wso2_product == "wso2-obam" || wso2_product == "wso2-obbi-worker" || wso2_product == "wso2-obbi-dashboard" || wso2_product == "wso2-obkm" || wso2_product == "wso2-obiam" || wso2_product == "wso2-obiam-accelerator" || wso2_product == "wso2-obam-accelerator") {
            def jobBuild = build job: 'product-ob', parameters: [
                    string(name: 'wso2_product', value: wso2_product),
                    string(name: 'wso2_product_version', value: wso2_product_version)], wait: true

            def jobResult = jobBuild.getResult()
            def jobName = "product-ob"
            result(jobResult, jobName, wso2_product_version)
        }

        currentBuild.result = "SUCCESS"

    } catch(Exception ex) {
        println("Error occurred while trigger the product docker builds");
        error()
    } finally {
        sendNotifications()
    }
}

def error() {
    currentBuild.result = "FAILURE"
}
def sendNotifications() {
    step([$class: "Mailer", notifyEveryUnstableBuild: true, recipients: '${EMAIL_TO}', sendToIndividuals: true])
}

def call() {
    node {
        stage('Get Updates 2.0 released products') {
            def product_list = "${RELEASED_PRODUCTS}"
            def arr = product_list.split(",")

            sh """
            { set +x; } 2>/dev/null
                rm -rf *
                touch successResult.html failResult.html product_list scan_list.txt
                echo "$RELEASED_PRODUCTS"  >> product_list
                echo "Updates 2.0 Released Products\n-------------------------"
                cat product_list
            """

            for (i in arr) {
                def wso2_product, wso2_version
                wso2_product = i.tokenize("-").init().join("-")
                wso2_version = i.tokenize("-").last()
                wso2_product_version = wso2_version.replaceAll(/\.[a-zA-Z]+\w/, "")
                triggerProductBuild(wso2_product, wso2_product_version)
            }
        }

        stage('Send Result Email') {
            script {
                try {
                    if (fileExists("successResult.html")) {
                        def emailBodySuccess = readFile "${WORKSPACE}/successResult.html"
                        def emailBodyFail = readFile "${WORKSPACE}/failResult.html"
                        String productList = readFile "product_list"

                        send("Docker Image Build Results! #(${env.BUILD_NUMBER}) - WSO2 IE Team", """
                            <div style="padding-left: 10px">
                            <div style="height: 4px; background-image: linear-gradient(to right, rgba(52, 201, 250), rgba(215, 239, 247));"></div>
                            <div style="margin: auto; background-color: #ffffff;">
                            <p style="height:10px;font-family: Lucida Grande;font-size: 20px;">
                            <font color="black"><b> Docker job status </b></font></p>
                            <table cellspacing="0" cellpadding="6" border="2" bgcolor="#f0f0f0" width="80%"><colgroup>
                            <col width="150"/>
                            <col width="150"/></colgroup>

                            <tr style="border: 1px solid black; font-size: 16px;">
                            <th bgcolor="#05B349" style="padding-top: 3px; padding-bottom: 3px">Success jobs</th>
                            <th bgcolor="#F44336" style="black">Failed/Unstable jobs</th></tr><tr>
                            <td>${emailBodySuccess}</td>
                            <td>${emailBodyFail}</td></tr></table><br/>
                            <table cellspacing="0" cellpadding="6" border="1" width="30%" style="font-size: 16px; background-color: #f0f0f0">
                            <colgroup><col width="150"/></colgroup>
                            <tr style="border: 1px solid black; padding-top: 3px; padding-bottom: 3px; background-color: #9E9E9E;">
                            <th>Updates 2.0 Released Products</th></tr><tr>
                            <td>${productList}</td></tr></table><br/>

                            <p style="font-family:'Courier New'">Build Info - ${env.BUILD_URL}.</p></div></div>
                         """)

                    } else {
                        log.info("No updates found..!")
                        send("Docker Image Build Results!", "Build Issue..!")
                    }

                } catch (e) {
                    echo "Error while sending mail: " + e.getMessage()
                    currentBuild.result = "FAILED"
                }
            }
        }
    }
}
