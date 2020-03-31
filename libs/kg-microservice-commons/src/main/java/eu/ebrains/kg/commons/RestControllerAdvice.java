/*
 * Copyright 2020 EPFL/Human Brain Project PCO
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
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import eu.ebrains.kg.commons.serviceCall.AuthenticationSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(annotations = RestController.class)
public class RestControllerAdvice {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    AuthContext authContext;

    @Autowired
    AuthenticationSvc authenticationSvc;

    private void validateToken(String token) {
        //TODO validate the token
        boolean validToken = true;
        if (!validToken) {
            throw new UnauthorizedException();
        }
    }

    @ModelAttribute
    public PaginationParam paginationParam(@RequestParam(value = "from", required = false, defaultValue = "0") long from, @RequestParam(value = "size", required = false) Long size) {
        PaginationParam paginationParam = new PaginationParam();
        paginationParam.setFrom(from);
        paginationParam.setSize(size);
        return paginationParam;
    }

    @ModelAttribute
    public void interceptAuthorizationToken(@RequestHeader(value = "Authorization", required = false) String userAuthorizationToken, @RequestHeader(value = "Client-Authorization", required = false) String clientAuthorizationToken) {
        UserAuthToken userToken = null;
        ClientAuthToken clientToken = null;
        if (userAuthorizationToken != null) {
            userToken = new UserAuthToken(userAuthorizationToken);
        }
        if (clientAuthorizationToken != null) {
            clientToken = new ClientAuthToken(clientAuthorizationToken);
        }
        AuthTokens authTokens = new AuthTokens(userToken, clientToken);
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
    protected ResponseEntity<?> handleServiceException(RuntimeException ex, WebRequest request){
        return ResponseEntity.status(((ServiceException)ex).getStatusCode()).body(ex.getMessage());
    }

    @ExceptionHandler({ServiceNotAvailableException.class})
    protected ResponseEntity<?> handleServiceNotFound(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }
}
