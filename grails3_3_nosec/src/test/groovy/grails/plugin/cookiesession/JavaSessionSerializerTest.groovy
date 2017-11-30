package grails.plugin.cookiesession

import grails.testing.web.controllers.ControllerUnitTest
import grails3_3.IndexController

class JavaSessionSerializerTest extends SessionTests implements SessionFixture, ControllerUnitTest<IndexController> {
    def setup() {
        setupCookieSession('java')
    }
}
