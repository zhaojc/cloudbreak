package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.periscope.api.model.ExceptionResult;

@Provider
public class UnsupportedOperationFailedExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsupportedOperationFailedExceptionMapper.class);

    @Override
    public Response toResponse(UnsupportedOperationException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ExceptionResult(exception.getMessage()))
                .build();
    }
}