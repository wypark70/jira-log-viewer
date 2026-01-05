package com.atsoft.jira.plugin.logviewer.rest

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import javax.ws.rs.*
import javax.ws.rs.core.Response

@Path("/labelit/items")
class ProxyResource {

    companion object {
        const val APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8"
        const val CONTENT_TYPE = "Content-Type"
        const val ACCEPT = "Accept"
        const val EXTERNAL_API_URL = "https://api.example.com/data"
    }

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    private fun forwardRequest(request: HttpRequest): Response {
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))

        val builder = Response.status(response.statusCode())
            .entity(response.body())
            .header(CONTENT_TYPE, APPLICATION_JSON_UTF8)

        response.headers().map().forEach { (key, values) ->
            values.forEach { value -> builder.header(key, value) }
        }

        return builder.build()
    }

    @GET
    @Produces(APPLICATION_JSON_UTF8)
    fun callExternalApiGet(): Response {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(EXTERNAL_API_URL))
            .GET()
            .header(ACCEPT, APPLICATION_JSON_UTF8)
            .build()

        return forwardRequest(request)
    }

    @POST
    @Consumes(APPLICATION_JSON_UTF8)
    @Produces(APPLICATION_JSON_UTF8)
    fun callExternalApiPost(body: String): Response {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(EXTERNAL_API_URL))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .header(CONTENT_TYPE, APPLICATION_JSON_UTF8)
            .header(ACCEPT, APPLICATION_JSON_UTF8)
            .build()

        return forwardRequest(request)
    }

}