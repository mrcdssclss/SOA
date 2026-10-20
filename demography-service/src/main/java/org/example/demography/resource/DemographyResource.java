package org.example.demography.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.demography.service.DemographyService;

@Path("/demography")
@Produces(MediaType.APPLICATION_JSON)
public class DemographyResource {
    @Inject
    private DemographyService demographyService;

    @GET
    @Path("/hair-color/{hair-color}/percentage")
    public Response hairColorPercentage(@PathParam("hair-color") String hairColor) {
        return Response.ok(demographyService.hairColorPercentage(hairColor)).build();
    }

    @GET
    @Path("/eye-color/{eye-color}/percentage")
    public Response eyeColorPercentage(@PathParam("eye-color") String eyeColor) {
        return Response.ok(demographyService.eyeColorPercentage(eyeColor)).build();
    }
}
