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

package it.schmit.keycloak.storage.crowd.group;

import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.group.GroupWithAttributes;
import com.atlassian.crowd.service.client.CrowdClient;
import it.schmit.keycloak.storage.crowd.CrowdUserAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrowdGroupMapperTest {

    @Mock private ComponentModel modelMock;
    @Mock private CrowdClient clientMock;

    @InjectMocks
    private CrowdGroupMapper crowdGroupMapper;

    private static final String USERNAME = "username";
    @Mock private CrowdUserAdapter crowdUserAdapterMock;
    @Captor private ArgumentCaptor<Set<GroupModel>> groupModelArgumentCaptor;

    @BeforeEach
    void setup() {
        when(crowdUserAdapterMock.getUsername()).thenReturn(USERNAME);
    }

    @Test
    void when_onLoadUser_then_expectedGroupsAreRetrieved() throws Exception {
        // user groups
        GroupWithAttributes singleGroupMock = createGroupMockWithName("singleGroupMock");
        GroupWithAttributes groupWithParentsMock = createGroupMockWithName("groupWithParentsMock");
        GroupWithAttributes groupWithChildrenMock = createGroupMockWithName("groupWithChildrenMock");

        List<Group> userGroups = new ArrayList<>();
        userGroups.add(singleGroupMock);
        userGroups.add(groupWithParentsMock);
        userGroups.add(groupWithChildrenMock);

        when(clientMock.getGroupsForUser(USERNAME, 0, Integer.MAX_VALUE)).thenReturn(userGroups);
        when(clientMock.getParentGroupsForGroup("singleGroupMock", 0, 1))
                .thenReturn(Collections.emptyList());
        when(clientMock.getChildGroupsOfGroup("singleGroupMock", 0, Integer.MAX_VALUE))
                .thenReturn(Collections.emptyList());
        when(clientMock.getChildGroupsOfGroup("groupWithParentsMock", 0, Integer.MAX_VALUE))
                .thenReturn(Collections.emptyList());

        // parent groups
        GroupWithAttributes parentGroupMock = createGroupMockWithName("parentGroupMock");
        GroupWithAttributes grandParentGroupMock = createGroupMockWithName("grandParentGroupMock");

        List<Group> parentGroups = new ArrayList<>();
        parentGroups.add(parentGroupMock);
        List<Group> grandParentGroups = new ArrayList<>();
        grandParentGroups.add(grandParentGroupMock);

        when(clientMock.getParentGroupsForGroup("groupWithParentsMock", 0, 1)).thenReturn(parentGroups);
        when(clientMock.getParentGroupsForGroup("parentGroupMock", 0, 1)).thenReturn(grandParentGroups);

        // child groups
        GroupWithAttributes firstChildGroupMock = createGroupMockWithName("firstChildGroupMock");
        GroupWithAttributes secondChildGroupMock = createGroupMockWithName("secondChildGroupMock");
        GroupWithAttributes grandChildGroupMock = createGroupMockWithName("grandChildGroupMock");

        List<Group> childGroups = new ArrayList<>();
        childGroups.add(firstChildGroupMock);
        childGroups.add(secondChildGroupMock);
        List<Group> grandChildGroups = new ArrayList<>();
        grandChildGroups.add(grandChildGroupMock);

        when(clientMock.getChildGroupsOfGroup("groupWithChildrenMock", 0, Integer.MAX_VALUE)).thenReturn(childGroups);
        when(clientMock.getChildGroupsOfGroup("firstChildGroupMock", 0, Integer.MAX_VALUE)).thenReturn(grandChildGroups);

        // THEN
        crowdGroupMapper.onLoadUser(crowdUserAdapterMock);

        // WHEN
        verify(crowdUserAdapterMock).setGroupsInternal(groupModelArgumentCaptor.capture());

        // verify
        Set<CrowdGroupAdapter> expectedGroups = new HashSet<>();
        expectedGroups.add(new CrowdGroupAdapter(modelMock, singleGroupMock));

        CrowdGroupAdapter groupWithParentsAdapter = new CrowdGroupAdapter(modelMock, groupWithParentsMock);
        CrowdGroupAdapter parentAdapter = new CrowdGroupAdapter(modelMock, parentGroupMock);
        CrowdGroupAdapter grandparentAdapter = new CrowdGroupAdapter(modelMock, grandParentGroupMock);
        groupWithParentsAdapter.setParent(parentAdapter);
        parentAdapter.setParent(grandparentAdapter);

        expectedGroups.add(groupWithParentsAdapter);

        CrowdGroupAdapter groupWithChildrenAdapter = new CrowdGroupAdapter(modelMock, groupWithChildrenMock);
        CrowdGroupAdapter firstChildAdapter = new CrowdGroupAdapter(modelMock, firstChildGroupMock);
        CrowdGroupAdapter secondChildAdapter = new CrowdGroupAdapter(modelMock, secondChildGroupMock);
        CrowdGroupAdapter grandchildAdapter = new CrowdGroupAdapter(modelMock, grandChildGroupMock);
        groupWithChildrenAdapter.addChild(firstChildAdapter);
        groupWithChildrenAdapter.addChild(secondChildAdapter);
        firstChildAdapter.addChild(grandchildAdapter);

        expectedGroups.add(groupWithChildrenAdapter);

        assertThat(groupModelArgumentCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expectedGroups);
    }

    private GroupWithAttributes createGroupMockWithName(String name) {
        GroupWithAttributes groupMock = mock(GroupWithAttributes.class);
        when(groupMock.getName()).thenReturn(name);

        return groupMock;
    }

    @Test
    void given_getGroupsForUserThrowsOperationFailedException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetGroupsForUserExceptionTest(new OperationFailedException());
    }

    @Test
    void given_getGroupsForUserThrowsInvalidAuthenticationException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetGroupsForUserExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_getGroupsForUserThrowsApplicationPermissionException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetGroupsForUserExceptionTest(new ApplicationPermissionException());
    }

    @Test
    void given_getGroupsForUserThrowsUserNotFoundException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetGroupsForUserExceptionTest(new UserNotFoundException("Boom!"));
    }

    private void runGetGroupsForUserExceptionTest(Exception exception) throws Exception {
        when(clientMock.getGroupsForUser(USERNAME, 0, Integer.MAX_VALUE)).thenThrow(exception);

        assertThatThrownBy(() -> crowdGroupMapper.onLoadUser(crowdUserAdapterMock))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

    @Test
    void given_getParentGroupsForGroupThrowsOperationFailedException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetParentGroupsForGroupExceptionTest(new OperationFailedException());
    }

    @Test
    void given_getParentGroupsForGroupThrowsInvalidAuthenticationException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetParentGroupsForGroupExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_getParentGroupsForGroupThrowsApplicationPermissionException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetParentGroupsForGroupExceptionTest(new ApplicationPermissionException());
    }

    @Test
    void given_getParentGroupsForGroupThrowsGroupNotFoundException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetParentGroupsForGroupExceptionTest(new GroupNotFoundException("Boom!"));
    }

    private void runGetParentGroupsForGroupExceptionTest(Exception exception) throws Exception {
        GroupWithAttributes groupMock = createGroupMockWithName("groupMock");

        List<Group> userGroups = new ArrayList<>();
        userGroups.add(groupMock);
        when(clientMock.getGroupsForUser(USERNAME, 0, Integer.MAX_VALUE)).thenReturn(userGroups);

        when(clientMock.getParentGroupsForGroup("groupMock", 0, 1)).thenThrow(exception);

        assertThatThrownBy(() -> crowdGroupMapper.onLoadUser(crowdUserAdapterMock))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

    @Test
    void given_getChildGroupsOfGroupThrowsOperationFailedException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetChildGroupsOfGroupExceptionTest(new OperationFailedException());
    }

    @Test
    void given_getChildGroupsOfGroupThrowsInvalidAuthenticationException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetChildGroupsOfGroupExceptionTest(new InvalidAuthenticationException("Boom!"));
    }

    @Test
    void given_getChildGroupsOfGroupThrowsApplicationPermissionException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetChildGroupsOfGroupExceptionTest(new ApplicationPermissionException());
    }

    @Test
    void given_getChildGroupsOfGroupThrowsGroupNotFoundException_when_onLoadUser_thenExceptionIsThrown() throws Exception {
        runGetChildGroupsOfGroupExceptionTest(new GroupNotFoundException("Boom!"));
    }

    private void runGetChildGroupsOfGroupExceptionTest(Exception exception) throws Exception {
        GroupWithAttributes groupMock = createGroupMockWithName("groupMock");

        List<Group> userGroups = new ArrayList<>();
        userGroups.add(groupMock);
        when(clientMock.getGroupsForUser(USERNAME, 0, Integer.MAX_VALUE)).thenReturn(userGroups);

        when(clientMock.getParentGroupsForGroup("groupMock", 0, 1)).thenReturn(Collections.emptyList());
        when(clientMock.getChildGroupsOfGroup("groupMock", 0, Integer.MAX_VALUE)).thenThrow(exception);

        assertThatThrownBy(() -> crowdGroupMapper.onLoadUser(crowdUserAdapterMock))
                .isExactlyInstanceOf(ModelException.class)
                .hasCause(exception);
    }

}
