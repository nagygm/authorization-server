package hu.nagygm.server.web

import hu.nagygm.server.mangement.client.R2dbcClientDao
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerResponse
import java.util.*

@Service
class ClientHandler(
    @Autowired val r2dbcClientDao: R2dbcClientDao
) {
    suspend fun findOne(uuid: UUID): ServerResponse {
        val result = r2dbcClientDao.fetchOne(uuid)
            .awaitFirst()
        return ServerResponse.ok().bodyValue(result).awaitFirst()
    }
}