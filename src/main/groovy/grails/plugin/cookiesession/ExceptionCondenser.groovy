/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Ben Lucchesi
 *  benlucchesi@gmail.com
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */
package grails.plugin.cookiesession

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Replaces exception objects in the session with the message string. Exception objects make the serialized
 * session much larger.
 */
@Slf4j
@CompileStatic
class ExceptionCondenser implements SessionPersistenceListener {
    @Override
    void afterSessionRestored(SerializableSession session) {
    }

    @Override
    void beforeSessionSaved(SerializableSession session) {
        // loop through the attributes and condense each exception to just its message
        for (String key : session.attributeNames) {
            if (session.getAttribute(key) instanceof Exception) {
                log.trace('condensing exception: {}', key)
                Exception excp = (Exception) session.getAttribute(key)
                session.setAttribute(key, excp.message)
            }
        }
    }
}
