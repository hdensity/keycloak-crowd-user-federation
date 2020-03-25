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

import com.atlassian.crowd.model.user.UserWithAttributes;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A read-only UserModel implementation for Crowd's UserWithAttributes.
 *
 * @author Sam Schmit
 * @since 1.0.0
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/storage/adapter/AbstractUserAdapterFederatedStorage.html">org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage</a>
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/models/UserModel.html">org.keycloak.models.UserModel</a>
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/models/RoleMapperModel.html">org.keycloak.models.RoleMapperModel</a>
 * @see <a href="https://docs.atlassian.com/atlassian-crowd/4.0.0/com/atlassian/crowd/model/user/UserWithAttributes.html">com.atlassian.crowd.model.user.UserWithAttributes</a>
 */
public class CrowdUserAdapter extends AbstractUserAdapterFederatedStorage {

    public static final String ATTR_DISPLAY_NAME = "displayName";

    private final String keycloakId;
    private final UserWithAttributes entity;

    private Set<GroupModel> groups;

    /**
     * Creates a new instance of this adapter.
     *
     * @param session the keycloak session
     * @param realm the realm model
     * @param model the provider's component model
     * @param entity the crowd user entity
     */
    public CrowdUserAdapter(
            KeycloakSession session, RealmModel realm, ComponentModel model, UserWithAttributes entity) {
        super(session, realm, model);

        this.keycloakId = StorageId.keycloakId(model, entity.getName());
        this.entity = entity;
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public String getUsername() {
        return entity.getName();
    }

    @Override
    public void setUsername(String username) {
        throw new ReadOnlyException();
    }

    @Override
    public String getEmail() {
        return entity.getEmailAddress();
    }

    @Override
    public void setEmail(String email) {
        throw new ReadOnlyException();
    }

    @Override
    public boolean isEmailVerified() {
        return true;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        throw new ReadOnlyException();
    }

    @Override
    public String getFirstName() {
        return entity.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        throw new ReadOnlyException();
    }

    @Override
    public String getLastName() {
        return entity.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        throw new ReadOnlyException();
    }

    public void setGroupsInternal(Set<GroupModel> groups) {
        this.groups = groups;
    }

    @Override
    protected Set<GroupModel> getGroupsInternal() {
        return groups;
    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException();
    }

    @Override
    public List<String> getAttribute(String name) {
        if (ATTR_DISPLAY_NAME.equals(name)) {
            List<String> strings = new ArrayList<>();
            strings.add(entity.getDisplayName());

            return strings;
        }

        if (entity.getKeys().contains(name)) {
            Set<String> values = entity.getValues(name);
            return values != null ? new ArrayList<>(values) : Collections.emptyList();
        }

        return Collections.emptyList();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw new ReadOnlyException();
    }

    @Override
    public String getFirstAttribute(String name) {
        if (ATTR_DISPLAY_NAME.equals(name)) {
            return entity.getDisplayName();
        }

        return entity.getValue(name);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        throw new ReadOnlyException();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putSingle(ATTR_DISPLAY_NAME, entity.getDisplayName());

        entity.getKeys().stream()
                .filter(key -> entity.getValues(key) != null)
                .forEach(key -> all.put(key, new ArrayList<>(entity.getValues(key))));

        return all;
    }

}
