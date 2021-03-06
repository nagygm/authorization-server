package hu.nagygm.server

import org.springframework.stereotype.Service

@Service
class SimpleComponent {
    fun foo() = "Bar" + "2"
}
