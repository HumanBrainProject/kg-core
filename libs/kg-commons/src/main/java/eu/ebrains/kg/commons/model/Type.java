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

package eu.ebrains.kg.commons.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Type {
    private String name;
    private List<String> labelProperties;

    public List<String> getLabelProperties() {
        return labelProperties;
    }

    public void setLabelProperties(List<String> labelProperties) {
        this.labelProperties = labelProperties;
    }

    public Type() {

    }


    public Type(String name) {
        this.name = URLDecoder.decode(name, StandardCharsets.UTF_8);
    }

    public String getName() {
        return name;
    }

    public String getEncodedName(){
        return URLEncoder.encode(getName(), StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object obj) {
        Type that =  (Type)obj;
        return this.name.equals(that.getName());
    }

    public static String labelFromName(String fullyQualifiedName){
        if(fullyQualifiedName!=null) {
            if (fullyQualifiedName.startsWith("@")) {
                return fullyQualifiedName.replace("@", "");
            }
            if(fullyQualifiedName.lastIndexOf("#")>-1){
                return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf("#")+1);
            }
            try {
                URI uri = new URI(fullyQualifiedName);
                return uri.getPath()!=null ? uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1) : null;
            } catch (URISyntaxException e) {
                return fullyQualifiedName;
            }
        }
        return null;
    }


}
