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

package eu.ebrains.kg.commons.permission.roles;

import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A role consolidates functionality elements and can be hierarchically stacked.
 * <p>
 * Example: Space-administrator inherits the permissions of Space-Curator etc...
 */
public enum ClientRole {

    //A power client can do almost everything except for manipulating clients.
    POWER_CLIENT(Arrays.stream(Functionality.values()).filter(f -> f.getFunctionalityGroup()!= Functionality.FunctionalityGroup.CLIENT).collect(Collectors.toList()).toArray(new Functionality[0])),

    //An admin client can do everything
    ADMIN(Functionality.values()),

    //This is a marker role -> it is to be able to flag a user as a technical (client) user.
    IS_CLIENT();

    private final String name;
    private final Set<Functionality> functionality;

    ClientRole(Functionality... functionality) {
        this.name = name().toLowerCase();
        this.functionality = new HashSet<>(Arrays.asList(functionality));
    }

    public String getName() {
        return name;
    }

    public Set<Functionality> getFunctionality() {
        return functionality;
    }

    public static ClientRole getRole(String role){
        return Arrays.stream(ClientRole.values()).filter(r -> r.toRole().getName().equals(role)).findFirst().orElse(null);
    }


    public static Set<FunctionalityInstance> functionalitiesForRole(String role){
        ClientRole clientRole = getRole(role);
        if(clientRole!=null){
            return clientRole.getFunctionality().stream().map(f -> new FunctionalityInstance(f, null, null)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public Role toRole() {
        return new Role(getName());
    }


}
