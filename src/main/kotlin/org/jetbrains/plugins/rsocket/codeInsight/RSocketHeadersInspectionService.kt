package org.jetbrains.plugins.rsocket.codeInsight

import com.intellij.httpClient.http.request.codeInsight.HttpRequestIncorrectHttpHeaderInspection
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager

class RSocketHeadersInspectionService(project: Project) {
    init {
        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)
        val inspectionToolWrapper = inspectionProfileManager.currentProfile.getInspectionTool("IncorrectHttpHeaderInspection", project)
        val httpHeaderInspection = inspectionToolWrapper?.tool as HttpRequestIncorrectHttpHeaderInspection
        val customHeaders = httpHeaderInspection.getCustomHeaders() as MutableSet<String>
        if (!(customHeaders.contains("X-AliBroker") || customHeaders.contains("X-ServiceName"))) {
            customHeaders.add("X-AliBroker")
            customHeaders.add("X-ServiceName")
            customHeaders.add("X-InstanceName")
            customHeaders.add("X-MajorVersion")
            customHeaders.add("X-ClusterName")
            customHeaders.add("X-Endpoint-UUID")
            customHeaders.add("X-Endpoint-Ip")
            customHeaders.add("Setup-Metadata")
            customHeaders.add("Setup-Data")
            customHeaders.add("Metadata-Type")
            customHeaders.add("Metadata")
        }
    }
}