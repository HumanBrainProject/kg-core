/*
 * Copyright 2021 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.exception.*;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

/**
 * This controller advice handles the population of the {@link AuthContext}, provides shared parameters (such as {@link PaginationParam}) and translates exceptions into http status codes.
 */
@ControllerAdvice(annotations = RestController.class)
public class RestControllerAdvice {

    private final AuthContext authContext;

    private final ToAuthentication toAuthentication;


    public RestControllerAdvice(AuthContext authContext, ToAuthentication toAuthentication) {
        this.authContext = authContext;
        this.toAuthentication = toAuthentication;
    }

    @ModelAttribute
    public IngestConfiguration ingestConfiguration(@RequestParam(value = "deferInference", required = false, defaultValue = "false") boolean deferInference, @RequestParam(value = "normalizePayload", required = false, defaultValue = "true") boolean normalizePayload) {
        IngestConfiguration ingestConfiguration = new IngestConfiguration();
        ingestConfiguration.setDeferInference(deferInference);
        ingestConfiguration.setNormalizePayload(normalizePayload);
        return ingestConfiguration;
    }


    @ModelAttribute
    public ResponseConfiguration responseConfiguration(@RequestParam(value = "returnPayload", required = false, defaultValue = "true") boolean returnPayload, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "true") boolean returnEmbedded) {
        ResponseConfiguration responseConfiguration = new ResponseConfiguration();
        responseConfiguration.setReturnAlternatives(returnAlternatives);
        responseConfiguration.setReturnEmbedded(returnEmbedded);
        responseConfiguration.setReturnPayload(returnPayload);
        responseConfiguration.setReturnPermissions(returnPermissions);
        return responseConfiguration;
    }


    /**
     * Defines the model attribute of the pagination parameters used in several queries.
     */
    @ModelAttribute
    public PaginationParam paginationParam(@RequestParam(value = "from", required = false, defaultValue = "0") long from, @RequestParam(value = "size", required = false) Long size) {
        PaginationParam paginationParam = new PaginationParam();
        paginationParam.setFrom(from);
        paginationParam.setSize(size);
        return paginationParam;
    }

    /**
     * Retrieves the authorization headers (user and client) and populates them in the {@link AuthContext}
     */
    @ModelAttribute
    public void interceptAuthorizationToken(@RequestHeader(value = "Authorization", required = false) String userAuthorizationToken, @RequestHeader(value = "Client-Authorization", required = false) String clientAuthorizationToken, @RequestHeader(value = "Client-Id", required = false) String clientId, @RequestHeader(value = "Client-Secret", required = false) String clientSecret, @RequestHeader(value = "Client-SA-Secret", required = false) String clientServiceAccountSecret, @RequestHeader(value = "Transaction-Id", required = false) UUID transactionId) {
        UserAuthToken userToken = null;
        ClientAuthToken clientToken = null;
        if (userAuthorizationToken != null) {
            userToken = new UserAuthToken(userAuthorizationToken);
        }
        if (clientAuthorizationToken != null) {
            clientToken = new ClientAuthToken(clientAuthorizationToken);
        }
        if (clientId != null) {
            if (clientAuthorizationToken != null) {
                throw new InvalidRequestException("You should provide either the Client-Authorization or the Client-Id (with a Client-Secret or Client-SA-Secret) header, but not both.");
            }
            if(clientSecret == null && clientServiceAccountSecret == null){
                throw new InvalidRequestException("You should provide either the Client-Secret or Client-SA-Secret header with Client-Id.");
            }
            else if (clientSecret != null && clientServiceAccountSecret != null){
                throw new InvalidRequestException("You should provide either a Client-Secret or a Client-SA-Secret header, but not both.");
            }
            if (clientSecret != null) {
                clientToken = toAuthentication.fetchToken(clientId, clientSecret);
            }
            if (clientServiceAccountSecret != null) {
                if(userToken != null ){
                    throw new InvalidRequestException("You should not provide a Client-SA-Secret header when you've already provided an Authoritation header.");
                }
                //A special treatment for service accounts - the service account of the given client will act as a user account
                ClientAuthToken saClientToken = toAuthentication.fetchToken(clientId, clientServiceAccountSecret);
                userToken = new UserAuthToken(saClientToken.getBearerToken());
            }
        }
        AuthTokens authTokens = new AuthTokens(userToken, clientToken);
        authTokens.setTransactionId(transactionId);
        authContext.setAuthTokens(authTokens);
    }

    @ExceptionHandler({InvalidRequestException.class, AmbiguousException.class})
    protected ResponseEntity<?> handleInvalidRequest(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler({ForbiddenException.class})
    protected ResponseEntity<?> handleForbidden(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler({UnauthorizedException.class})
    protected ResponseEntity<?> handleUnauthorized(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler({ServiceException.class})
    protected ResponseEntity<?> handleServiceException(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(((ServiceException) ex).getStatusCode()).body(ex.getMessage());
    }

    @ExceptionHandler({ServiceNotAvailableException.class})
    protected ResponseEntity<?> handleServiceNotFound(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class})
    protected ResponseEntity<?> handleIllegalState(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }
}
