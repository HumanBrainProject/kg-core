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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.release.api;


import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/internal/releases")
public class ReleaseAPIRest implements eu.ebrains.kg.commons.api.Release {

    private final ReleaseAPI releaseAPI;

    public ReleaseAPIRest(ReleaseAPI releaseAPI) {
        this.releaseAPI = releaseAPI;
    }

    @PutMapping("/{space}/{id}")
    public void releaseInstance(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam(value = "rev", required = false) String revision){
        releaseAPI.releaseInstance(space, id, revision);
    }

    @DeleteMapping("/{space}/{id}")
    public void unreleaseInstance(@PathVariable("space") String space, @PathVariable("id") UUID id){
        releaseAPI.unreleaseInstance(space, id);
    }

    @GetMapping("/{space}/{id}")
    public ReleaseStatus getReleaseStatus(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope){
        return releaseAPI.getReleaseStatus(space, id, releaseTreeScope);
    }

}
