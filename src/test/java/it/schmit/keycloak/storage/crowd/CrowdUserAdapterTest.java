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

import com.atlassian.crowd.model.user.UserWithAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrowdUserAdapterTest {

    @Mock private KeycloakSession sessionMock;
    @Mock private RealmModel realmMock;
    @Mock private ComponentModel modelMock;
    @Mock private UserWithAttributes userMock;

    private CrowdUserAdapter crowdUserAdapter;

    private static final String MODEL_ID = "model id";
    private static final String USER_NAME = "username";
    private static final String USER_EMAIL = "email@address.here";
    private static final String USER_FIRST_NAME = "first name";
    private static final String USER_LAST_NAME = "last name";

    @BeforeEach
    void setupGroupAdapter() {
        when(modelMock.getId()).thenReturn(MODEL_ID);
        when(userMock.getName()).thenReturn(USER_NAME);

        crowdUserAdapter = new CrowdUserAdapter(sessionMock, realmMock, modelMock, userMock);
    }

    @Test
    void when_getId_then_expectedValueIsReturned() {
        assertThat(crowdUserAdapter.getId()).isEqualTo(StorageId.keycloakId(modelMock, userMock.getName()));
    }

    @Test
    void when_getUsername_then_expectedValueIsReturned() {
        assertThat(crowdUserAdapter.getUsername()).isEqualTo(USER_NAME);
    }

    @Test
    void when_setUsername_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdUserAdapter.setUsername("new name")).isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_getEmail_then_expectedValueIsReturned() {
        when(userMock.getEmailAddress()).thenReturn(USER_EMAIL);
        assertThat(crowdUserAdapter.getEmail()).isEqualTo(USER_EMAIL);
    }

    @Test
    void when_setEmail_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdUserAdapter.setEmail("new email")).isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_isEmailVerified_then_trueIsReturned() {
        assertThat(crowdUserAdapter.isEmailVerified()).isTrue();
    }

    @Test
    void when_setEmailVerified_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdUserAdapter.setEmailVerified(true)).isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_getFirstName_then_expectedValueIsReturned() {
        when(userMock.getFirstName()).thenReturn(USER_FIRST_NAME);
        assertThat(crowdUserAdapter.getFirstName()).isEqualTo(USER_FIRST_NAME);
    }

    @Test
    void when_setFirstName_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdUserAdapter.setFirstName("new name"))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_getLastName_then_expectedValueIsReturned() {
        when(userMock.getLastName()).thenReturn(USER_LAST_NAME);
        assertThat(crowdUserAdapter.getLastName()).isEqualTo(USER_LAST_NAME);
    }

    @Test
    void when_setLastName_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdUserAdapter.setLastName("new name"))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_setGroupsInternal_then_noExceptionIsThrown() {
        Set<GroupModel> groups = new HashSet<>();
        groups.add(mock(GroupModel.class));

        crowdUserAdapter.setGroupsInternal(groups);
    }

    @Test
    void when_getGroupsInternal_then_expectedValueIsReturned() {
        Set<GroupModel> groups = new HashSet<>();
        groups.add(mock(GroupModel.class));

        crowdUserAdapter.setGroupsInternal(groups);

        assertThat(crowdUserAdapter.getGroupsInternal()).containsExactlyElementsOf(groups);
    }

    @Test
    void when_removeAttribute_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdUserAdapter.removeAttribute("attr"))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void given_displayName_when_getAttribute_then_expectedValueIsReturned() {
        String displayName = "display name";
        when(userMock.getDisplayName()).thenReturn(displayName);

        assertThat(crowdUserAdapter.getAttribute(CrowdUserAdapter.ATTR_DISPLAY_NAME)).contains(displayName);
    }

    @Test
    void given_unknownCrowdUserAttribute_when_getAttribute_then_expectedValueIsReturned() {
        assertThat(crowdUserAdapter.getAttribute("attr")).isEmpty();
    }

    @Test
    void given_nullCrowdUserAttribute_when_getAttribute_then_expectedValueIsReturned() {
        Set<String> keys = new HashSet<>();
        keys.add("attr");

        when(userMock.getKeys()).thenReturn(keys);
        when(userMock.getValues("attr")).thenReturn(null);

        assertThat(crowdUserAdapter.getAttribute("attr")).isEmpty();
    }

    @Test
    void given_nonNullCrowdUserAttribute_when_getAttribute_then_expectedValueIsReturned() {
        Set<String> keys = new HashSet<>();
        keys.add("attr");

        Set<String> values = new HashSet<>();
        values.add("value");

        when(userMock.getKeys()).thenReturn(keys);
        when(userMock.getValues("attr")).thenReturn(values);

        assertThat(crowdUserAdapter.getAttribute("attr")).containsAll(values);
    }

    @Test
    void when_setAttribute_then_readOnlyExceptionIsThrown() {
        List<String> values = new ArrayList<>();
        values.add("value");

        assertThatThrownBy(() -> crowdUserAdapter.setAttribute("attr", values))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void given_displayName_when_getFirstAttribute_then_expectedValueIsReturned() {
        String displayName = "display name";
        when(userMock.getDisplayName()).thenReturn(displayName);

        assertThat(crowdUserAdapter.getFirstAttribute(CrowdUserAdapter.ATTR_DISPLAY_NAME)).isEqualTo(displayName);
    }

    @Test
    void given_unknownCrowdUserAttribute_when_getFirstAttribute_then_expectedValueIsReturned() {
        assertThat(crowdUserAdapter.getFirstAttribute("attr")).isNull();
    }

    @Test
    void given_knownCrowdUserAttribute_when_getFirstAttribute_then_expectedValueIsReturned() {
        when(userMock.getValue("attr")).thenReturn("value");

        assertThat(crowdUserAdapter.getFirstAttribute("attr")).isEqualTo("value");
    }

    @Test
    void when_setSingleAttribute_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdUserAdapter.setSingleAttribute("attr", "value"))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_getAttributes_then_expectedValuesAreReturned() {
        String displayName = "display name";
        when(userMock.getDisplayName()).thenReturn(displayName);

        Set<String> keys = new HashSet<>();
        keys.add("attr");
        keys.add("null attr");

        Set<String> values = new HashSet<>();
        values.add("value");

        when(userMock.getKeys()).thenReturn(keys);
        when(userMock.getValues("attr")).thenReturn(values);
        when(userMock.getValues("null attr")).thenReturn(null);

        List<String> displayNameValue = new ArrayList<>();
        displayNameValue.add(displayName);

        assertThat(crowdUserAdapter.getAttributes()).containsOnly(
                entry(CrowdUserAdapter.ATTR_DISPLAY_NAME, displayNameValue),
                entry("attr", new ArrayList<>(values)));
    }

}
