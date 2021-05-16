package hu.nagygm.server

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class AuthorizationServerApplicationTest : AnnotationSpec() {
    @Test
    fun `Should return zettelModule root path HELLO WORLD`() {
        val a = "Hello World"
        a shouldBe "Hello World"
    }

}
