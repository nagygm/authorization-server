package hu.nagygm.oauth2.server.router

import hu.nagygm.oauth2.config.OAuth2Api
import io.kotest.core.spec.style.AnnotationSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [(OAuth2Api::class)])
class RouterTest : AnnotationSpec() {
    @Autowired
    lateinit var api: OAuth2Api

}