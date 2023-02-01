package org.jetbrains.plugins.rsocket.endpoints

import com.intellij.lang.java.JavaLanguage
import com.intellij.microservices.endpoints.presentation.EndpointMethodPresentation
import com.intellij.microservices.oas.OasEndpointPath
import com.intellij.microservices.oas.OasHttpMethod
import com.intellij.microservices.oas.OasOperation
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.rsocket.RSOCKET_REQUEST_TYPES
import org.jetbrains.plugins.rsocket.rsocketIcon


@Suppress("UnstableApiUsage")
class RSocketEndpoint(val requestType: String, val routing: String, private val element: PsiMethod) : ItemPresentation, EndpointMethodPresentation, DataProvider {

    override fun getPresentableText(): String {
        return routing
    }

    override fun getIcon(unused: Boolean) = rsocketIcon

    override fun getLocationString(): String {
        return element.containingFile.name.let {
            it.substring(0, it.lastIndexOf('.'))
        }
    }

    override val endpointMethodOrder = 0

    override val endpointMethodPresentation: String
        get() = requestType
    override val endpointMethods: List<String>
        get() = RSOCKET_REQUEST_TYPES.toList()

    override fun getData(dataId: String): Any? = when (dataId) {
        CommonDataKeys.PSI_ELEMENT.name -> element
        CommonDataKeys.PROJECT.name -> element.project
        CommonDataKeys.VIRTUAL_FILE.name -> element.containingFile.virtualFile
        CommonDataKeys.PSI_FILE.name -> element.containingFile
        CommonDataKeys.NAVIGATABLE.name -> element
        CommonDataKeys.LANGUAGE.name -> JavaLanguage.INSTANCE
        "endpoint.documentation.element" -> element
        "endpoint.urlTargetInfo" -> listOf(RSocketUrlTargetInfo(element, requestType, routing))
        "endpoint.openApiPath" -> listOf(
            OasEndpointPath(
                routing,
                "RSocket Service Call",
                listOf(OasOperation(OasHttpMethod.POST, listOf(), null,
                    routing, requestType, false, emptyList(), null, emptyList()))
            )
        )
        else -> {
            //println("==========dataId: ${dataId}")
            null
        }
    }

}