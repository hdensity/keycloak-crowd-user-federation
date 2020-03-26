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

package it.schm.keycloak.storage.crowd.group;

import com.atlassian.crowd.model.group.GroupWithAttributes;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrowdGroupAdapterTest {

    @Mock private ComponentModel modelMock;
    @Mock private GroupWithAttributes groupMock;

    private CrowdGroupAdapter crowdGroupAdapter;

    private static final String MODEL_ID = "model id";
    private static final String GROUP_NAME = "group name";

    @BeforeEach
    void setupGroupAdapter() {
        when(modelMock.getId()).thenReturn(MODEL_ID);
        when(groupMock.getName()).thenReturn(GROUP_NAME);

        crowdGroupAdapter = new CrowdGroupAdapter(modelMock, groupMock);
    }

    @Test
    void when_getId_then_expectedValueIsReturned() {
        assertThat(crowdGroupAdapter.getId())
                .isEqualTo(StorageId.keycloakId(modelMock, groupMock.getName()));
    }

    @Test
    void when_getName_then_expectedValueIsReturned() {
        assertThat(crowdGroupAdapter.getName()).isEqualTo(GROUP_NAME);
    }

    @Test
    void when_setName_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdGroupAdapter.setName("new name")).isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_setSingleAttribute_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdGroupAdapter.setSingleAttribute("attr", "value"))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_setAttribute_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdGroupAdapter.setAttribute("attr", new ArrayList<>()))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_removeAttribute_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdGroupAdapter.removeAttribute("attr"))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void given_unknownAttribute_when_getFirstAttribute_then_expectedValueIsReturned() {
        assertThat(crowdGroupAdapter.getFirstAttribute("attr")).isNull();
    }

    @Test
    void given_knownAttribute_when_getFirstAttribute_then_expectedValueIsReturned() {
        when(groupMock.getValue("attr")).thenReturn("value");

        assertThat(crowdGroupAdapter.getFirstAttribute("attr")).isEqualTo("value");
    }

    @Test
    void given_unknownAttribute_when_getAttribute_then_expectedValueIsReturned() {
        when(groupMock.getValues("attr")).thenReturn(null);

        assertThat(crowdGroupAdapter.getAttribute("attr")).isEmpty();
    }

    @Test
    void given_knownAttribute_when_getAttribute_then_expectedValueIsReturned() {
        Set<String> values = new HashSet<>();
        values.add("value");

        when(groupMock.getValues("attr")).thenReturn(values);

        assertThat(crowdGroupAdapter.getAttribute("attr")).containsOnly("value");
    }

    @Test
    void when_getAttributes_then_expectedValueIsReturned() {
        Set<String> keys = new HashSet<>();
        keys.add("attr");

        Set<String> values = new HashSet<>();
        values.add("value");

        when(groupMock.getKeys()).thenReturn(keys);
        when(groupMock.getValues("attr")).thenReturn(values);

        assertThat(crowdGroupAdapter.getAttributes()).containsOnly(entry("attr", new ArrayList<>(values)));
    }

    @Test
    void when_getParent_then_expectedValueIsReturned() {
        GroupModel parentGroupMock = mock(GroupModel.class);
        crowdGroupAdapter.setParent(parentGroupMock);

        assertThat(crowdGroupAdapter.getParent()).isEqualTo(parentGroupMock);
    }

    @Test
    void given_noParent_when_getParentId_then_nullIsReturned() {
        assertThat(crowdGroupAdapter.getParentId()).isNull();
    }

    @Test
    void given_parent_when_getParentId_then_expectedValueIsReturned() {
        GroupModel parentGroupMock = mock(GroupModel.class);
        when(parentGroupMock.getId()).thenReturn("parent_id");

        crowdGroupAdapter.setParent(parentGroupMock);

        assertThat(crowdGroupAdapter.getParentId()).isEqualTo("parent_id");
    }

    @Test
    void when_getSubGroups_then_nullIsReturned() {
        assertThat(crowdGroupAdapter.getSubGroups()).isEmpty();

        GroupModel groupMock = mock(GroupModel.class);
        crowdGroupAdapter.addChild(groupMock);
        assertThat(crowdGroupAdapter.getSubGroups()).containsOnly(groupMock);
    }

    @Test
    void when_setParent_then_noExceptionsIsThrown() {
        crowdGroupAdapter.setParent(mock(GroupModel.class));
    }

    @Test
    void when_addChild_then_readOnlyExceptionIsThrown() {
        GroupModel groupMock = mock(GroupModel.class);
        crowdGroupAdapter.addChild(groupMock);

        assertThat(crowdGroupAdapter.getSubGroups()).containsOnly(groupMock);
    }

    @Test
    void when_removeChild_then_readOnlyExceptionIsThrown() {
        GroupModel groupMock = mock(GroupModel.class);
        crowdGroupAdapter.addChild(groupMock);

        crowdGroupAdapter.removeChild(mock(GroupModel.class));
        assertThat(crowdGroupAdapter.getSubGroups()).containsOnly(groupMock);

        crowdGroupAdapter.removeChild(groupMock);
        assertThat(crowdGroupAdapter.getSubGroups()).isEmpty();
    }

    @Test
    void when_getRealmRoleMappings_then_emptySetIsReturned() {
        assertThat(crowdGroupAdapter.getRealmRoleMappings()).isEmpty();
    }

    @Test
    void when_getClientRoleMappings_then_emptySetIsReturned() {
        assertThat(crowdGroupAdapter.getClientRoleMappings(mock(ClientModel.class))).isEmpty();
    }

    @Test
    void when_hasRole_then_falseIsReturned() {
        assertThat(crowdGroupAdapter.hasRole(mock(RoleModel.class))).isFalse();
    }

    @Test
    void when_grantRole_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdGroupAdapter.grantRole(mock(RoleModel.class)))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void when_getRoleMappings_then_emptySetIsReturned() {
        assertThat(crowdGroupAdapter.getRoleMappings()).isEmpty();
    }

    @Test
    void when_deleteRoleMapping_then_readOnlyExceptionIsThrown() {
        assertThatThrownBy(() -> crowdGroupAdapter.deleteRoleMapping(mock(RoleModel.class)))
                .isExactlyInstanceOf(ReadOnlyException.class);
    }

    @Test
    void equalsAndHashcode() {
        EqualsVerifier.forClass(CrowdGroupAdapter.class)
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withIgnoredFields("group")
                .verify();
    }

}
