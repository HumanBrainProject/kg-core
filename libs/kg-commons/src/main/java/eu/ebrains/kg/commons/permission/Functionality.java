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

package eu.ebrains.kg.commons.permission;

import eu.ebrains.kg.commons.model.DataStage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Functionality {

    READ_PERMISSION("permissions-read", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null),
    CREATE_PERMISSION("permissions-create", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null),
    DELETE_PERMISSION("permissions-delete", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null),

    //Space mgmt
    READ_SPACE("spaces-read", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.SPACES, null),
    CONFIGURE_SPACE("spaces-configure", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.SPACES, null),
    CREATE_SPACE("spaces-create", Permission.Level.GLOBAL_ONLY, FunctionalityGroup.SPACES, null),
    DELETE_SPACE("spaces-delete", Permission.Level.GLOBAL_ONLY, FunctionalityGroup.SPACES, null),

    //Invitations
    INVITE_FOR_REVIEW("instances-invite4review", Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),
    INVITE_FOR_SUGGESTION("instances-invite4suggestion", Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),

    //Instances
    READ_RELEASED("releasedInstances-read", Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.RELEASED),
    READ("instances-read", Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),
    SUGGEST("instances-suggest", Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),
    WRITE("instances-write", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),
    CREATE("instances-create", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),
    RELEASE("instances-release", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),
    UNRELEASE("instances-unrelease", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.RELEASED),
    DELETE("instances-delete", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS),


    // Client mgmt
    IS_CLIENT("is-client", Permission.Level.GLOBAL_ONLY, FunctionalityGroup.CLIENT, null),
    READ_CLIENT("clients-read", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null),
    CONFIGURE_CLIENT("clients-configure", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null),
    CREATE_CLIENT("clients-create", Permission.Level.GLOBAL_ONLY, FunctionalityGroup.CLIENT, null),
    DELETE_CLIENT("clients-delete", Permission.Level.GLOBAL_ONLY, FunctionalityGroup.CLIENT, null),
    READ_CLIENT_PERMISSION("clientPermissions-read", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null),
    CREATE_CLIENT_PERMISSION("clientPermissions-create", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null),
    DELETE_CLIENT_PERMISSION("clientPermissions-delete", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null),

    //Query
    READ_QUERY("queries-read", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null),
    CREATE_QUERY("queries-create", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null),
    EXECUTE_QUERY("queries-execute", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null),
    EXECUTE_SYNC_QUERY("queries-sync-execute", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null),
    DELETE_QUERY("queries-delete", Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null);


    private final DataStage stage;
    private final String roleName;
    private final List<Permission.Level> allowedPermissionLevels;
    private final FunctionalityGroup functionalityGroup;

    Functionality(String roleName, List<Permission.Level> allowedPermissionLevels, FunctionalityGroup functionalityGroup, DataStage stage) {
        this.roleName = roleName;
        this.allowedPermissionLevels = allowedPermissionLevels;
        this.functionalityGroup = functionalityGroup;
        this.stage = stage;
    }

    public static Functionality fromRoleName(String roleName){
        for (Functionality value : values()) {
            if(value.getRoleName().equals(roleName)){
                return value;
            }
        }
        return null;
    }

    public FunctionalityGroup getFunctionalityGroup(){
        return functionalityGroup;
    }

    public String getRoleName() {
        return roleName;
    }

    public List<Permission.Level> getAllowedPermissionLevels() {
        return allowedPermissionLevels;
    }

    public static List<Functionality> getGlobalFunctionality() {
        return Arrays.stream(values()).filter(f -> f.getAllowedPermissionLevels().contains(Permission.Level.GLOBAL)).collect(Collectors.toList());
    }

    public DataStage getStage() {
        return stage;
    }

    public enum FunctionalityGroup{
        INSTANCE, CLIENT, QUERY, PERMISSIONS, SPACES
    }
}
