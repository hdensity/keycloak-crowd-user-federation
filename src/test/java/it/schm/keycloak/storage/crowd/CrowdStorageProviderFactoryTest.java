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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrowdStorageProviderFactoryTest {

    private CrowdStorageProviderFactory providerFactory = new CrowdStorageProviderFactory();

    @Test
    void when_getId_then_expectedValueIsReturned() {
        assertThat(providerFactory.getId()).isEqualTo("crowd");
    }

    @Test
    void when_getConfigProperties_then_expectedValuesAreReturned() {
        assertThat(providerFactory.getConfigProperties()).isEqualTo(CrowdStorageProviderFactory.configMetadata);
    }

    @Test
    void given_nullUrl_when_validateConfiguration_then_exceptionIsThrown() {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

        ComponentModel modelMock = mock(ComponentModel.class);
        when(modelMock.getConfig()).thenReturn(config);

        assertThatThrownBy(() -> providerFactory.validateConfiguration(null, null, modelMock))
                .isExactlyInstanceOf(ComponentValidationException.class);
    }

    @Test
    void given_nullApplicationName_when_validateConfiguration_then_exceptionIsThrown() {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(CrowdStorageProviderFactory.CONFIG_URL, "value");

        ComponentModel modelMock = mock(ComponentModel.class);
        when(modelMock.getConfig()).thenReturn(config);

        assertThatThrownBy(() -> providerFactory.validateConfiguration(null, null, modelMock))
                .isExactlyInstanceOf(ComponentValidationException.class);
    }

    @Test
    void given_nullApplicationPassword_when_validateConfiguration_then_exceptionIsThrown() {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(CrowdStorageProviderFactory.CONFIG_URL, "value");
        config.putSingle(CrowdStorageProviderFactory.CONFIG_APPLICATION_NAME, "value");

        ComponentModel modelMock = mock(ComponentModel.class);
        when(modelMock.getConfig()).thenReturn(config);

        assertThatThrownBy(() -> providerFactory.validateConfiguration(null, null, modelMock))
                .isExactlyInstanceOf(ComponentValidationException.class);
    }

    @Test
    void given_properConfig_when_validateConfiguration_then_noExceptionIsThrown() {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(CrowdStorageProviderFactory.CONFIG_URL, "value");
        config.putSingle(CrowdStorageProviderFactory.CONFIG_APPLICATION_NAME, "value");
        config.putSingle(CrowdStorageProviderFactory.CONFIG_APPLICATION_PASSWORD, "value");

        ComponentModel modelMock = mock(ComponentModel.class);
        when(modelMock.getConfig()).thenReturn(config);

        providerFactory.validateConfiguration(null, null, modelMock);
    }

    @Test
    void when_create_then_providerWithExpectedValuesIsCreated() {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(CrowdStorageProviderFactory.CONFIG_URL, "http://localhost");
        config.putSingle(CrowdStorageProviderFactory.CONFIG_APPLICATION_NAME, "application");
        config.putSingle(CrowdStorageProviderFactory.CONFIG_APPLICATION_PASSWORD, "password");

        KeycloakSession sessionMock = mock(KeycloakSession.class);
        ComponentModel modelMock = mock(ComponentModel.class);
        when(modelMock.getConfig()).thenReturn(config);

        assertThat(providerFactory.create(sessionMock, modelMock)).isExactlyInstanceOf(CrowdStorageProvider.class);
    }

}
