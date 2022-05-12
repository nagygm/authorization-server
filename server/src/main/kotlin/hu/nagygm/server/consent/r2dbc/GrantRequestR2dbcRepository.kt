package hu.nagygm.server.consent.r2dbc

import hu.nagygm.oauth2.server.service.GrantRequest
import io.r2dbc.spi.Row
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.relational.core.mapping.Table
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Repository
interface  GrantRequestR2dbcRepository : R2dbcRepository<GrantRequestTable, String>

interface GrantRequestDao {
    fun getByIdAndClientId(id: String, clientId: String): Mono<out GrantRequest>
    fun getByCodeAndAndClientId(code: String, clientId: String): Mono<out GrantRequest>
    fun getByIdAndAssociatedUserId(id: String, appUserId: String): Mono<out GrantRequest>
}

@Repository
class R2dbcGrantRequestDao(@Autowired private val client: DatabaseClient) : GrantRequestDao {
    val rowToGrantRequestTable = { row: Row ->
        GrantRequestTable(
            redirectUri = row.get("redirect_uri", String::class.java)!!,
            scopes = row.get("scopes", Array<String>::class.java)?.toSet()!!,
            responseType = row.get("response_type", String::class.java)!!,
            clientId = row.get("client_id", String::class.java)!!,
            id = row.get("id", UUID::class.java)!!,
            code = row.get("code", String::class.java),
            state = row.get("state", String::class.java)!!,
            codeCreatedAt = row.get("code_created_at", Instant::class.java),
            requestState = row.get("request_state", String::class.java)!!,
            acceptedScopes = row.get("accepted_scopes", Array<String>::class.java)?.toSet()!!,
            associatedUserId = row.get("associated_user_id", UUID::class.java),
            consentRequestedAt = row.get("consent_requested_at", Instant::class.java),
            processedAt = row.get("processed_at", Instant::class.java),
            codeChallenge = row.get("code_challenge", String::class.java),
            codeChallengeMethod = row.get("code_challenge_method", String::class.java)
        )
    }

    override fun getByIdAndClientId(id: String, clientId: String): Mono<out GrantRequest> {
        return client.sql("""SELECT
            |gr.id, gr.redirect_uri, gr.scopes, gr.response_type, gr.client_id, gr.code, gr.state, gr.code_created_at,
                           gr.request_state, gr.accepted_scopes, gr.associated_user_id, gr.processed_at, gr.consent_requested_at,
                           gr.code_challenge, gr.code_challenge_method
            |  FROM grant_request gr where gr.client_id = :clientId and gr.id::text = :id""".trimMargin())
            .bind("id", id).bind("clientId", clientId)
            .map { r -> GrantRequestAdapter(rowToGrantRequestTable(r)) }.one()
    }

    override fun getByCodeAndAndClientId(code: String, clientId: String): Mono<out GrantRequest> {
        return client.sql("SELECT * FROM grant_request gr where gr.client_id = :clientId and gr.code = :code")
            .bind("code", code).bind("clientId", clientId)
            .map { r -> GrantRequestAdapter(rowToGrantRequestTable(r)) }.one()
    }

    override fun getByIdAndAssociatedUserId(id: String, appUserId: String): Mono<out GrantRequest> {
        return client.sql("SELECT * FROM grant_request gr where gr.associated_user_id::text = :appUserId and gr.id::text = :id")
            .bind("id", id).bind("appUserId", appUserId)
            .map { r -> GrantRequestAdapter(rowToGrantRequestTable(r)) }.one()
    }
}

@Table("grant_request")
class GrantRequestTable(
    val redirectUri: String,
    val scopes: Set<String>,
    val responseType: String,
    val clientId: String,
    @Id
    var id: UUID?,
    var code: String?,
    val state: String,
    var codeCreatedAt: Instant?,
    var requestState: String,
    var acceptedScopes: Set<String>,
    var associatedUserId: UUID?,
    var consentRequestedAt: Instant?,
    var processedAt: Instant?,
    var codeChallenge: String?,
    var codeChallengeMethod: String?
)

class GrantRequestAdapter(
    override val redirectUri: String,
    override val scopes: Set<String>,
    override val responseType: String,
    override val clientId: String,
    override var id: String?,
    override var code: String?,
    override val state: String,
    override var codeCreatedAt: Instant?,
    override var requestState: String,
    override var acceptedScopes: Set<String>,
    override var associatedUserId: String?,
    override var consentRequestedAt: Instant?,
    override var processedAt: Instant?,
    override var codeChallenge: String?,
    override var codeChallengeMethod: String?
) : GrantRequest {
    constructor(grantRequest: GrantRequest) : this(
        grantRequest.redirectUri,
        grantRequest.scopes,
        grantRequest.responseType,
        grantRequest.clientId,
        grantRequest.id,
        grantRequest.code,
        grantRequest.state,
        grantRequest.codeCreatedAt,
        grantRequest.requestState,
        grantRequest.acceptedScopes,
        grantRequest.associatedUserId,
        grantRequest.consentRequestedAt,
        grantRequest.processedAt,
        grantRequest.codeChallenge,
        grantRequest.codeChallengeMethod
    )
    constructor(grantRequestTable: GrantRequestTable) :
            this(
                grantRequestTable.redirectUri,
                grantRequestTable.scopes,
                grantRequestTable.responseType,
                grantRequestTable.clientId,
                grantRequestTable.id?.toString(),
                grantRequestTable.code,
                grantRequestTable.state,
                grantRequestTable.codeCreatedAt,
                grantRequestTable.requestState,
                grantRequestTable.acceptedScopes,
                grantRequestTable.associatedUserId?.toString(),
                grantRequestTable.consentRequestedAt,
                grantRequestTable.processedAt,
                grantRequestTable.codeChallenge,
                grantRequestTable.codeChallengeMethod
            )
}
