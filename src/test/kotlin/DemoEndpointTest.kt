package demo

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.StreamingHttpClient
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.runtime.server.EmbeddedServer
import io.reactivex.Flowable
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoEndpointTest {
    private lateinit var embeddedServer: EmbeddedServer
    private lateinit var client: StreamingHttpClient
    private lateinit var objectMapper: ObjectMapper

    @BeforeAll
    fun initServer() {
        embeddedServer = ApplicationContext.run(
            EmbeddedServer::class.java,
            "test"
        )

        while (!embeddedServer.isRunning) {
        }

        client = HttpClient.create(embeddedServer.url) as StreamingHttpClient
        objectMapper = embeddedServer.applicationContext.getBean(ObjectMapper::class.java)
    }

    @AfterAll
    fun destroyServer() {
        if (::embeddedServer.isInitialized)
            runCatching { embeddedServer.close() }
    }

    @Test
    fun `exchange stream will hang here while retrieving the response`() {
        val bytes = ByteArray(4096) { (it % 256).toByte() }

        val body = MultipartBody.builder()
            .addPart(
                "data",
                "randomFileName.dat",
                MediaType.APPLICATION_OCTET_STREAM_TYPE,
                ByteArrayInputStream(bytes),
                bytes.size.toLong()
            )

        val request = HttpRequest.POST("demo", body)
            .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        val responseFlowable = Flowable.fromPublisher(client.exchangeStream(request))
        val httpResponse = responseFlowable.blockingLast()

        val responseObject = objectMapper.readValue(httpResponse.body()!!.toByteArray(), ResponseObject::class.java)

        assertEquals(200, httpResponse.status.code)
        assertEquals("Read 4096 bytes", responseObject.message)
    }
}
