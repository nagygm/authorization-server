package hu.nagygm.server

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class KotlinPractice : AnnotationSpec() {
    @Test
    fun `Trying out scope functions`() {
       var s: String? = null
       val hasS = s?.let { it.contains("s") }

        hasS shouldBe null
        
    }


}