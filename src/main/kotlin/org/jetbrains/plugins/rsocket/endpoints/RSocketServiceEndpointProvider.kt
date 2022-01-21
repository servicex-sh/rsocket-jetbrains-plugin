package org.jetbrains.plugins.rsocket.endpoints

import com.intellij.lang.java.JavaLanguage
import com.intellij.microservices.endpoints.*
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.plugins.rsocket.file.RSocketServiceFileIndex
import org.jetbrains.plugins.rsocket.rsocketIcon

@Suppress("UnstableApiUsage")
class RSocketServiceEndpointProvider : EndpointsProvider<RSocketEndpointsGroup, RSocketEndpoint> {

    companion object {
        var RSOCKET_ENDPOINT_TYPE = EndpointType("RSocket", rsocketIcon) {
            "RSocket Server"
        }
        var RSOCKET_FRAMEWORK = FrameworkPresentation(
            "SpringRSocket",
            "Spring RSocket",
            rsocketIcon
        )
    }

    override val endpointType: EndpointType
        get() = RSOCKET_ENDPOINT_TYPE
    override val presentation: FrameworkPresentation
        get() = RSOCKET_FRAMEWORK

    override fun getEndpointData(group: RSocketEndpointsGroup, endpoint: RSocketEndpoint, dataId: String): Any? {
        return endpoint.getData(dataId)
    }

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): List<RSocketEndpointsGroup> {
        if (filter == ExternalEndpointsFilter) {
            return emptyList()
        }
        val groups = RSocketServiceFileIndex.findRSocketServiceFiles(project)
            .map { psiFile ->
                var vendor = "Spring RSocket"
                if (psiFile.text.contains("@RSocketService")) {
                    vendor = "Alibaba RSocket"
                }
                RSocketEndpointsGroup(project, psiFile, vendor)
            }
            .toList()
        if (groups.any { it.vendor == "Alibaba RSocket" }) {
            RSOCKET_FRAMEWORK = FrameworkPresentation(
                "AliRSocket",
                "Alibaba RSocket",
                rsocketIcon
            )
        }
        return groups
    }

    override fun getEndpointPresentation(group: RSocketEndpointsGroup, endpoint: RSocketEndpoint): ItemPresentation {
        return endpoint
    }

    override fun getEndpoints(group: RSocketEndpointsGroup): Iterable<RSocketEndpoint> {
        return group.endpoints()
    }

    override fun getModificationTracker(project: Project): ModificationTracker {
        return PsiModificationTracker.SERVICE.getInstance(project).forLanguage(JavaLanguage.INSTANCE)
    }

    override fun getStatus(project: Project): EndpointsProvider.Status = when {
        getEndpointGroups(project, object : EndpointsFilter {}).isNotEmpty() -> EndpointsProvider.Status.AVAILABLE
        else -> EndpointsProvider.Status.HAS_ENDPOINTS
    }

    override fun isValidEndpoint(group: RSocketEndpointsGroup, endpoint: RSocketEndpoint): Boolean {
        return true
    }
}