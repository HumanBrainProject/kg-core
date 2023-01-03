/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

import eu.ebrains.kg.commons.exception.NotAcceptedTermsOfUseException;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.models.UserWithRoles;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final Logger requestLogger = LoggerFactory.getLogger(getClass());
    private final AuthContext authContext;

    public RequestLoggingFilter(AuthContext authContext) {
        this.authContext = authContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean publicAPI = request.getRequestURI().startsWith(String.format("/%s", Version.V3_BETA)) || request.getRequestURI().startsWith(String.format("/%s", Version.V3));;
        UUID apiRequestId = UUID.randomUUID();
        if (publicAPI) {
            UserWithRoles userWithRoles;
            try {
                userWithRoles = authContext.getUserWithRoles();
            } catch (UnauthorizedException | NotAcceptedTermsOfUseException ex) {
                userWithRoles = null;
            }
            if(requestLogger.isInfoEnabled()) {
                requestLogger.info("action=API request, id={}, method={}, path={}, query={}, authenticatedUser={}, authenticatedClient={}", apiRequestId, request.getMethod(), request.getRequestURI(), request.getQueryString(), userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getNativeId() : "anonymous", userWithRoles != null && userWithRoles.getClientId() != null ? userWithRoles.getClientId() : "direct access");
            }
            Date start = new Date();
            filterChain.doFilter(request, response);
            Date end = new Date();
            if(requestLogger.isInfoEnabled()) {
                requestLogger.info("action=API response, id={}, method={}, path={}, query={}, statusCode={}, executionDuration={} ms, authenticatedUser={}, authenticatedClient={}", apiRequestId, request.getMethod(), request.getRequestURI(), request.getQueryString(), response.getStatus(), end.getTime() - start.getTime(), userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getNativeId() : "anonymous", userWithRoles != null && userWithRoles.getClientId() != null ? userWithRoles.getClientId() : "direct access");
            }
        } else {
            filterChain.doFilter(request, response);
        }

    }

}
