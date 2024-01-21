package io.quarkiverse.jimmer.it.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkiverse.jimmer.it.service.IUserRoleService;

@Path("/userRoleResources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRoleResources {

    private final IUserRoleService iUserRoleService;

    public UserRoleResources(IUserRoleService iUserRoleService) {
        this.iUserRoleService = iUserRoleService;
    }

    @GET
    @Path("/userRole")
    public Response getBookById(@RestQuery String id) {
        return Response.ok(iUserRoleService.findById(id)).build();
    }
}
