package ru.devinfra.keycloak.auth.requirerole;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

/**
 * checks if the user has a given role and return correct error for usage in direct grant flow.
 */
public class RequireRoleDirectGrantAuthenticator extends RequireRoleAuthenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        ClientModel client = context.getAuthenticationSession().getClient();
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();

        String roleName = resolveRoleName(authenticatorConfig.getConfig(), client);
        if (roleName == null) {
            context.success();
            return;
        }

        RoleModel requiredRole = resolveRequiredRole(roleName, realm, client);
        if (requiredRole == null) {
            context.success();
            return;
        }

        if (isUserInRole(user, requiredRole)) {
            context.success();
            return;
        }

        LOG.infof("Access denied because of missing role. realm=%s username=%s role=%s", realm.getName(), user.getUsername(), roleName);
        String responsePhrase = String.format("Access denied because of missing role. realm=%s username=%s role=%s", realm.getName(), user.getUsername(), roleName);

        Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "missing_role", responsePhrase);
        context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
    }

    private Response errorResponse(int status, String error, String errorDescription) {
        OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(error, errorDescription);
        return Response.status(status).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
