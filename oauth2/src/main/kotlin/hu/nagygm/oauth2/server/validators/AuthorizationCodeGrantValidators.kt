package hu.nagygm.oauth2.server.validators

interface Validator<T> {
    fun validate(value: T): Result?
    interface Result {
        val isOk: Boolean
        val message: String?

        companion object {
            val OK: Result = object : Result {
                override val message: String?
                    get() = "OK"

                override fun toString(): String {
                    return "OK"
                }

                override val isOk: Boolean
                    get() = true
            }
        }
    }
}