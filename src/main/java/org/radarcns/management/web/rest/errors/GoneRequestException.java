package org.radarcns.management.web.rest.errors;

import java.util.Map;
import javax.ws.rs.core.Response;

public class GoneRequestException extends RadarWebApplicationException{
    /**
     * Create a custom conflict exception with the given message, parameter map, and url to the
     * conflicting resource.
     * @param message the message
     */
    public GoneRequestException(String message, String entityName, String errorCode) {
        super(Response.Status.GONE, message, entityName, errorCode);
    }

    /**
     * Create a custom conflict exception with the given message, parameter map, and url to the
     * conflicting resource.
     * @param message the message
     * @param paramMap the parameter map
     */
    public GoneRequestException(String message, String entityName, String errorCode,
        Map<String, String> paramMap) {
        super(Response.Status.GONE, message, entityName, errorCode, paramMap);
    }
}
