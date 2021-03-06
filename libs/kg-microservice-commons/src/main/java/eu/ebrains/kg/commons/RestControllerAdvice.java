/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.exception.*;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

/**
 * This controller advice handles the population of the {@link AuthContext}, provides shared parameters (such as {@link PaginationParam}) and translates exceptions into http status codes.
 */
@ControllerAdvice(annotations = RestController.class)
public class RestControllerAdvice {

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
     * Validates that there is a valid authorization header combination handed in
     */
    @ModelAttribute
    public void validateAuthorizationTokenCombinations(@RequestHeader(value = "Authorization", required = false) String userAuthorizationToken, @RequestHeader(value = "Client-Authorization", required = false) String clientAuthorizationToken, @RequestHeader(value = "Client-Id", required = false) String clientId, @RequestHeader(value = "Client-Secret", required = false) String clientSecret, @RequestHeader(value = "Client-SA-Secret", required = false) String clientServiceAccountSecret) {
        if (clientAuthorizationToken != null) {
            if(clientId !=null || clientSecret != null || clientServiceAccountSecret!=null) {
                throw new InvalidRequestException("You've provided a Client-Authorization header, so you shouldn't define any of Client-Id, Client-Secret or Client-SA-Secret.");
            }
        }
        if (clientId != null) {
            if(clientSecret == null && clientServiceAccountSecret == null){
                throw new InvalidRequestException("You should provide either the Client-Secret or Client-SA-Secret header with Client-Id.");
            }
            else if (clientSecret != null && clientServiceAccountSecret != null){
                throw new InvalidRequestException("You should provide either a Client-Secret or a Client-SA-Secret header, but not both.");
            }
            if (clientServiceAccountSecret != null && userAuthorizationToken!=null) {
               throw new InvalidRequestException("You should not provide a Client-SA-Secret header when you've already provided an Authorization header.");
            }
        }
    }

    @ExceptionHandler({InstanceNotFoundException.class})
    protected ResponseEntity<?> handleInstanceNotFound(RuntimeException ex, WebRequest request) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({InvalidRequestException.class, AmbiguousException.class})
    protected ResponseEntity<?> handleInvalidRequest(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler({ForbiddenException.class, NotAcceptedTermsOfUseException.class})
    protected ResponseEntity<?> handleForbidden(RuntimeException ex, WebRequest request) {
        if(ex instanceof NotAcceptedTermsOfUseException){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(((NotAcceptedTermsOfUseException)ex).getTermsOfUseError());
        }
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
