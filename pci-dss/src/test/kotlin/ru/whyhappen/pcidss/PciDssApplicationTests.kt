package ru.whyhappen.pcidss

import com.github.kpavlov.jreactive8583.server.Iso8583Server
import com.solab.iso8583.IsoMessage
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class PciDssApplicationTests {
    @Autowired
    private lateinit var isoServer: Iso8583Server<IsoMessage>

    @Test
    fun `iso8583 server is running and properly configured`() {
        assertTrue(isoServer.isStarted)
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
