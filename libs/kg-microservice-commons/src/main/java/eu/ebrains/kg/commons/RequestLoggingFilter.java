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

import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.models.UserWithRoles;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthContext authContext;

    public RequestLoggingFilter(AuthContext authContext) {
        this.authContext = authContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean publicAPI = request.getRequestURI().startsWith(String.format("/%s", Version.API));
        UUID apiRequestId = UUID.randomUUID();
        if (publicAPI) {
            UserWithRoles userWithRoles;
            try {
                userWithRoles = authContext.getUserWithRoles();
            } catch (UnauthorizedException ex) {
                userWithRoles = null;
            }
            logger.info("{}, {}, {}, {}, {}, {}, {}", StructuredArguments.keyValue("event", "API request"),
                    StructuredArguments.keyValue("id", apiRequestId),
                    StructuredArguments.keyValue("method", request.getMethod()),
                    StructuredArguments.keyValue("path", request.getRequestURI()),
                    StructuredArguments.keyValue("query", request.getQueryString()),
                    StructuredArguments.keyValue("user", userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getNativeId() : "anonymous"),
                    StructuredArguments.keyValue("client", userWithRoles != null ? userWithRoles.getClientId() : "unknown"));
            filterChain.doFilter(request, response);
            logger.info("{}, {}, {}, {}, {}, {}, {}, {}", StructuredArguments.keyValue("event", "API response"),
                    StructuredArguments.keyValue("id", apiRequestId),
                    StructuredArguments.keyValue("method", request.getMethod()),
                    StructuredArguments.keyValue("path", request.getRequestURI()),
                    StructuredArguments.keyValue("query", request.getQueryString()),
                    StructuredArguments.keyValue("statusCode", response.getStatus()),
                    StructuredArguments.keyValue("user", userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getNativeId() : "anonymous"),
                    StructuredArguments.keyValue("client", userWithRoles != null ? userWithRoles.getClientId() : "unknown"));
        } else {
            filterChain.doFilter(request, response);
        }

    }

}