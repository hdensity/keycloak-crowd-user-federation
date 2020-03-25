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

package com.github.hdensity.keycloak.storage.crowd;

import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.ExpiredCredentialException;
import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.model.user.UserWithAttributes;
import com.atlassian.crowd.search.query.entity.restriction.BooleanRestrictionImpl;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.PropertyImpl;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.service.client.CrowdClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.crowd.search.query.entity.restriction.BooleanRestriction.BooleanLogic.OR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrowdStorageProviderTest {

    @Mock private KeycloakSession sessionMock;
    @Mock private ComponentModel modelMock;
    @Mock private CrowdClient clientMock;

    private CrowdStorageProvider crowdStorageProvider;

    @Mock private RealmModel realmModelMock;
    private static final String USERNAME = "username";

    @BeforeEach
    void setup() {
        crowdStorageProvider = spy(new CrowdStorageProvider(sessionMock, modelMock, clientMock));
    }

    // UserLookupProvider methods

    @Test
    void given_knownCrowdUser_when_getUserByUsername_then_expectedValueIsReturned() throws Exception {
        UserWithAttributes crowdUserMock = mock(UserWithAttributes.class);
        when(clientMock.getUserWithAttributes(USERNAME)).thenReturn(crowdUserMock);

        assertThat(crowdStorageProvider.getUserByUsername(USERNAME, realmModelMock))
                .isExactlyInstanceOf(CrowdUserAdapter.class)
                .extracting("entity")
                .isEqualTo(crowdUserMock);
    }

    @Test
    void given_getUserWithAttributesThrowsUserNotFoundException_when_getUserByUsername_then_nullIsReturned() throws Exception {
        when(clientMock.getUserWithAttributes(USERNAME)).thenThrow(new UserNotFoundException("Boom!"));

        assertThat(crowdStorageProvider.getUserByUsername(USERNAME, realmModelMock)).isNull();
    }

    @Test
    void given_getUserWithAttributesThrowsOperationFailedException_when_getUserByUsername_then_exceptionIsThrown() throws Exception {
        runGetUserByUsernameExceptionTest(new OperationFailedException());
    }

    @Test
    void given_getUserWithAttributesThrowsInvalidAuthenticationException_when_getUserByUsername_then_exceptionIsThrown() throws Exception {
        runGetUserByUsernameExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_getUserWithAttributesThrowsApplicationPermissionException_when_getUserByUsername_then_exceptionIsThrown() throws Exception {
        runGetUserByUsernameExceptionTest(new ApplicationPermissionException());
    }

    private void runGetUserByUsernameExceptionTest(Exception exception) throws Exception {
        when(clientMock.getUserWithAttributes(USERNAME)).thenThrow(exception);

        assertThatThrownBy(() -> crowdStorageProvider.getUserByUsername(USERNAME, realmModelMock))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

    @Test
    void when_getUserById_then_getUserByUsernameIsCalled() throws Exception {
        UserWithAttributes crowdUserMock = mock(UserWithAttributes.class);
        when(clientMock.getUserWithAttributes(USERNAME)).thenReturn(crowdUserMock);

        crowdStorageProvider.getUserById("f:42:" + USERNAME, realmModelMock);

        verify(crowdStorageProvider).getUserByUsername(USERNAME, realmModelMock);
    }

    @Test
    void when_getUserByEmail_then_searchForUserIsCalled() {
        crowdStorageProvider.getUserByEmail("email", realmModelMock);

        Map<String, String> params = new HashMap<>();
        params.put("email", "email");

        verify(crowdStorageProvider).searchForUser(params, realmModelMock, 0, 1);
    }

    // UserQueryProvider methods

    @Test
    void when_getUsersCount_then_expectedResultIsReturned() throws Exception {
        List<String> expectedResult = new ArrayList<>();
        expectedResult.add("user1");
        expectedResult.add("user2");

        when(clientMock.searchUserNames(CrowdStorageProvider.NOOP_SEARCH_RESTRICTION, 0, Integer.MAX_VALUE)).thenReturn(expectedResult);

        assertThat(crowdStorageProvider.getUsersCount(realmModelMock)).isEqualTo(2);
    }

    @Test
    void given_searchUserNamesThrowsOperationFailedException_when_getUsersCount_then_ExceptionsIsThrown() throws Exception {
        runGetUsersCountExceptionTest(new OperationFailedException());
    }

    @Test
    void given_searchUserNamesThrowsInvalidAuthenticationException_when_getUsersCount_then_ExceptionsIsThrown() throws Exception {
        runGetUsersCountExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_searchUserNamesThrows_when_getUsersCount_then_ExceptionsIsThrown() throws Exception {
        runGetUsersCountExceptionTest(new ApplicationPermissionException());
    }

    private void runGetUsersCountExceptionTest(Exception exception) throws Exception {
        when(clientMock.searchUserNames(CrowdStorageProvider.NOOP_SEARCH_RESTRICTION, 0, Integer.MAX_VALUE)).thenThrow(exception);

        assertThatThrownBy(() -> crowdStorageProvider.getUsersCount(realmModelMock))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

    @Test
    void when_getUsersWithoutLimits_then_getUsersWithLimitsIsCalled() {
        crowdStorageProvider.getUsers(realmModelMock);

        verify(crowdStorageProvider).getUsers(realmModelMock, 0, Integer.MAX_VALUE);
    }

    @Test
    void when_getUsersWithLimits_then_searchForUsersIsCalled() {
        crowdStorageProvider.getUsers(realmModelMock, 0, Integer.MAX_VALUE);

        verify(crowdStorageProvider).searchForUser("", realmModelMock, 0, Integer.MAX_VALUE);
    }

    @Test
    void when_searchForUserWithoutLimits_then_searchForUserWithLimitsIsCalled() {
        crowdStorageProvider.searchForUser("search", realmModelMock);

        verify(crowdStorageProvider).searchForUser("search", realmModelMock, 0, Integer.MAX_VALUE);
    }

    @Test
    void when_searchForUserWithString_then_searchForUserWithParamsIsCalled() {
        crowdStorageProvider.searchForUser("search", realmModelMock, 0, Integer.MAX_VALUE);

        Map<String, String> params = new HashMap<>();
        params.put("first", "search");
        params.put("last", "search");
        params.put("email", "search");
        params.put("username", "search");

        verify(crowdStorageProvider).searchForUser(params, realmModelMock, 0, Integer.MAX_VALUE);
    }

    @Test
    void when_searchForUserWithParamsAndNoLimits_then_searchForUserWithParamsAndLimitsIsCalled() {
        crowdStorageProvider.searchForUser(new HashMap<>(), realmModelMock);

        verify(crowdStorageProvider).searchForUser(new HashMap<>(), realmModelMock, 0, Integer.MAX_VALUE);
    }

    // TODO searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults)

    @Test
    void given_emptyParamsMap_when_searchForUser_then_expectedValuesAreReturned() throws Exception {
        UserWithAttributes userMock = mock(UserWithAttributes.class);

        List<UserWithAttributes> users = new ArrayList<>();
        users.add(userMock);

        when(clientMock.searchUsersWithAttributes(CrowdStorageProvider.NOOP_SEARCH_RESTRICTION, 0, Integer.MAX_VALUE))
                .thenReturn(users);

        assertThat(crowdStorageProvider.searchForUser(new HashMap<>(), realmModelMock, 0, Integer.MAX_VALUE))
                .hasSize(1)
                .element(0)
                .extracting("entity")
                .isEqualTo(userMock);
    }

    @Test
    void given_paramsMap_when_searchForUser_then_expectedValuesAreReturned() throws Exception {
        UserWithAttributes userMock = mock(UserWithAttributes.class);

        List<UserWithAttributes> users = new ArrayList<>();
        users.add(userMock);

        Map<String, String> params = new HashMap<>();
        params.put("first", "name");
        params.put("attr", "value");

        SearchRestriction searchRestriction = new BooleanRestrictionImpl(
                OR,
                new TermRestriction<>(
                        new PropertyImpl<>("firstName", String.class),
                        MatchMode.CONTAINS,
                        "name"),
                new TermRestriction<>(
                        new PropertyImpl<>("attr", String.class),
                        MatchMode.CONTAINS,
                        "value"));

        when(clientMock.searchUsersWithAttributes(searchRestriction, 0, Integer.MAX_VALUE))
                .thenReturn(users);

        assertThat(crowdStorageProvider.searchForUser(params, realmModelMock, 0, Integer.MAX_VALUE))
                .hasSize(1)
                .element(0)
                .extracting("entity")
                .isEqualTo(userMock);
    }

    @Test
    void given_searchUsersWithAttributesThrowsInvalidAuthenticationException_when_searchForUser_then_ExceptionsIsThrown() throws Exception {
        runSearchForUserExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_searchUsersWithAttributesThrowsOperationFailedException_when_searchForUser_then_ExceptionsIsThrown() throws Exception {
        runSearchForUserExceptionTest(new OperationFailedException());
    }

    @Test
    void given_searchUsersWithAttributesThrowsApplicationPermissionException_when_searchForUser_then_ExceptionsIsThrown() throws Exception {
        runSearchForUserExceptionTest(new ApplicationPermissionException());
    }

    private void runSearchForUserExceptionTest(Exception exception) throws Exception {
        when(clientMock.searchUsersWithAttributes(CrowdStorageProvider.NOOP_SEARCH_RESTRICTION, 0, Integer.MAX_VALUE))
                .thenThrow(exception);

        assertThatThrownBy(() -> crowdStorageProvider.searchForUser(
                        new HashMap<>(), realmModelMock, 0, Integer.MAX_VALUE))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

    @Test
    void when_searchForUserByUserAttribute_then_searchForUserIsCalled() {
        crowdStorageProvider.searchForUserByUserAttribute("attr", "value", realmModelMock);

        Map<String, String> params = new HashMap<>();
        params.put("attr", "value");

        verify(crowdStorageProvider).searchForUser(params, realmModelMock, 0, Integer.MAX_VALUE);
    }

    @Test
    void when_getGroupMembers_then_getGroupMembersWithLimitsIsCalled() {
        GroupModel groupMock = mock(GroupModel.class);
        crowdStorageProvider.getGroupMembers(realmModelMock, groupMock);

        verify(crowdStorageProvider).getGroupMembers(realmModelMock, groupMock, 0, Integer.MAX_VALUE);
    }

    @Test
    void when_getGroupMembersWithLimits_then_expectedResultIsReturned() throws Exception {
        GroupModel groupMock = mock(GroupModel.class);
        when(groupMock.getName()).thenReturn("group name");

        List<User> userList = new ArrayList<>();
        userList.add(mock(UserWithAttributes.class));

        when(clientMock.getUsersOfGroup("group name", 0, Integer.MAX_VALUE)).thenReturn(userList);

        List<UserModel> groupMembers = crowdStorageProvider.getGroupMembers(
                realmModelMock, groupMock, 0, Integer.MAX_VALUE);
        assertThat(groupMembers.get(0)).extracting("entity").isEqualTo(userList.get(0));
    }

    @Test
    void given_unknownGroup_when_getGroupMembersWithLimits_then_emptyResultIsReturned() throws Exception {
        GroupModel groupMock = mock(GroupModel.class);
        when(groupMock.getName()).thenReturn("group name");

        when(clientMock.getUsersOfGroup("group name", 0, Integer.MAX_VALUE))
                .thenThrow(new GroupNotFoundException("Boom!"));

        assertThat(crowdStorageProvider.getGroupMembers(realmModelMock, groupMock, 0, Integer.MAX_VALUE))
                .isEmpty();
    }

    @Test
    void given_getUsersOfGroupThrowsApplicationPermissionException_when_getGroupMembersWithLimits_then_exceptionIsThrown() throws Exception {
        runGetGroupMembersWithLimitsExceptionTest(new ApplicationPermissionException());
    }

    @Test
    void given_getUsersOfGroupThrowsInvalidAuthenticationException_when_getGroupMembersWithLimits_then_exceptionIsThrown() throws Exception {
        runGetGroupMembersWithLimitsExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_getUsersOfGroupThrowsOperationFailedException_when_getGroupMembersWithLimits_then_exceptionIsThrown() throws Exception {
        runGetGroupMembersWithLimitsExceptionTest(new OperationFailedException());
    }

    private void runGetGroupMembersWithLimitsExceptionTest(Exception exception) throws Exception {
        GroupModel groupMock = mock(GroupModel.class);
        when(groupMock.getName()).thenReturn("group name");

        when(clientMock.getUsersOfGroup("group name", 0, Integer.MAX_VALUE)).thenThrow(exception);

        assertThatThrownBy(() -> crowdStorageProvider.getGroupMembers(realmModelMock, groupMock, 0, Integer.MAX_VALUE))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

    // CredentialInputValidator methods

    @Test
    void when_isConfiguredFor_then_supportsCredentialTypeIsCalled() {
        crowdStorageProvider.isConfiguredFor(realmModelMock, mock(UserModel.class), "type");

        verify(crowdStorageProvider).supportsCredentialType("type");
    }

    @Test
    void given_passwordCredentialType_when_supportsCredentialType_then_trueIsReturned() {
        assertThat(crowdStorageProvider.supportsCredentialType(PasswordCredentialModel.TYPE)).isTrue();
    }

    @Test
    void given_otherCredentialType_when_supportsCredentialType_then_falseIsReturned() {
        assertThat(crowdStorageProvider.supportsCredentialType(OTPCredentialModel.TYPE)).isFalse();
    }

    @Test
    void given_unsupportedCredentialType_when_isValid_then_falseIsReturned() {
        CredentialInput input = new UserCredentialModel("id", OTPCredentialModel.TYPE, "otp");
        assertThat(crowdStorageProvider.isValid(realmModelMock, mock(UserModel.class), input)).isFalse();
    }

    @Test
    void given_validCredentials_when_isValid_then_trueIsReturned() throws Exception {
        runIsValidTest(mock(User.class), true);
    }

    @Test
    void given_invalidCredentials_when_isValid_then_falseIsReturned() throws Exception {
        runIsValidTest(null, false);
    }

    private void runIsValidTest(User expectedUser, boolean expectedResult) throws Exception {
        CredentialInput input = new UserCredentialModel(
                "id", PasswordCredentialModel.TYPE, "password");

        when(clientMock.authenticateUser(USERNAME, "password")).thenReturn(expectedUser);

        UserModel userMock = mock(UserModel.class);
        when(userMock.getUsername()).thenReturn(USERNAME);

        assertThat(crowdStorageProvider.isValid(realmModelMock, userMock, input)).isEqualTo(expectedResult);
    }

    @Test
    void given_authenticateUserThrowsInactiveAccountException_when_isValid_then_falseIsReturned() throws Exception {
        runAuthenticateUserFalseTest(new InactiveAccountException("Boom!"));
    }

    @Test
    void given_authenticateUserThrowsUserNotFoundException_when_isValid_then_falseIsReturned() throws Exception {
        runAuthenticateUserFalseTest(new UserNotFoundException("Boom!"));
    }

    @Test
    void given_authenticateUserThrowsExpiredCredentialException_when_isValid_then_falseIsReturned() throws Exception {
        runAuthenticateUserFalseTest(new ExpiredCredentialException());
    }

    private void runAuthenticateUserFalseTest(Exception exception) throws Exception {
        CredentialInput input = new UserCredentialModel(
                "id", PasswordCredentialModel.TYPE, "password");

        when(clientMock.authenticateUser(USERNAME, "password")).thenThrow(exception);

        UserModel userMock = mock(UserModel.class);
        when(userMock.getUsername()).thenReturn(USERNAME);

        assertThat(crowdStorageProvider.isValid(realmModelMock, userMock, input)).isFalse();
    }

    @Test
    void given_authenticateUserThrowsApplicationPermissionException_when_isValid_then_exceptionIsThrown() throws Exception {
        runAuthenticateUserExceptionTest(new ApplicationPermissionException());
    }

    @Test
    void given_authenticateUserThrowsInvalidAuthenticationException_when_isValid_then_exceptionIsThrown() throws Exception {
        runAuthenticateUserExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_authenticateUserThrowsOperationFailedException_when_isValid_then_exceptionIsThrown() throws Exception {
        runAuthenticateUserExceptionTest(new OperationFailedException());
    }

    private void runAuthenticateUserExceptionTest(Exception exception) throws Exception {
        CredentialInput input = new UserCredentialModel(
                "id", PasswordCredentialModel.TYPE, "password");

        when(clientMock.authenticateUser(USERNAME, "password")).thenThrow(exception);

        UserModel userMock = mock(UserModel.class);
        when(userMock.getUsername()).thenReturn(USERNAME);

        assertThatThrownBy(() -> crowdStorageProvider.isValid(realmModelMock, userMock, input))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

    // Provider method implementations

    @Test
    void when_close_then_noDependencyIsCalled() {
        crowdStorageProvider.close();

        verifyNoInteractions(sessionMock, modelMock, clientMock);
    }

}
