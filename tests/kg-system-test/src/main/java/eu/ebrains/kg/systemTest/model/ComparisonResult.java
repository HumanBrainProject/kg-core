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

package eu.ebrains.kg.systemTest.model;

import java.util.Map;

public class ComparisonResult<T> {

    T expectedValue;
    T actualValue;
    boolean correct;
    String message;
    Map<String, ?> extraInformation;

    public Map<String, ?> getExtraInformation() {
        return extraInformation;
    }

    public void setExtraInformation(Map<String, ?> extraInformation) {
        this.extraInformation = extraInformation;
    }

    public T getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(T expectedValue) {
        this.expectedValue = expectedValue;
    }

    public T getActualValue() {
        return actualValue;
    }

    public void setActualValue(T actualValue) {
        this.actualValue = actualValue;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
