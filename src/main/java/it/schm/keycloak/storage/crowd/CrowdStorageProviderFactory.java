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

import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.service.client.CrowdClient;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * Factory implementation to manage the configuration and creation of the {@link CrowdStorageProvider}.
 *
 * @author Sam Schmit
 * @since 1.0.0
 * @see <a href="https://www.keycloak.org/docs-api/9.0/javadocs/org/keycloak/storage/UserStorageProviderFactory.html">org.keycloak.storage.UserStorageProviderFactory</a>
 * @see CrowdStorageProvider
 */
public class CrowdStorageProviderFactory implements UserStorageProviderFactory<CrowdStorageProvider> {

    private static final String PROVIDER_NAME = "crowd";

    protected static final String CONFIG_URL = "url";
    protected static final String CONFIG_APPLICATION_NAME = "applicationName";
    protected static final String CONFIG_APPLICATION_PASSWORD = "applicationPassword";

    protected static final List<ProviderConfigProperty> configMetadata;

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                    .name(CONFIG_URL)
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .label("Crowd URL")
                    .helpText("Base url for Crowd server")
                    .add()
                .property()
                    .name(CONFIG_APPLICATION_NAME)
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .label("Crowd Application Name")
                    .helpText("Application name registered in Crowd server")
                    .add()
                .property()
                    .name(CONFIG_APPLICATION_PASSWORD)
                    .type(ProviderConfigProperty.PASSWORD)
                    .label("Crowd Application Password")
                    .helpText("Application password registered in Crowd server")
                    .add()
                .build();
    }

    /**
     * Returns the name of this provider ("crowd").
     *
     * @return the id of the provider ("crowd")
     */
    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    /**
     * Returns the configuration properties of this provider.
     *
     * @return the configuration properties of this provider
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    /**
     * Validates a given configuration for required parameters.
     *
     * @param session the keycloak session
     * @param realm the keycloak realm
     * @param config the config to validate
     * @throws ComponentValidationException thrown if the given config is invalid
     */
    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        String url = config.getConfig().getFirst(CONFIG_URL);
        if (url == null) {
            throw new ComponentValidationException("Please provide base URL to crowd server");
        }

        String applicationName = config.getConfig().getFirst(CONFIG_APPLICATION_NAME);
        if (applicationName == null) {
            throw new ComponentValidationException("Please provide Application name registered in crowd");
        }

        String applicationPassword = config.getConfig().getFirst(CONFIG_APPLICATION_PASSWORD);
        if (applicationPassword == null) {
            throw new ComponentValidationException("Please provide Application password registered in crowd");
        }
    }

    /**
     * Creates a new instance of the {@link CrowdStorageProvider}.
     *
     * @param session the keycloak session
     * @param model the keycloak component model
     * @return a new isntance of the provider
     */
    @Override
    public CrowdStorageProvider create(KeycloakSession session, ComponentModel model) {
        CrowdClient client = new RestCrowdClientFactory().newInstance(
                model.getConfig().getFirst(CONFIG_URL),
                model.getConfig().getFirst(CONFIG_APPLICATION_NAME),
                model.getConfig().getFirst(CONFIG_APPLICATION_PASSWORD));

        return new CrowdStorageProvider(session, model, client);
    }

}
