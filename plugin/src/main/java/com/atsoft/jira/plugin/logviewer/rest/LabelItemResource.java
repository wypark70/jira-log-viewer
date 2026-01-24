package com.atsoft.jira.plugin.logviewer.rest;

import com.atsoft.jira.plugin.logviewer.dto.LabelItemDto;
import com.atsoft.jira.plugin.logviewer.service.LabelItemDbService;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import javax.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/label-item")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LabelItemResource {
    private final LabelItemDbService service;
    private final GlobalPermissionManager globalPermissionManager;
    private final JiraAuthenticationContext authenticationContext;

    @Inject
    public LabelItemResource(LabelItemDbService service, GlobalPermissionManager globalPermissionManager,
            JiraAuthenticationContext authenticationContext) {
        this.service = service;
        this.globalPermissionManager = globalPermissionManager;
        this.authenticationContext = authenticationContext;
    }

    private void checkAdminPermission() {
        ApplicationUser user = authenticationContext.getLoggedInUser();
        GlobalPermissionKey adminPermission = GlobalPermissionKey.ADMINISTER;

        if (user != null || adminPermission != null && !globalPermissionManager.hasPermission(adminPermission, user)) {
            throw new ForbiddenException("Admin permission required");
        }
    }

    @GET
    public Response getAll() {
        List<LabelItemDto> items = service.getAll();
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        LabelItemDto item = service.getById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(item).build();
    }

    @POST
    public Response create(LabelItemDto request) {
        checkAdminPermission();
        try {
            LabelItemDto item = service.create(request.getCustomFieldId(), request.getName(), request.getProjectId());
            return Response.status(Response.Status.CREATED).entity(item).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error creating item").build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") int id, LabelItemDto request) {
        checkAdminPermission();
        LabelItemDto item = service.update(id, request.getCustomFieldId(), request.getName(), request.getProjectId());
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(item).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        checkAdminPermission();
        service.delete(id);
        return Response.noContent().build();
    }
}