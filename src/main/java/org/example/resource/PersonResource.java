package org.example.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.example.model.Person;
import org.example.service.PersonService;

import java.net.URI;
import java.util.Map;

@Path("/people")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonResource {

    @Inject
    private PersonService personService;

    @POST
    public Response create(Person person,
                           @Context UriInfo uriInfo) {
        Person createdPerson = personService.create(person);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(createdPerson.getId().toString())
                .build();

        return Response.created(location)
                .entity(createdPerson)
                .build();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        Person person = personService.findById(id);

        if (person == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "code", 404,
                            "message", "Person not found"
                    ))
                    .build();
        }

        return Response.ok(person).build();
    }


    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id,
                           Person person) {
        Person updatedPerson = personService.update(id, person);

        if (updatedPerson == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "code", 404,
                            "message", "Person not found"
                    ))
                    .build();
        }

        return Response.ok(updatedPerson).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        boolean deleted = personService.delete(id);

        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "code", 404,
                            "message", "Person not found"
                    ))
                    .build();
        }

        return Response.noContent().build();
    }


    @GET
    public Response findAll(@Context UriInfo uriInfo) {
        return Response.ok(personService.findAll(uriInfo.getQueryParameters())).build();
    }

    @GET
    @Path("/eye-color-min")
    public Response findWithMinEyeColor() {
        Person person = personService.findWithMinEyeColor();
        return person == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(person).build();
    }

    @GET
    @Path("/id-max")
    public Response findWithMaxId() {
        Person person = personService.findWithMaxId();
        return person == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(person).build();
    }

    @GET
    @Path("/eye-color-greater/count")
    public Response countEyeColorsGreater(@QueryParam("color") String color) {
        return Response.ok(personService.countEyeColorsGreater(color)).build();
    }
}
