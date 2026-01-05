package com.atsoft.jira.plugin.logviewer.rest

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.ws.rs.GET
import javax.ws.rs.HttpMethod
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/proxy-pass/labelit/items")
class ProxyPassResource {

    companion object {
        private const val TARGET_URL = "http://external-api.com/rest/labelit/1.0/items"
        private const val CONNECT_TIMEOUT = 5000
        private const val READ_TIMEOUT = 10000
    }

    @GET
    fun proxyGet(): Response = runCatchingProxy {
        val connection = openProxyConnection(HttpMethod.GET)

        Response.ok(connection.proxyStream)
            .header(HttpHeaders.CONTENT_TYPE, connection.contentType)
            .build()
    }

    @POST
    fun proxyPost(requestBody: InputStream, @Context headers: HttpHeaders): Response = runCatchingProxy {
        val connection = openProxyConnection(HttpMethod.POST).apply {
            doOutput = true
            headers.getHeaderString(HttpHeaders.CONTENT_TYPE)?.let { setRequestProperty(HttpHeaders.CONTENT_TYPE, it) }

            // 요청 바디 전송
            outputStream.use { requestBody.copyTo(it) }
        }

        Response.status(connection.responseCode)
            .entity(connection.proxyStream)
            .header(HttpHeaders.CONTENT_TYPE, connection.contentType)
            .build()
    }

    // --- Helper Extensions & Functions ---

    /**
     * 반복되는 try-catch를 Kotlin 스타일의 runCatching으로 래핑
     */
    private inline fun runCatchingProxy(block: () -> Response): Response {
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            Response.serverError()
                .entity("Proxy Error: ${e.message}")
                .build()
        }
    }

    /**
     * 공통 연결 설정 로직
     */
    private fun openProxyConnection(method: String): HttpURLConnection {
        // String -> URI -> URL 순서로 변환하여 안전하게 생성
        val url = URI.create(TARGET_URL).toURL()

        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
        }
    }

    /**
     * HttpURLConnection에서 응답 스트림을 추출하여 StreamingOutput으로 변환하는 확장 프로퍼티
     */
    private val HttpURLConnection.proxyStream: StreamingOutput
        get() = StreamingOutput { output ->
            val stream = if (responseCode in 200..299) inputStream else errorStream
            stream?.use { it.copyTo(output); output.flush() }
        }
}