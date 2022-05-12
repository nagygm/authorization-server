package hu.nagygm.server.web

import hu.nagygm.server.mangement.client.r2dbc.ClientProjection
import hu.nagygm.server.mangement.client.r2dbc.R2dbcClientDao
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import java.util.*

//TODO add service layer, add business logic, add permissions, swagger description
@RestController
class RestClientController(
    @Autowired val r2dbcClientDao: R2dbcClientDao
) {
    @GetMapping("/management/v1/clients")
    suspend fun get(): Page<ClientProjection> {
        val results = r2dbcClientDao.fetchAll()
            .collectList().awaitFirst()
        return PageImpl(
            results,
            PageRequest.of(0, 10), results.size.toLong()
        )
    }

    @GetMapping("/management/v1/clients/{uuid}")
    suspend fun getOne(@PathVariable uuid: UUID): ClientProjection {
        val result = r2dbcClientDao.fetchOne(uuid)
            .awaitFirst()
        return result
    }

    @PostMapping("/management/v1/clients")
    suspend fun create(@RequestBody client: ClientProjection): ClientProjection {
        val result = r2dbcClientDao.save(client)
            .awaitFirst()
        return result
    }

    @PutMapping("/management/v1/clients/{uuid}")
    suspend fun update(@PathVariable uuid: UUID, @RequestBody client: ClientProjection): ClientProjection {
        client.id = uuid
        val result = r2dbcClientDao.save(client)
            .awaitFirst()
        return result
    }


    //TODO change to ClientDto from projection
//    @Schema(description = "Get all users")
//    data class ClientDto()
}