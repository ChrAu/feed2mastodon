package de.hexix.user;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Path("/api/user")
public class UserResource {

    private static final Set<String> ALLOWED_REDIRECT_PATHS = Collections.unmodifiableSet(Set.of(
            "/",
            "/profile",
            "/dashboard"
    ));

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();

        if (securityIdentity.isAnonymous()) {
            userInfo.put("loggedIn", false);
            return userInfo;
        }

        userInfo.put("loggedIn", true);
        userInfo.put("username", securityIdentity.getPrincipal().getName()); // Standardmäßig oft preferred_username
        userInfo.put("roles", securityIdentity.getRoles());

        Principal principal = securityIdentity.getPrincipal();
        if (principal instanceof JsonWebToken jwt) {

            userInfo.put("id", jwt.getClaim("sub")); // Subject (Benutzer-ID)
            userInfo.put("givenName", jwt.getClaim("given_name"));
            userInfo.put("familyName", jwt.getClaim("family_name"));
            userInfo.put("email", jwt.getClaim("email"));
            userInfo.put("emailVerified", jwt.getClaim("email_verified"));
            userInfo.put("preferredUsername", jwt.getClaim("preferred_username"));
        }


        return userInfo;
    }

    @GET
    @Path("/login")
    @Authenticated // Das hier triggert den Redirect zu Keycloak
    public Response login(@QueryParam("redirect") String redirect) {
        // Validierung: Nur explizit freigegebene interne Pfade erlauben (Open Redirect Protection!)
        String target = (redirect != null && ALLOWED_REDIRECT_PATHS.contains(redirect)) ? redirect : "/";
        return Response.seeOther(URI.create(target)).build();
    }
}
