package com.atsoft.jira.plugin.logviewer.rest

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/proxy")
class ProxyPassResource {
    @GET
    @Path("/pass-through")
    fun passThrough(): Response? {
        try {
            // 1. 외부 API 연결
            val targetUrl = "http://external-api.com/data"
            val connection = URL(targetUrl).openConnection() as HttpURLConnection
            connection.setRequestMethod("GET")

            // 2. 외부 API가 주는 Content-Type을 그대로 가져옴 (핵심!)
            // 예: "application/json; charset=EUC-KR" 이면 그대로 가져옴
            val originalContentType = connection.getContentType()

            // 3. 스트림 연결 (InputStream -> OutputStream)
            // StreamingOutput을 사용하면 JAX-RS가 응답을 쓸 때 write() 메서드를 실행해줍니다.
            val stream: StreamingOutput = object : StreamingOutput {
                @Throws(IOException::class)
                override fun write(output: OutputStream) {
                    // 외부 API의 스트림을 엶
                    connection.getInputStream().use { input ->
                        val buffer = ByteArray(8192) // 8KB 버퍼
                        var length: Int
                        // 읽어서 -> 바로 씀 (중간 변환 X, 해석 X)
                        while ((input.read(buffer).also { length = it }) != -1) {
                            output.write(buffer, 0, length)
                        }
                        output.flush()
                    }
                }
            }

            // 4. 응답 생성
            // 내가 만든 API의 Content-Type을 외부 API 것과 똑같이 맞춰서 내보냄
            return Response.ok(stream)
                .header("Content-Type", originalContentType)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            return Response.serverError().entity(e.message).build()
        }
    }

    @POST
    @Path("/pass-through-post") // 1. InputStream을 인자로 받으면 JAX-RS가 Body를 파싱하지 않고 Raw Stream으로 줍니다.
    // 2. HttpHeaders로 클라이언트가 보낸 Content-Type을 확인합니다.
    fun proxyPost(requestBody: InputStream, @Context headers: HttpHeaders): Response? {
        var connection: HttpURLConnection? = null
        try {
            // --- [단계 1] 외부 API 연결 설정 ---
            val targetUrl = "http://external-api.com/create"
            connection = URL(targetUrl).openConnection() as HttpURLConnection?
            connection!!.setRequestMethod("POST")
            connection.setDoOutput(true) // POST엔 필수 (Body를 보낼 것이므로)

            // 클라이언트가 보낸 Content-Type을 그대로 외부 API에 전달 (예: application/json)
            val contentType = headers.getHeaderString("Content-Type")
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType)
            }

            connection.getOutputStream().use { externalOut ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while ((requestBody.read(buffer).also { bytesRead = it }) != -1) {
                    externalOut.write(buffer, 0, bytesRead)
                }
                externalOut.flush()
            }
            // --- [단계 3] 외부 API 응답 처리 (외부 API -> 내 API -> Client) ---
            // 외부 API의 응답 코드 확인
            val responseCode = connection.getResponseCode()

            // 성공(200번대)일 때는 InputStream, 에러일 때는 ErrorStream을 선택
            val inputStreamToRead = if (responseCode >= 200 && responseCode < 300)
                connection.getInputStream()
            else
                connection.getErrorStream()

            // 외부 API의 응답 Content-Type을 가져옴
            val responseContentType = connection.getContentType()

            // StreamingOutput 생성 (GET 때와 동일)
            // 주의: lambda 내부에서 connection을 사용하므로 effectively final이어야 해서 여기서 변수 할당
            val finalInput = inputStreamToRead

            val stream = StreamingOutput { output: OutputStream? ->
                if (finalInput != null) {
                    finalInput.use { input ->
                        val buffer = ByteArray(8192)
                        var length: Int
                        while ((input.read(buffer).also { length = it }) != -1) {
                            output!!.write(buffer, 0, length)
                        }
                        output!!.flush()
                    }
                }
            }

            // 결과 반환 (외부 API의 상태 코드와 헤더를 그대로 유지)
            return Response.status(responseCode)
                .entity(stream)
                .header("Content-Type", responseContentType)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            return Response.serverError().entity("Proxy Error: " + e.message).build()
        }
    }
}