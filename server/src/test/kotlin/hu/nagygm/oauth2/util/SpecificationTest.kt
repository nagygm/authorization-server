package hu.nagygm.oauth2.util

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class SpecificationTest : AnnotationSpec() {

    @Test
    fun `Tests simple TestSpecification when businessFieldis 10 then returns with satisfied result`() {
        val testSpecification = TestSpecification(10)
        val result = testSpecification.isSatisfiedBy(ObjectUnderTest(10))
        result.isSatisfied shouldBe true
    }

    @Test
    fun `Tests simple Specification OR operation when on returns true`() {
        val firstSpecification = TestSpecification(10)
        val differentSpecification = TestSpecification(9)
        val result = firstSpecification.or(differentSpecification).isSatisfiedBy(ObjectUnderTest(10))
        result.isSatisfied shouldBe true
    }

    @Test
    fun `Tests simple Specification OR operation when all specification returns false`() {
        val firstSpecification = TestSpecification(8)
        val differentSpecification = TestSpecification(9)
        val result = firstSpecification.or(differentSpecification).isSatisfiedBy(ObjectUnderTest(10))
        result.isSatisfied shouldBe false
    }

    @Test
    fun `Tests simple Specification AND operation whne one returns false`() {
        val firstSpecification = TestSpecification(10)
        val differentSpecification = TestSpecification(9)
        val result = (firstSpecification and differentSpecification).isSatisfiedBy(ObjectUnderTest(10))
        result.isSatisfied shouldBe false
    }

    @Test
    fun `Tests simple Specification AND operation when all succeeds`() {
        val firstSpecification = TestSpecification(10)
        val differentSpecification = TestSpecification(10)
        val result = firstSpecification.and(differentSpecification).isSatisfiedBy(ObjectUnderTest(10))
        result.isSatisfied shouldBe true
    }

    @Test
    fun `Tests not operation on Specification`() {
        val firstSpecification = TestSpecification(10)
        val result = firstSpecification.not().isSatisfiedBy(ObjectUnderTest(10))
        result.isSatisfied shouldBe false
    }

    class ObjectUnderTest(val businessField: Int)

    class TestSpecification(private val validValue: Int) : CompositeSpecification<ObjectUnderTest, TestSpecificationResult>() {
        override fun isSatisfiedBy(candidate : ObjectUnderTest): TestSpecificationResult {
            return TestSpecificationResult(
                    candidate.businessField == validValue
            )
        }
    }

    class TestSpecificationResult (override val isSatisfied: Boolean) :
        SpecificationResult<TestSpecificationResult> {
        override fun createResult(isSatisfied: Boolean): TestSpecificationResult {
            return TestSpecificationResult(isSatisfied)
        }
    }
}
