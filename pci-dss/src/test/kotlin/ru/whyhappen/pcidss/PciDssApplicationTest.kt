package ru.whyhappen.pcidss

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583Server

@SpringBootTest
@ActiveProfiles("test")
class PciDssApplicationTest {
    @Autowired
    private lateinit var server: Iso8583Server

    @Test
    fun `iso8583 server is running and properly configured`() {
        server.isStarted shouldBe true
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("keystore.path") {
                System.getProperty("java.io.tmpdir") + "/keystore.bcfks"
            }
        }
    }
}
