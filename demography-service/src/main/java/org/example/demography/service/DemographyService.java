package org.example.demography.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.example.demography.client.PeopleClient;

import java.util.Set;

@ApplicationScoped
public class DemographyService {
    private static final Set<String> HAIR_COLORS = Set.of("RED", "BLACK", "ORANGE", "WHITE", "BROWN");
    private static final Set<String> EYE_COLORS = Set.of("GREEN", "BLACK", "BLUE", "YELLOW", "ORANGE");

    @Inject
    private PeopleClient peopleClient;

    public double hairColorPercentage(String color) {
        if (!HAIR_COLORS.contains(color)) {
            throw new BadRequestException("Unknown hair color");
        }
        try {
            return peopleClient.hairColorPercentage(color);
        } catch (ProcessingException e) {
            throw new ServiceUnavailableException(Response.status(503).build(), e);
        }
    }

    public double eyeColorPercentage(String color) {
        if (!EYE_COLORS.contains(color)) {
            throw new BadRequestException("Unknown eye color");
        }
        try {
            return peopleClient.eyeColorPercentage(color);
        } catch (ProcessingException e) {
            throw new ServiceUnavailableException(Response.status(503).build(), e);
        }
    }
}
