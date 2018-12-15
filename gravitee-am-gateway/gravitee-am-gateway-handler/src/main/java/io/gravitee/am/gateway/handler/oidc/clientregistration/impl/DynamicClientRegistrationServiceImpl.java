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
package io.gravitee.am.gateway.handler.oidc.clientregistration.impl;

import io.gravitee.am.common.oidc.Scope;
import io.gravitee.am.gateway.handler.jwk.JwkService;
import io.gravitee.am.gateway.handler.oidc.clientregistration.DynamicClientRegistrationService;
import io.gravitee.am.gateway.handler.oidc.request.DynamicClientRegistrationRequest;
import io.gravitee.am.model.Client;
import io.gravitee.am.service.CertificateService;
import io.gravitee.am.service.GrantTypeService;
import io.gravitee.am.service.IdentityProviderService;
import io.gravitee.am.service.ResponseTypeService;
import io.gravitee.am.service.exception.InvalidClientMetadataException;
import io.gravitee.am.service.exception.InvalidRedirectUriException;
import io.gravitee.am.service.utils.UriBuilder;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static io.gravitee.am.common.oidc.Scope.SCOPE_DELIMITER;

/**
 * @author Alexandre FARIA (lusoalex at github.com)
 * @author GraviteeSource Team
 */
public class DynamicClientRegistrationServiceImpl implements DynamicClientRegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicClientRegistrationServiceImpl.class);

    @Autowired
    private ResponseTypeService responseTypeService;

    @Autowired
    private GrantTypeService grantTypeService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private JwkService jwkService;

    @Autowired
    public WebClient client;

    /**
     * Identity provider is not part of dynamic client registration but needed on the client.
     * So we set the first identoty provider available on the domain.
     * @param domain Domain
     * @param client App to create
     * @return
     */
    @Override
    public Single<Client> applyDefaultIdentityProvider(String domain, Client client) {
        return identityProviderService.findByDomain(domain)
            .map(identityProviders -> {
                if(identityProviders!=null && !identityProviders.isEmpty()) {
                    client.setIdentities(Collections.singleton(identityProviders.get(0).getId()));
                }
                return client;
            });
    }

    /**
     * Certificate provider is not part of dynamic client registration but needed on the client.
     * So we set the first certificate provider available on the domain.
     * @param domain Domain
     * @param client App to create
     * @return
     */
    @Override
    public Single<Client> applyDefaultCertificateProvider(String domain, Client client) {
        return certificateService.findByDomain(domain)
                .map(certificates -> {
                    if(certificates!=null && !certificates.isEmpty()) {
                        client.setCertificate(certificates.get(0).getId());
                    }
                    return client;
                });
    }

    /**
     * Validate payload according to openid specifications.
     *
     * https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
     *
     * @param request DynamicClientRegistrationRequest
     */
    @Override
    public Single<DynamicClientRegistrationRequest> validateClientRequest(final DynamicClientRegistrationRequest request) {
        LOGGER.debug("Validating dynamic client registration payload");

        //Should not be null...
        if(request==null) {
            return Single.error(new InvalidClientMetadataException());
        }

        return this.validateRedirectUri(request)
                .flatMap(this::validateResponseType)
                .flatMap(this::validateGrantType)
                .flatMap(this::validateRequestUri)
                .flatMap(this::validateSectorIdentifierUri)
                .flatMap(this::validateJKWs)
                .flatMap(this::validateScopes);
    }

    private Single<DynamicClientRegistrationRequest> validateRedirectUri(DynamicClientRegistrationRequest request) {
        //Redirect_uri is required, must be informed and filled without null values.
        if(request.getRedirectUris()==null || !request.getRedirectUris().isPresent() || request.getRedirectUris().get().isEmpty()) {
            return Single.error(new InvalidRedirectUriException());
        }
        return Single.just(request);
    }

    private Single<DynamicClientRegistrationRequest> validateResponseType(DynamicClientRegistrationRequest request) {
        //if response_type provided, they must be valid.
        if(request.getResponseTypes()!=null) {
            if(!responseTypeService.isValideResponseType(request.getResponseTypes().get())) {
                return Single.error(new InvalidClientMetadataException("Invalid response type."));
            }
        }
        return Single.just(request);
    }

    private Single<DynamicClientRegistrationRequest> validateGrantType(DynamicClientRegistrationRequest request) {
        //if grant_type provided, they must be valid.
        if(request.getGrantTypes()!=null) {
            if(!grantTypeService.isValideGrantType(request.getGrantTypes().get())) {
                return Single.error(new InvalidClientMetadataException("Missing or invalid grant type."));
            }
        }
        return Single.just(request);
    }

    private Single<DynamicClientRegistrationRequest> validateRequestUri(DynamicClientRegistrationRequest request) {
        //Check request_uri well formated
        if(request.getRequestUris()!=null && request.getRequestUris().isPresent()) {
            try {
                //throw exception if uri mal formated
                request.getRequestUris().get().stream().forEach(this::formatUrl);
            } catch (InvalidClientMetadataException err) {
                return Single.error(new InvalidClientMetadataException("request_uris: "+err.getMessage()));
            }
        }
        return Single.just(request);
    }

    private Single<DynamicClientRegistrationRequest> validateSectorIdentifierUri(DynamicClientRegistrationRequest request) {
        //if sector_identifier_uri is provided, then retrieve content and validate redirect_uris among this list.
        if(request.getSectorIdentifierUri()!=null && request.getSectorIdentifierUri().isPresent()) {

            URI uri;
            try {
                //throw exception if uri mal formated
                uri = formatUrl(request.getSectorIdentifierUri().get());
            } catch (InvalidClientMetadataException err) {
                return Single.error(new InvalidClientMetadataException("sector_identifier_uri: "+err.getMessage()));
            }

            if(!uri.getScheme().equalsIgnoreCase("https")) {
                return Single.error(new InvalidClientMetadataException("Scheme must be https for sector_identifier_uri : "+request.getSectorIdentifierUri().get()));
            }

            return client.getAbs(uri.toString())
                    .rxSend()
                    .map(HttpResponse::bodyAsString)
                    .map(JsonArray::new)
                    .onErrorResumeNext(Single.error(new InvalidClientMetadataException("Unable to parse sector_identifier_uri : "+ uri.toString())))
                    .flatMapPublisher(Flowable::fromIterable)
                    .cast(String.class)
                    .collect(HashSet::new,(set, value)->set.add(value))
                    .flatMap(allowedRedirectUris -> Observable.fromIterable(request.getRedirectUris().get())
                            .filter(redirectUri -> !allowedRedirectUris.contains(redirectUri))
                            .collect(ArrayList<String>::new, (list, missingRedirectUri)-> list.add(missingRedirectUri))
                            .flatMap(missing -> {
                                if(!missing.isEmpty()) {
                                    return Single.error(
                                            new InvalidRedirectUriException("redirect uris are not allowed according to sector_identifier_uri: "+
                                                    String.join(" ",missing)
                                            )
                                    );
                                } else {
                                    return Single.just(request);
                                }
                            })
                    );
        }
        return Single.just(request);
    }

    private Single<DynamicClientRegistrationRequest> validateJKWs(DynamicClientRegistrationRequest request) {
        //The jwks_uri and jwks parameters MUST NOT be used together.
        if(request.getJwks()!=null && request.getJwks().isPresent() && request.getJwksUri()!=null && request.getJwksUri().isPresent()) {
            return Single.error(new InvalidClientMetadataException("The jwks_uri and jwks parameters MUST NOT be used together."));
        }

        //Check jwks_uri
        if(request.getJwksUri()!=null && request.getJwksUri().isPresent()) {
            return jwkService.getKeys(request.getJwksUri().get())
                    .switchIfEmpty(Maybe.error(new InvalidClientMetadataException("No JWK found behing jws uri...")))
                    .flatMapSingle(jwkSet -> {
                        /* Uncomment if we expect to save it as fallback
                        if(jwkSet!=null && jwkSet.isPresent()) {
                            request.setJwks(jwkSet);
                        }
                        */
                        return Single.just(request);
                    });
        }

        return Single.just(request);
    }

    /**
     * Here the target is not to validate all scopes but ensure that openid is well included.
     * The scopes validations are done later (validateMetadata) on the process.
     * @param request DynamicClientRegistrationRequest
     * @return DynamicClientRegistrationRequest
     */
    private Single<DynamicClientRegistrationRequest> validateScopes(DynamicClientRegistrationRequest request) {

        if(request.getScope()!=null && request.getScope().isPresent()) {
            if(request.getScope().get().stream().filter(Scope.OPENID.getName()::equals).count()==0) {
                List scopes = new LinkedList(request.getScope().get());
                scopes.add(Scope.OPENID.getName());
                request.setScope(Optional.of(String.join(SCOPE_DELIMITER,scopes)));
            }
        } else {
            request.setScope(Optional.of(Scope.OPENID.getName()));
        }

        return Single.just(request);
    }

    /**
     * Check Uri is well formatted
     * @param uri String
     * @return URI if well formatted, else throw an InvalidClientMetadataException
     */
    private URI formatUrl(String uri) {
        try {
            return UriBuilder.fromHttpUrl(uri).build();
        }
        catch(IllegalArgumentException | URISyntaxException ex) {
            throw new InvalidClientMetadataException(uri+" is not valid.");
        }
    }
}
