package com.atsoft.jira.plugin.logviewer.rest

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response

@Path("/labelit/items")
class ProxyResource {

    companion object {
        const val APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8"
        const val CONTENT_TYPE = "Content-Type"
        const val ACCEPT = "Accept"
        const val EXTERNAL_API_URL = "https://api.example.com/data"
    }

    private val httpClient = HttpClient.newHttpClient()

    private fun forwardRequest(request: HttpRequest): Response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).let { resp ->
            Response.status(resp.statusCode())
                .entity(resp.body())
                .apply {
                    header(CONTENT_TYPE, APPLICATION_JSON_UTF8)
                    resp.headers().map().forEach { (key, values) ->
                        values.forEach { header(key, it) }
                    }
                }
                .build()
        }

    /**
     * Forwards external GET request with JSON headers
     */
    @GET
    @Produces(APPLICATION_JSON_UTF8)
    fun get(): Response =
        HttpRequest.newBuilder()
            .uri(URI.create(EXTERNAL_API_URL))
            .GET()
            .header(ACCEPT, APPLICATION_JSON_UTF8)
            .build()
            .let(::forwardRequest)

    /**
     * Forwards external POST request with JSON body
     */
    @POST
    @Consumes(APPLICATION_JSON_UTF8)
    @Produces(APPLICATION_JSON_UTF8)
    fun post(body: String): Response =
        HttpRequest.newBuilder()
            .uri(URI.create(EXTERNAL_API_URL))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .header(CONTENT_TYPE, APPLICATION_JSON_UTF8)
            .header(ACCEPT, APPLICATION_JSON_UTF8)
            .build()
            .let(::forwardRequest)
}