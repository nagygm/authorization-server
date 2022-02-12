package hu.nagygm.server.web

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import java.util.*


//TODO implement
@RestController
class RestResourceController {

    @GetMapping("#{OAuth2AuthorizationServerEndpointConfiguration.BasePathV1.management}/resources")
    suspend fun get(): Page<ResourceDto> {
        return PageImpl(
            listOf(),
            PageRequest.of(0,10), 0
        )
    }

    @GetMapping("#{OAuth2AuthorizationServerEndpointConfiguration.BasePathV1.management}/resources/{uuid}")
    suspend fun getOne(@PathVariable uuid: UUID): ResourceDto {
        return ResourceDto(UUID.randomUUID(), "fake", "fakehost", "fakedesc", "fakeclientid")
    }

    @PostMapping("#{OAuth2AuthorizationServerEndpointConfiguration.BasePathV1.management}/resources")
    suspend fun create(@RequestBody client: ResourceDto): ResourceDto {
        return ResourceDto(UUID.randomUUID(), "fake", "fakehost", "fakedesc", "fakeclientid")
    }

    @PutMapping("#{OAuth2AuthorizationServerEndpointConfiguration.BasePathV1.management}/resources/{uuid}")
    suspend fun update(@PathVariable uuid: UUID, @RequestBody client: ResourceDto): ResourceDto {
        return ResourceDto(UUID.randomUUID(), "fake", "fakehost", "fakedesc", "fakeclientid")
    }

    data class ResourceDto(
        val id: UUID,
        val name: String,
        val host: String,
        val description: String,
        val clientId: String,
    )
}