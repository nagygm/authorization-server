package hu.nagygm.server.consent

import hu.nagygm.server.ClientRegistrationEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ConsentServiceImpl(
    @Autowired val userDetailsService: ReactiveUserDetailsService, @Autowired val consentRepository: ConsentRepository
) : ConsentService {
    override suspend fun acceptConsent(id: String): ConsentResponse {
        val consent = consentRepository.findByClientId(id)
        consent.accepted = true
        consent.answeredAt = Instant.now()
        consentRepository.save(consent)
        return ConsentResponse(consent.id, true, consent.redirectUrl)
    }

    override suspend fun refuseConsent(id: String): ConsentResponse {
        val consent = consentRepository.findByClientId(id)
        consent.accepted = false
        consent.answeredAt = Instant.now()
        consentRepository.save(consent)
        return ConsentResponse(consent.id, false, consent.redirectUrl)
    }
}

interface ConsentService {
    suspend fun acceptConsent(id: String): ConsentResponse
    suspend fun refuseConsent(id: String): ConsentResponse
}

interface ConsentRepository : ReactiveMongoRepository<ConsentEntity, String> {
    suspend fun findByClientId(clientId: String): ConsentEntity
}

@Document
data class ConsentEntity(
    @Id
    val id: String,
    val scopes: Set<String>,
    val clientId: String,
    val userId: String,
    val createdAt: Instant,
    var answeredAt: Instant?,
    var accepted: Boolean?,
    val redirectUrl: String,
)

data class ConsentResponse(
    val id: String,
    val accepted: Boolean,
    val redirectUrl: String
)
