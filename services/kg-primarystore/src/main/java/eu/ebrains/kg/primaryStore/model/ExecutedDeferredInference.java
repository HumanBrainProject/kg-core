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

package eu.ebrains.kg.primaryStore.model;

public class ExecutedDeferredInference {
   private final DeferredInference deferredInference;
   private final boolean successful;
   private final Exception exception;

   public ExecutedDeferredInference(DeferredInference deferredInference, boolean successful, Exception exception) {
      this.deferredInference = deferredInference;
      this.successful = successful;
      this.exception = exception;
   }

   public Exception getException() {
      return exception;
   }

   public DeferredInference getDeferredInference() {
      return deferredInference;
   }

   public boolean isSuccessful() {
      return successful;
   }
}
