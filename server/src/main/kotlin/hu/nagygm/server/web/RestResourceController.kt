package hu.nagygm.server.web

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import java.util.*


//TODO implement
@RestController
class RestResourceController {

    @GetMapping("/management/v1/resources")
    suspend fun get(): Page<ResourceDto> {
        return PageImpl(
            listOf(),
            PageRequest.of(0,10), 0
        )
    }

    @GetMapping("/management/v1/resources/{uuid}")
    suspend fun getOne(@PathVariable uuid: UUID): ResourceDto {
        return ResourceDto(UUID.randomUUID(), "fake", "fakehost", "fakedesc", "fakeclientid")
    }

    @PostMapping("/management/v1/resources")
    suspend fun create(@RequestBody client: ResourceDto): ResourceDto {
        return ResourceDto(UUID.randomUUID(), "fake", "fakehost", "fakedesc", "fakeclientid")
    }

    @PutMapping("/management/v1/resources/{uuid}")
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