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

import com.atlassian.crowd.model.group.GroupWithAttributes;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

/**
 * A read-only GroupModel implementation for Crowd's GroupWithAttributes.
 *
 * @author Sam Schmit
 * @since 1.0.0
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/models/GroupModel.html">org.keycloak.models.GroupModel</a>
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/models/RoleMapperModel.html">org.keycloak.models.RoleMapperModel</a>
 * @see <a href="https://docs.atlassian.com/atlassian-crowd/4.0.0/com/atlassian/crowd/model/group/GroupWithAttributes.html">com.atlassian.crowd.model.group.GroupWithAttributes</a>
 */
public class CrowdGroupAdapter implements GroupModel {

    private final String id;
    private final GroupWithAttributes group;

    private GroupModel parent;
    private Set<GroupModel> subGroups = new HashSet<>();

    public CrowdGroupAdapter(ComponentModel model, GroupWithAttributes group) {
        this.id = StorageId.keycloakId(model, group.getName());
        this.group = group;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return group.getName();
    }

    @Override
    public void setName(String name) {
        throw new ReadOnlyException();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        throw new ReadOnlyException();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw new ReadOnlyException();
    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException();
    }

    @Override
    public String getFirstAttribute(String name) {
        return group.getValue(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        Set<String> values = group.getValues(name);

        return values != null ? new ArrayList<>(values) : Collections.emptyList();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return group.getKeys().stream().collect(toMap(key -> key, this::getAttribute));
    }

    @Override
    public GroupModel getParent() {
        return parent;
    }

    @Override
    public String getParentId() {
        return parent != null ? parent.getId() : null;
    }

    @Override
    public Set<GroupModel> getSubGroups() {
        return subGroups;
    }

    @Override
    public void setParent(GroupModel group) {
        this.parent = group;
    }

    @Override
    public void addChild(GroupModel subGroup) {
        subGroups.add(subGroup);
    }

    @Override
    public void removeChild(GroupModel subGroup) {
        subGroups.remove(subGroup);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        return Collections.emptySet();
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        return Collections.emptySet();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        throw new ReadOnlyException();
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        return Collections.emptySet();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        throw new ReadOnlyException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrowdGroupAdapter that = (CrowdGroupAdapter) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(parent, that.parent) &&
                Objects.equals(subGroups, that.subGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parent, subGroups);
    }
}
