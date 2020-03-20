/*
 * Copyright © 2020 Sam Schmit
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package it.schmit.keycloak.storage.crowd;

import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.ExpiredCredentialException;
import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.user.UserWithAttributes;
import com.atlassian.crowd.search.query.entity.restriction.BooleanRestrictionImpl;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.PropertyImpl;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.service.client.CrowdClient;
import it.schmit.keycloak.storage.crowd.group.CrowdGroupMapper;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.crowd.search.query.entity.restriction.BooleanRestriction.BooleanLogic.OR;
import static java.util.stream.Collectors.toList;

/**
 * UserStorageProvider implementation providing read-only user federation to an Atlassian Crowd deployment.
 *
 * @author Sam Schmit
 * @since 1.0.0
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/storage/UserStorageProvider.html">org.keycloak.storage.UserStorageProvider</a>
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/storage/user/UserLookupProvider.html">org.keycloak.storage.user.UserLookupProvider</a>
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/storage/user/UserQueryProvider.html">org.keycloak.storage.user.UserQueryProvider</a>
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/credential/CredentialInputValidator.html">org.keycloak.credential.CredentialInputValidator</a>
 */
public class CrowdStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(CrowdStorageProvider.class);

    private static final TermRestriction<String> NOOP_SEARCH_RESTRICTION =
            new TermRestriction<>(new PropertyImpl<>("name", String.class), MatchMode.CONTAINS, "");

    private static final Map<String, String> PARAM_MAP;

    static {
        PARAM_MAP = new HashMap<>();
        PARAM_MAP.put("first", "firstName");
        PARAM_MAP.put("last", "lastName");
        PARAM_MAP.put("username", "name");
    }

    private KeycloakSession session;
    private CrowdClient client;
    private ComponentModel model;

    /**
     * Creates a new instance of this provider.
     *
     * @param session the Keycloak session
     * @param model the provider's component model
     * @param client the crowd rest client
     */
    public CrowdStorageProvider(KeycloakSession session, ComponentModel model, CrowdClient client) {
        this.session = session;
        this.model = model;
        this.client = client;
    }

    // UserLookupProvider methods

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        try {
            return convertToKeycloakUser(realm, client.getUserWithAttributes(username));
        } catch (UserNotFoundException e) {
            return null;
        } catch (OperationFailedException | InvalidAuthenticationException | ApplicationPermissionException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return getUserByUsername(StorageId.externalId(id), realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        Map<String, String> params = new HashMap<>();
        params.put("email", email);

        return searchForUser(params, realm, 0, 1)
                .stream().findFirst().orElse(null);
    }

    // UserQueryProvider methods

    @Override
    public int getUsersCount(RealmModel realm) {
        try {
            return client.searchUserNames(NOOP_SEARCH_RESTRICTION, 0, Integer.MAX_VALUE).size();
        } catch (OperationFailedException | InvalidAuthenticationException | ApplicationPermissionException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return searchForUser("", realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        Map<String, String> params = new HashMap<>();
        params.put("first", search);
        params.put("last", search);
        params.put("email", search);
        params.put("username", search);

        return searchForUser(params, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(
            Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        SearchRestriction searchRestriction;

        if (params.isEmpty()) {
            searchRestriction = NOOP_SEARCH_RESTRICTION;
        } else {
            List<SearchRestriction> termRestrictions = params.entrySet().stream()
                    .map(param -> new TermRestriction<>(
                            new PropertyImpl<>(PARAM_MAP.getOrDefault(param.getKey(), param.getKey()), String.class),
                            MatchMode.CONTAINS,
                            param.getValue()))
                    .collect(toList());

            searchRestriction = new BooleanRestrictionImpl(OR, termRestrictions);
        }

        try {
            return client.searchUsersWithAttributes(searchRestriction, firstResult, maxResults)
                    .stream()
                    .map(user -> convertToKeycloakUser(realm, user))
                    .collect(toList());
        } catch (InvalidAuthenticationException | OperationFailedException | ApplicationPermissionException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        Map<String, String> params = new HashMap<>();
        params.put(attrName, attrValue);

        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        try {
            return client.getUsersOfGroup(group.getName(), firstResult, maxResults).stream()
                    .map(user -> convertToKeycloakUser(realm, (UserWithAttributes) user))
                    .collect(toList());
        } catch (GroupNotFoundException e) {
            return Collections.emptyList();
        } catch (ApplicationPermissionException | InvalidAuthenticationException | OperationFailedException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    // CredentialInputValidator methods

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) {
            return false;
        }

        try {
            return client.authenticateUser(user.getUsername(), input.getChallengeResponse()) != null;
        } catch (InactiveAccountException | UserNotFoundException e) {
            return false;
        } catch (ApplicationPermissionException | InvalidAuthenticationException
                | OperationFailedException | ExpiredCredentialException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    // Provider method implementations

    @Override
    public void close() {
        // no-op
    }

    // helpers

    private CrowdUserAdapter convertToKeycloakUser(RealmModel realm, UserWithAttributes user) {
        return new CrowdGroupMapper(model, client).onLoadUser(new CrowdUserAdapter(session, realm, model, user));
    }

}
