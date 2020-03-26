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

package it.schm.keycloak.storage.crowd;

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
import it.schm.keycloak.storage.crowd.group.CrowdGroupMapper;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
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

    /**
     * A Crowd search restriction matching everything, used in cases where an unrestricted Keycloak method is mapped
     * to a search in Crowd.
     */
    protected static final SearchRestriction NOOP_SEARCH_RESTRICTION =
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

    /**
     * Retrieves a user by its username.
     *
     * @param username The username of the user to retrieve
     * @param realm The realm from which to retrieve the user.
     * @return The user with the given username, if found, null otherwise
     */
    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        try {
            return convertToKeycloakUser(realm, client.getUserWithAttributes(username));
        } catch (UserNotFoundException e) {
            return null;
        } catch (OperationFailedException | InvalidAuthenticationException | ApplicationPermissionException e) {
            logger.error(e);
            throw new ModelException(e);
        }
    }

    /**
     * Retrieves a user by its id.
     *
     * @param id The id of the user to retrieve
     * @param realm The realm from which to retrieve the user.
     * @return The user with the given id, if found, null otherwise
     */
    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return getUserByUsername(StorageId.externalId(id), realm);
    }

    /**
     * Retrieves a user by its email address.
     *
     * @param email The email address of the user to retrieve
     * @param realm The realm from which to retrieve the user.
     * @return The user with the given email address, if found, null otherwise
     */
    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        Map<String, String> params = new HashMap<>();
        params.put("email", email);

        return searchForUser(params, realm, 0, 1).stream().findFirst().orElse(null);
    }

    // UserQueryProvider methods

    /**
     * Returns the number of users, without considering any service account.
     *
     * @param realm the realm
     * @return the number of users
     */
    @Override
    public int getUsersCount(RealmModel realm) {
        try {
            return client.searchUserNames(NOOP_SEARCH_RESTRICTION, 0, Integer.MAX_VALUE).size();
        } catch (OperationFailedException | InvalidAuthenticationException | ApplicationPermissionException e) {
            logger.error(e);
            throw new ModelException(e);
        }
    }

    /**
     * Retrieves all users of the given realm.
     *
     * @param realm the realm for which to retrieve users
     * @return the list of users of this realm
     */
    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, 0, Integer.MAX_VALUE);
    }

    /**
     * Retrieves a maximum of {@code maxResult} users of the given realm, starting at index {@code firstResult}.
     *
     * @param realm the realm for which to retrieve users
     * @param firstResult the index of the first user to retrieve
     * @param maxResults the number of users to retrieve
     * @return a list of users
     */
    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return searchForUser("", realm, firstResult, maxResults);
    }

    /**
     * Search for users with username, email or first + last name that is like search string.
     *
     * @param search the search string to use
     * @param realm the realm in which to search for users
     * @return the list of users matching the given search string
     */
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE);
    }

    /**
     * Search for users with username, email or first + last name that is like search string.
     *
     * @param search the search string to use
     * @param realm the realm in which to search for users
     * @param firstResult the index of the first user to retrieve
     * @param maxResults the number of users to retrieve
     * @return the list of users matching the given search string
     */
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        Map<String, String> params = new HashMap<>();
        params.put("first", search);
        params.put("last", search);
        params.put("email", search);
        params.put("username", search);

        return searchForUser(params, realm, firstResult, maxResults);
    }

    /**
     * Search for user by parameter. Valid parameters are:
     * <ul>
     * <li>"first" - first name</li>
     * <li>"last" - last name</li>
     * <li>"email" - email</li>
     * <li>"username" - username</li>
     * </ul>
     *
     * @param params the parameters to match against
     * @param realm the realm in which to search for users
     * @return the list of users matching the given parameters
     */
    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    /**
     * Search for user by parameter. Valid parameters are:
     * <ul>
     * <li>"first" - first name</li>
     * <li>"last" - last name</li>
     * <li>"email" - email</li>
     * <li>"username" - username</li>
     * </ul>
     *
     * @param params the parameters to match against
     * @param realm the realm in which to search for users
     * @param firstResult the index of the first user to retrieve
     * @param maxResults the number of users to retrieve
     * @return the list of users matching the given parameters
     */
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
            throw new ModelException(e);
        }
    }

    /**
     * Search for users that have a specific attribute with a specific value.
     *
     * @param attrName the attribute name to search for
     * @param attrValue the attribute value to search for
     * @param realm the realm in which to search for users
     * @return the list of users matching the given attribute and attribute value
     */
    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        Map<String, String> params = new HashMap<>();
        params.put(attrName, attrValue);

        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    /**
     * Get users that belong to a specific group.
     *
     * @param realm the realm in which to search for users
     * @param group the group for which to retrieve users
     * @return the list of users of the given group
     */
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, 0, Integer.MAX_VALUE);
    }

    /**
     * Get users that belong to a specific group.
     *
     * @param realm the realm in which to search for users
     * @param group the group for which to retrieve users
     * @param firstResult the index of the first user to retrieve
     * @param maxResults the number of users to retrieve
     * @return the list of users of the given group
     */
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
            throw new ModelException(e);
        }
    }

    // CredentialInputValidator methods

    /**
     * Checks whether the provider is configured for the specified credential type.
     *
     * @param realm the realm to check for
     * @param user the user to check for
     * @param credentialType the credential type to check for
     * @return true if the provider is configured for the specified credential type, false otherwise
     */
    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    /**
     * Checks whether the provider supports the given credential type.
     *
     * @param credentialType the credential type to check.
     * @return true, if the given credential type is {@code PasswordCredentialModel.TYPE}, false otherwise.
     */
    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    /**
     * Tests whether a credential is valid.
     *
     * @param realm The realm in which to which the credential belongs to
     * @param user The user for which to test the credential
     * @param input the credential details to verify
     * @return true if the passed secret is correct
     */
    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) {
            return false;
        }

        try {
            return client.authenticateUser(user.getUsername(), input.getChallengeResponse()) != null;
        } catch (InactiveAccountException | UserNotFoundException | ExpiredCredentialException e) {
            return false;
        } catch (ApplicationPermissionException | InvalidAuthenticationException | OperationFailedException e) {
            logger.error(e);
            throw new ModelException(e);
        }
    }

    // Provider method implementations

    /**
     * Close the provider and free up resources.
     */
    @Override
    public void close() {
        // no-op
    }

    // helpers

    private CrowdUserAdapter convertToKeycloakUser(RealmModel realm, UserWithAttributes user) {
        return new CrowdGroupMapper(model, client).onLoadUser(new CrowdUserAdapter(session, realm, model, user));
    }

}
