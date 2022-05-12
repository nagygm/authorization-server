package hu.nagygm.oauth2.server.security.twofactor

interface MFAService {
    fun validateCode(code: String) : Boolean
}

class TOTPMFAService : MFAService {
    override fun validateCode(code: String) : Boolean = TODO("not implemented")
}

/**
 * Should work like this: for clint we register mandatory 2fa
 * If it has mandatory 2FA during login we include 2FA also
 * Also every login should create fingerprint of machine, and if it is not yet in the db it should send an email
 * to the user to verify login login-page should poll if linked -> this also can be a
 */