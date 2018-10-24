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
package io.gravitee.am.model.jose;

/**
 *
 * The "kty" (key type) parameter identifies the cryptographic algorithm
 * family used with the key, such as "RSA" or "EC".  "kty" values should
 * either be registered in the IANA "JSON Web Key Types"
 *
 * Source https://tools.ietf.org/html/rfc7517#section-4.1
 *
 * Registred values : https://www.iana.org/assignments/jose/jose.xhtml#web-key-types
 *
 * @author Alexandre FARIA (lusoalex at github.com)
 * @author GraviteeSource Team
 */
public enum KeyType {

    EC("EC","Elliptic Curve"),
    RSA("RSA","RSA"),
    OCT("oct","Octet sequence"),
    OKP("OKP","Octet string key pairs");

    private String keyType;
    private String name;

    KeyType(String keyType, String name) {
        this.keyType = keyType;
        this.name = name;
    }

    public String getKeyType() { return keyType; }
    public String getName() { return name;}


    @Override
    public String toString() {
        return super.toString();
    }
}
