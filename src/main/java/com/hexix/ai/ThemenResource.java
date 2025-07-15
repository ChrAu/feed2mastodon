package com.hexix.ai;

import com.hexix.ai.dto.ThemenDTO;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Comparator;
import java.util.List;

@Path("/themen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ThemenResource {


    @GET
    @Transactional
    public List<ThemenDTO> getAll(){


        final List<ThemenEntity> themenEntities = ThemenEntity.<ThemenEntity>findAll().stream().sorted(Comparator.comparing(ThemenEntity::getLastPost, Comparator.nullsLast(Comparator.reverseOrder()))).toList();

        return themenEntities.stream().map(themenEntity -> new ThemenDTO(themenEntity.uuid, themenEntity.thema, themenEntity.lastPost)).toList();
    }


    @POST
    @Transactional
    @Consumes(MediaType.TEXT_PLAIN)
    public ThemenDTO create(String thema){

        final ThemenEntity themenEntity = new ThemenEntity();
        themenEntity.thema = thema;
        themenEntity.persist();
        return ThemenDTO.export(themenEntity);
    }

}
