package hu.nagygm.server

import hu.nagygm.oauth2.client.registration.ClientConfigurationMap
import hu.nagygm.oauth2.client.registration.ClientConfigurationParamKeys
import hu.nagygm.server.web.UserController
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

class AuthorizationServerApplicationTest : AnnotationSpec() {
    override fun extensions() = listOf(SpringExtension)


    @Test
    fun `Try ClientConfigurationMap api`() {
        val ccm = ClientConfigurationMap()
        ccm.put(ClientConfigurationParamKeys.AccessTokenLifetime, 1)
        ccm.get(ClientConfigurationParamKeys.AccessTokenLifetime).plus(1) shouldBe 2
        ccm.put(ClientConfigurationParamKeys.Asd, "")
    }


}

