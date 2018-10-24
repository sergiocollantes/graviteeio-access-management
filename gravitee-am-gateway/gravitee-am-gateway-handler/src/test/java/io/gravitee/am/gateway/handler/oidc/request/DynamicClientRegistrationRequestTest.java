/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.gateway.handler.oidc.request;

import io.gravitee.am.model.Client;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Alexandre FARIA (lusoalex at github.com)
 * @author GraviteeSource Team
 */
public class DynamicClientRegistrationRequestTest {

    @Test
    public void testPatch() {
        //Build Object to patch
        Client toPatch = new Client();
        toPatch.setDynamicClientRegistrationEnabled(true);
        toPatch.setClientName("oldName");
        toPatch.setClientSecret("expectedSecret");
        toPatch.setClientUri("shouldDisappear");
        toPatch.setScope(Arrays.asList("scopeA","scopeB"));
        toPatch.setAccessTokenValiditySeconds(7200);
        toPatch.setRefreshTokenValiditySeconds(3600);
        toPatch.setResponseTypes(Arrays.asList("old","old2"));

        //Build patcher
        DynamicClientRegistrationRequest patcher = new DynamicClientRegistrationRequest();
        patcher.setClientName(Optional.of("expectedClientName"));
        patcher.setClientUri(Optional.empty());
        patcher.setGrantTypes(Optional.of(Arrays.asList("grant1","grant2")));
        patcher.setResponseTypes(Optional.empty());
        patcher.setScope(Optional.of("scope1 scope2"));

        //Apply patch
        Client result = patcher.patch(toPatch);

        //Checks
        assertNotNull(result);
        assertTrue("DCR settings should not be replaced",result.isDynamicClientRegistrationEnabled());
        assertEquals("Client name should have been replaced","expectedClientName",result.getClientName());
        assertEquals("Client secret should have been kept","expectedSecret", result.getClientSecret());
        assertNull("Client uri should have been erased",result.getClientUri());
        assertEquals("Access token validity should have been kept",7200,result.getAccessTokenValiditySeconds());
        assertEquals("Refresh token validity should have been kept",3600, result.getRefreshTokenValiditySeconds());
        assertArrayEquals("Grant types should have been replaced",Arrays.asList("grant1","grant2").toArray(),result.getGrantTypes().toArray());
        assertArrayEquals("Response type should have been replaced by default falues",Arrays.asList("code").toArray(),result.getResponseTypes().toArray());
        assertArrayEquals("Scopes should have been replaced",Arrays.asList("scope1","scope2").toArray(), result.getScope().toArray());
    }
}
