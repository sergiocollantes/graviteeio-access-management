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
package io.gravitee.am.gateway.handler.oidc.response;

import io.gravitee.am.model.Client;
import io.gravitee.am.model.jose.ECKey;
import io.gravitee.am.model.jose.RSAKey;
import io.gravitee.am.model.oidc.JWKSet;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexandre FARIA (lusoalex at github.com)
 * @author GraviteeSource Team
 */
public class DynamicClientRegistrationResponseTest {

    @Test
    public void convert() {
        RSAKey rsaKey = new RSAKey();
        rsaKey.setKty("RSA");
        rsaKey.setKid("kidRSA");
        rsaKey.setUse("enc");
        rsaKey.setE("exponent");
        rsaKey.setN("modulus");

        ECKey ecKey = new ECKey();
        ecKey.setKty("EC");
        ecKey.setKid("kidEC");
        ecKey.setUse("enc");
        ecKey.setCrv("P-256");
        ecKey.setX("vBT2JhFHd62Jcf4yyBzSV9NuDBNBssR1zlmnHelgZcs");
        ecKey.setY("up8E8b3TjeKS2v2GCH23UJP0bak0La77lkQ7_n4djqE");

        JWKSet jwkSet = new JWKSet();
        jwkSet.setKeys(Arrays.asList(rsaKey,ecKey));

        Client client = new Client();
        client.setClientId("clientId");
        client.setClientName("clientName");
        client.setJwks(jwkSet);

        DynamicClientRegistrationResponse response = DynamicClientRegistrationResponse.fromClient(client);

        assertNotNull("expecting response",response);
        assertEquals(response.getClientId(),"clientId");
        assertEquals(response.getClientName(),"clientName");
        assertTrue(response.getJwks().getKeys().size()==2);
    }
}
