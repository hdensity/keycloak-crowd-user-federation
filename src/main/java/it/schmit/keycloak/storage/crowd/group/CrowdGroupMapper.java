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

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.group.GroupWithAttributes;
import com.atlassian.crowd.service.client.CrowdClient;
import it.schmit.keycloak.storage.crowd.CrowdStorageProvider;
import it.schmit.keycloak.storage.crowd.CrowdUserAdapter;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelException;
import org.keycloak.storage.StorageId;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mapper used to resolve a user's groups, retrieving them, as well as their respective parents and children.
 *
 * @author Sam Schmit
 * @since 1.0.0
 */
public class CrowdGroupMapper {

    private static final Logger logger = Logger.getLogger(CrowdStorageProvider.class);

    private final ComponentModel model;
    private final CrowdClient client;

    /**
     * Create's a new instance of this mapper.
     *
     * @param model this provider's component model
     * @param client the crowd rest client
     */
    public CrowdGroupMapper(ComponentModel model, CrowdClient client) {
        this.model = model;
        this.client = client;
    }

    /**
     * Retrieves the provided user's groups and resolves their respective parents and children.
     *
     * @param user The user for which to resolve groups
     * @return The provided user with it's groups set
     */
    public CrowdUserAdapter onLoadUser(CrowdUserAdapter user) {
        try {
            Set<GroupModel> userGroups = client.getGroupsForUser(user.getUsername(), 0, Integer.MAX_VALUE).stream()
                    .map(group -> new CrowdGroupAdapter(model, (GroupWithAttributes) group))
                    .peek(group -> {
                        loadParent(group);
                        loadSubGroups(group);
                    })
                    .collect(Collectors.toSet());

            user.setGroupsInternal(userGroups);

            return user;
        } catch (OperationFailedException | InvalidAuthenticationException |
                ApplicationPermissionException | UserNotFoundException e) {
            logger.error(e);
            throw new ModelException(e);
        }
    }

    private void loadParent(CrowdGroupAdapter groupAdapter) {
        try {
            client.getParentGroupsForGroup(StorageId.externalId(groupAdapter.getId()), 0, 1)
                    .stream()
                    .map(group -> new CrowdGroupAdapter(model, (GroupWithAttributes) group))
                    .peek(this::loadParent)
                    .forEach(groupAdapter::setParent);
        } catch (OperationFailedException | InvalidAuthenticationException |
                ApplicationPermissionException | GroupNotFoundException e) {
            logger.error(e);
            throw new ModelException(e);
        }
    }

    private void loadSubGroups(CrowdGroupAdapter groupAdapter) {
        try {
            client.getChildGroupsOfGroup(StorageId.externalId(groupAdapter.getId()), 0, Integer.MAX_VALUE).stream()
                    .map(group -> new CrowdGroupAdapter(model, (GroupWithAttributes) group))
                    .peek(this::loadSubGroups)
                    .forEach(groupAdapter::addChild);
        } catch (OperationFailedException | InvalidAuthenticationException |
                ApplicationPermissionException | GroupNotFoundException e) {
            logger.error(e);
            throw new ModelException(e);
        }
    }

}
