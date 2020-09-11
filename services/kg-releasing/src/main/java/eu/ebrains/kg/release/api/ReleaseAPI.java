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

package eu.ebrains.kg.release.api;


import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.release.controller.Release;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/internal/releases")
public class ReleaseAPI {

    @Autowired
    Release release;

    @PutMapping("/{space}/{id}")
    public void releaseInstance(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam(value = "rev", required = false) String revision){
        release.release(new Space(space), id, revision);
    }

    @DeleteMapping("/{space}/{id}")
    public void unreleaseInstance(@PathVariable("space") String space, @PathVariable("id") UUID id){
        release.unrelease(new Space(space), id);
    }

    @GetMapping("/{space}/{id}")
    public ReleaseStatus getReleaseStatus(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope){
        return release.getStatus(new Space(space), id, releaseTreeScope);
    }


}
