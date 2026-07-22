/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
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
package eu.mihosoft.vmf.vmftext;

/**
 * Thrown when VMF-Text code generation fails.
 *
 * <p>Generation used to swallow failures (print a stack trace and continue),
 * which produced a "successful" build with missing or partial generated code.
 * Generation now fails fast by throwing this exception; the Gradle plugin
 * surfaces it as a {@code GradleException} so the build aborts with a clear
 * message instead of silently emitting broken sources.</p>
 */
public class VMFTextGenerationException extends RuntimeException {

    public VMFTextGenerationException(String message) {
        super(message);
    }

    public VMFTextGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
