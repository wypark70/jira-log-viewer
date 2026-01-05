package ut.com.atsoft.jira.plugin.logviewer.rest

import com.atsoft.jira.plugin.logviewer.rest.ProxyResource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@ExtendWith(MockitoExtension::class)
@TestMethodOrder(MethodOrderer.DisplayName::class)
@DisplayName("ProxyResource 테스트")
internal class ProxyResourceTest {

    @Mock
    private lateinit var mockHttpClient: HttpClient

    @Mock
    private lateinit var mockHttpResponse: HttpResponse<String>

    private lateinit var proxyResource: ProxyResource

    @BeforeEach
    fun setUp() {
        proxyResource = ProxyResource()
        // Use reflection to inject the mock HttpClient
        val httpClientField = ProxyResource::class.java.getDeclaredField("httpClient")
        httpClientField.isAccessible = true
        httpClientField.set(proxyResource, mockHttpClient)
    }

    @Test
    @DisplayName("1. GET 요청이 성공적으로 전달되어야 한다")
    fun testGetRequestSuccess() {
        // Given
        val expectedBody = """{"message": "success"}"""
        val expectedStatusCode = 200

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(expectedBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(
                mapOf("X-Custom-Header" to listOf("custom-value")),
                { _, _ -> true }
            )
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.get()

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertEquals(expectedBody, response.entity)

        // Verify the request was made
        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(mockHttpClient).send(requestCaptor.capture(), any<HttpResponse.BodyHandler<String>>())

        val capturedRequest = requestCaptor.firstValue
        assertEquals(ProxyResource.EXTERNAL_API_URL, capturedRequest.uri().toString())
        assertEquals("GET", capturedRequest.method())
        assertTrue(capturedRequest.headers().firstValue("Accept").isPresent)
        assertEquals(ProxyResource.APPLICATION_JSON_UTF8, capturedRequest.headers().firstValue("Accept").get())
    }

    @Test
    @DisplayName("2. POST 요청이 body와 함께 성공적으로 전달되어야 한다")
    fun testPostRequestSuccess() {
        // Given
        val requestBody = """{"name": "test", "value": "data"}"""
        val expectedResponseBody = """{"id": "123", "status": "created"}"""
        val expectedStatusCode = 201

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(expectedResponseBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(
                mapOf("Location" to listOf("/api/items/123")),
                { _, _ -> true }
            )
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.post(requestBody)

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertEquals(expectedResponseBody, response.entity)

        // Verify the request was made with correct body
        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(mockHttpClient).send(requestCaptor.capture(), any<HttpResponse.BodyHandler<String>>())

        val capturedRequest = requestCaptor.firstValue
        assertEquals(ProxyResource.EXTERNAL_API_URL, capturedRequest.uri().toString())
        assertEquals("POST", capturedRequest.method())
        assertTrue(capturedRequest.headers().firstValue("Content-Type").isPresent)
        assertEquals(ProxyResource.APPLICATION_JSON_UTF8, capturedRequest.headers().firstValue("Content-Type").get())
        assertTrue(capturedRequest.headers().firstValue("Accept").isPresent)
        assertEquals(ProxyResource.APPLICATION_JSON_UTF8, capturedRequest.headers().firstValue("Accept").get())
    }

    @Test
    @DisplayName("3. GET 요청 실패 시 에러 상태 코드가 반환되어야 한다")
    fun testGetRequestFailure() {
        // Given
        val expectedStatusCode = 404
        val expectedBody = """{"error": "Not Found"}"""

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(expectedBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(emptyMap(), { _, _ -> true })
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.get()

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertEquals(expectedBody, response.entity)
    }

    @Test
    @DisplayName("4. POST 요청 실패 시 에러 상태 코드가 반환되어야 한다")
    fun testPostRequestFailure() {
        // Given
        val requestBody = """{"invalid": "data"}"""
        val expectedStatusCode = 400
        val expectedBody = """{"error": "Bad Request"}"""

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(expectedBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(emptyMap(), { _, _ -> true })
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.post(requestBody)

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertEquals(expectedBody, response.entity)
    }

    @Test
    @DisplayName("5. 응답 헤더가 올바르게 전달되어야 한다")
    fun testResponseHeadersForwarding() {
        // Given
        val expectedBody = """{"data": "test"}"""
        val expectedStatusCode = 200

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(expectedBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(
                mapOf(
                    "X-Custom-Header" to listOf("value1", "value2"),
                    "X-Another-Header" to listOf("single-value")
                ),
                { _, _ -> true }
            )
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.get()

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertNotNull(response.metadata)

        // Verify Content-Type header is set
        val contentTypeHeaders = response.metadata["Content-Type"]
        assertNotNull(contentTypeHeaders)
        assertTrue(contentTypeHeaders.toString().contains(ProxyResource.APPLICATION_JSON_UTF8))
    }

    @Test
    @DisplayName("6. 빈 body로 POST 요청이 가능해야 한다")
    fun testPostWithEmptyBody() {
        // Given
        val emptyBody = ""
        val expectedStatusCode = 200
        val expectedResponseBody = """{"status": "ok"}"""

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(expectedResponseBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(emptyMap(), { _, _ -> true })
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.post(emptyBody)

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertEquals(expectedResponseBody, response.entity)
    }

    @Test
    @DisplayName("7. 한글 데이터가 UTF-8로 올바르게 처리되어야 한다")
    fun testKoreanDataHandling() {
        // Given
        val koreanBody = """{"message": "안녕하세요", "description": "한글 테스트입니다"}"""
        val expectedStatusCode = 200

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(koreanBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(emptyMap(), { _, _ -> true })
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.post(koreanBody)

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertEquals(koreanBody, response.entity)

        // Verify UTF-8 encoding is used
        val requestCaptor = argumentCaptor<HttpRequest>()
        verify(mockHttpClient).send(requestCaptor.capture(), any<HttpResponse.BodyHandler<String>>())

        val capturedRequest = requestCaptor.firstValue
        assertTrue(capturedRequest.headers().firstValue("Content-Type").get().contains("UTF-8"))
    }

    @Test
    @DisplayName("8. 서버 에러(5xx) 상태 코드가 올바르게 반환되어야 한다")
    fun testServerError() {
        // Given
        val expectedStatusCode = 500
        val expectedBody = """{"error": "Internal Server Error"}"""

        whenever(mockHttpResponse.statusCode()).thenReturn(expectedStatusCode)
        whenever(mockHttpResponse.body()).thenReturn(expectedBody)
        whenever(mockHttpResponse.headers()).thenReturn(
            java.net.http.HttpHeaders.of(emptyMap(), { _, _ -> true })
        )

        whenever(mockHttpClient.send(any(), any<HttpResponse.BodyHandler<String>>()))
            .thenReturn(mockHttpResponse)

        // When
        val response = proxyResource.get()

        // Then
        assertEquals(expectedStatusCode, response.status)
        assertEquals(expectedBody, response.entity)
    }
}
