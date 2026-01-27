package com.atsoft.jira.plugin.logviewer.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

@Path("/byte-pass-through/labelit/items")
public class BytePassThroughResource {

    private static final String TARGET_URL = "http://external-api.com/rest/labelit/1.0/items";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;

    @GET
    public Response passThrough() {
        try {
            HttpURLConnection connection = openConnection(TARGET_URL, HttpMethod.GET);
            return buildProxyResponse(connection);
        } catch (Exception e) {
            return handleProxyError(e);
        }
    }

    @POST
    public Response proxyPost(InputStream requestBody, @Context HttpHeaders headers) {
        try {
            HttpURLConnection connection = openConnection(TARGET_URL, HttpMethod.POST);
            connection.setDoOutput(true);

            // 클라이언트 Content-Type 전달
            Optional.ofNullable(headers.getHeaderString(HttpHeaders.CONTENT_TYPE))
                    .ifPresent(type -> connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, type));

            // 요청 데이터 전송 (Client -> 외부 API)
            try (var os = connection.getOutputStream()) {
                requestBody.transferTo(os);
                os.flush();
            }

            return buildProxyResponse(connection);
        } catch (Exception e) {
            return handleProxyError(e);
        }
    }

    // --- Private Helper Methods ---

    /**
     * URI를 통한 안전한 HttpURLConnection 생성 및 기본 설정
     */
    private HttpURLConnection openConnection(String spec, String method) throws Exception {
        var connection = (HttpURLConnection) URI.create(spec).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        return connection;
    }

    /**
     * 외부 API 응답을 분석하여 JAX-RS Response로 변환 (중복 로직 통합)
     */
    private Response buildProxyResponse(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();

        // 성공 시 InputStream, 실패 시 ErrorStream 선택
        InputStream responseStream = (status >= 200 && status < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        StreamingOutput output = out -> {
            try (InputStream is = responseStream) {
                if (is != null) {
                    is.transferTo(out); // Java 9+ 제공 메서드
                    out.flush();
                }
            }
        };

        return Response.status(status)
                .entity(output)
                .header(HttpHeaders.CONTENT_TYPE, connection.getContentType())
                .build();
    }

    private Response handleProxyError(Exception e) {
        e.printStackTrace();
        return Response.serverError()
                .entity("Proxy Error: " + e.getMessage())
                .build();
    }
}