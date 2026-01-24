package com.atsoft.jira.plugin.logviewer.rest

import com.atsoft.jira.plugin.logviewer.service.LogViewerSettingsService
import java.io.File
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/files")
class LogViewerResource(
    private val settingsService: LogViewerSettingsService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getFiles(@QueryParam("directory") directory: String?): Response {
        val targetDir = if (!directory.isNullOrBlank()) {
            File(directory)
        } else {
            // Default to the parent directory of the configured log file, or user home if not set
            val configuredPath = settingsService.getLogFilePath()
            if (configuredPath.isNotBlank()) {
                File(configuredPath).parentFile ?: File(System.getProperty("user.home"))
            } else {
                 File(System.getProperty("user.home"))
            }
        }

        if (!targetDir.exists() || !targetDir.isDirectory) {
             return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid directory path: ${targetDir.absolutePath}")
                .build()
        }
        
        val files = targetDir.listFiles()?.map { file ->
            FileModel(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                modified = file.lastModified(),
                isDirectory = file.isDirectory
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()

        return Response.ok(files).build()
    }
}
