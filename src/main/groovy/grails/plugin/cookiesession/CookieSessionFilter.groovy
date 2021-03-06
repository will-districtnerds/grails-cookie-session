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
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */
package grails.plugin.cookiesession

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.BeansException
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Wraps the request and response so that access to the session is delegated to the SessionRepository.
 */
@CompileStatic
@Slf4j
class CookieSessionFilter extends OncePerRequestFilter implements InitializingBean, ApplicationContextAware {
    ApplicationContext applicationContext
    SessionRepository sessionRepository
    private Collection<SessionPersistenceListener> sessionPersistenceListeners
    private boolean enforceSession

    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.trace('setApplicationContext()')
        this.applicationContext = applicationContext
    }

    @Override
    void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet()
        log.trace('afterPropertiesSet()')

        enforceSession = this.applicationContext.containsBeanDefinition('securityContextSessionPersistenceListener')

        Map beans = applicationContext.getBeansOfType(SessionPersistenceListener)
        sessionPersistenceListeners = beans.values()
        log.trace('added listeners: {}', beans.keySet())
    }

    @Override
    protected void initFilterBean() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        log.trace('doFilterInternal(), request type: {}, response type: {}, filter chain type: {}',
                request.class.name, response.class.name, chain.class.name)

        SessionRepositoryRequestWrapper requestWrapper = new SessionRepositoryRequestWrapper(request, sessionRepository)
        requestWrapper.servletContext = this.servletContext
        requestWrapper.setSessionPersistenceListeners(this.sessionPersistenceListeners)
        requestWrapper.restoreSession()

        // if spring security integration is supported it is necessary to enforce session creation
        // if one does not exist yet. otherwise the security context will not be persisted and
        // propagated between requests if the application did not happen to use a session yet.

        SessionRepositoryResponseWrapper responseWrapper = new SessionRepositoryResponseWrapper(
                response, sessionRepository, requestWrapper, this.enforceSession)
        responseWrapper.setSessionPersistenceListeners(this.sessionPersistenceListeners)
        chain.doFilter(requestWrapper, responseWrapper)
    }
}
