package demo

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.PartData
import io.micronaut.http.multipart.StreamingFileUpload
import io.reactivex.Flowable
import io.reactivex.Single
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory

@Controller("demo")
class DemoEndpoint {
    private val log = LoggerFactory.getLogger(javaClass)!!

    @Post(
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON]
    )
    fun test(data: StreamingFileUpload): Flowable<HttpResponse<ResponseObject>> {
        var numBytes = 0L

        return Single.create<HttpResponse<ResponseObject>> { emitter ->
            Flowable.fromPublisher(data).subscribeWith(
                object : Subscriber<PartData> {
                    lateinit var sub: Subscription

                    override fun onSubscribe(s: Subscription) {
                        sub = s
                        sub.request(1)
                    }

                    override fun onNext(t: PartData) {
                        numBytes += t.byteBuffer.remaining()
                        sub.request(1)
                    }

                    override fun onComplete() {
                        log.error("ATTENTION: THIS FIRES!!!")

                        emitter.onSuccess(HttpResponse.ok(ResponseObject("Read $numBytes bytes")))
                    }

                    override fun onError(t: Throwable) {
                        emitter.onError(t)
                    }
                }
            )
        }.toFlowable()
    }
}

data class ResponseObject(val message: String)
