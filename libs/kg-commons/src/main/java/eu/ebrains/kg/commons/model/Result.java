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

package eu.ebrains.kg.commons.model;
import java.util.Date;
import java.util.UUID;

public class Result<T> {

    protected T data;
    protected String message;
    protected Error error;
    protected Long startTime;
    protected Long durationInMs;
    protected UUID transactionId;

    public static class Error{
        private int code;
        private String message;
        private UUID instanceId;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public UUID getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(UUID instanceId) {
            this.instanceId = instanceId;
        }
    }

    public static <T> Result<T> ok(){
        return ok(null, null);
    }

    public static <T> Result<T> ok(T data) {
        return ok(data, null);
    }

    public static <T> Result<T> ok(T data, String message) {
        Result<T> result = new Result<>();
        result.data = data;
        if (message != null) {
            result.message = message;
        }
        return result;
    }

    public static <T> Result<T> nok(int errorCode, String message){
        return nok(errorCode, message, null);
    }
    public static <T> Result<T> nok(int errorCode, String message, UUID uuid){
        Result<T> result = new Result<>();
        Error error = new Error();
        error.setCode(errorCode);
        error.setMessage(message);
        if(uuid!=null) {
            error.setInstanceId(uuid);
        }
        result.error = error;
        return result;
    }


    public Error getError() {
        return error;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public Result<T> setExecutionDetails(Date startTime, Date endTime){
        this.startTime = startTime.getTime();
        this.durationInMs = endTime.getTime() - this.startTime;
        return this;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getDurationInMs() {
        return durationInMs;
    }
}
