package ru.whyhappen.pcidss

import com.github.kpavlov.jreactive8583.server.Iso8583Server
import com.solab.iso8583.IsoMessage
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest
class PcidssPocApplicationTests {
    @Autowired
    private lateinit var isoServer: Iso8583Server<IsoMessage>

    @Test
    fun `iso8583 server is running and properly configured`() {
        assertTrue(isoServer.isStarted)
        assertNotNull(isoServer.configurer)
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
