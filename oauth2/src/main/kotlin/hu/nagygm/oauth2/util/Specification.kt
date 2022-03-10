package hu.nagygm.oauth2.util

interface Specification<T, R : SpecificationResult<R>> {
    fun isSatisfiedBy(candidate: T): R
    infix fun and(specification: Specification<T,R>): Specification<T,R>
    infix fun andNot(specification: Specification<T,R>): Specification<T,R>
    infix fun or(specification: Specification<T,R>): Specification<T,R>
    infix fun orNot(specification: Specification<T,R>): Specification<T,R>
    fun not(): Specification<T,R>
}

interface SpecificationResult<R : SpecificationResult<R>> {
    val isSatisfied: Boolean
    infix fun and(satisfiedBy: SpecificationResult<R>): R {
        return createResult(this.isSatisfied && satisfiedBy.isSatisfied)
    }
    infix fun or(satisfiedBy: SpecificationResult<R>): R {
        return createResult(this.isSatisfied || satisfiedBy.isSatisfied)
    }
    fun not(): R {
        return createResult(this.isSatisfied.not())
    }
    fun createResult(isSatisfied: Boolean): R
}

abstract class CompositeSpecification<T, R: SpecificationResult<R>>() : Specification<T,R> {
    abstract override fun isSatisfiedBy(candidate: T): R

    override fun and(specification: Specification<T, R>): Specification<T,R> {
        return AndSpecification(this, specification)
    }

    override fun andNot(specification: Specification<T,R>): Specification<T,R> {
        return AndSpecification(this, specification.not())
    }

    override fun or(specification: Specification<T,R>): Specification<T,R> {
        return OrSpecification(this, specification)
    }

    override fun orNot(specification: Specification<T,R>): Specification<T,R> {
        return OrSpecification(this, specification.not())
    }

    override fun not(): Specification<T,R> {
        return NotSpecification(this)
    }
}

class AndSpecification<T, R : SpecificationResult<R>>(private val left: Specification<T, R>, private val right: Specification<T,R>) : CompositeSpecification<T,R>() {
    override fun isSatisfiedBy(candidate: T): R {
        return left.isSatisfiedBy(candidate) and right.isSatisfiedBy(candidate)
    }
}

class OrSpecification<T, R : SpecificationResult<R>>(private val left: Specification<T, R>, private val right: Specification<T,R>) : CompositeSpecification<T,R>() {
    override fun isSatisfiedBy(candidate: T): R {
        return left.isSatisfiedBy(candidate) or right.isSatisfiedBy(candidate)
    }
}

class NotSpecification<T, R : SpecificationResult<R>>(private val left: Specification<T, R>) : CompositeSpecification<T,R>() {
    override fun isSatisfiedBy(candidate: T): R {
        return left.isSatisfiedBy(candidate).not()
    }
}
