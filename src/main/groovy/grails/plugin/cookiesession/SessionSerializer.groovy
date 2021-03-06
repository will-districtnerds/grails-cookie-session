/*
 * Copyright 2012-2018 the original author or authors.
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
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */
package grails.plugin.cookiesession

/**
 * Interface for objects that can serialize an HttpSession into an OutputStream and deserialize from
 * an InputStream. It is expected that given an implementation of this interface can deserialize sessions
 * across JVM invocations assuming any configuration remains the same. It is not expected that differing
 * implementations can deserialize each other's sessions.
 *
 * Do not compress, encrypt or encode the serialized data. These functions will be done by the caller.
 */
interface SessionSerializer {

    void serialize(SerializableSession session, OutputStream outputStream)

    void serialize(Map<String, Serializable> attributes, OutputStream outputStream)

    SerializableSession deserialize(InputStream serializedSession)
}
