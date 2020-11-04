package armeria.lecture.week1;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpObject;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.util.EventLoopGroups;

import io.netty.channel.EventLoop;

class ReactiveStreamsSubscriberTest {

    @Test
    void aggregate() {
        final HttpResponse res = HttpResponse.of(ResponseHeaders.of(200), HttpData.ofUtf8("foo"));
        // HTTP/1.1 200 OK
        // Content-length: 3
        //
        // foo

        assert res instanceof Publisher;

        final CompletableFuture<AggregatedHttpResponse> aggregated = res.aggregate();
        final AggregatedHttpResponse aggregatedHttpResponse = aggregated.join();
        System.err.println(aggregatedHttpResponse.headers().status());
        System.err.println(aggregatedHttpResponse.contentUtf8());
    }

    @Test
    void customSubscriber() {
        final HttpResponse res = HttpResponse.of(ResponseHeaders.of(200), HttpData.ofUtf8("foo"));

        final CompletableFuture<MyAggregatedHttpResponse> aggregated = aggregate(res);
        final MyAggregatedHttpResponse aggregatedHttpResponse = aggregated.join();
        System.err.println(aggregatedHttpResponse.headers().status());
        System.err.println(aggregatedHttpResponse.contentUtf8());
    }

    private CompletableFuture<MyAggregatedHttpResponse> aggregate(HttpResponse res) {
        final CompletableFuture<MyAggregatedHttpResponse> future = new CompletableFuture<>();
        final EventLoop executors = EventLoopGroups.newEventLoopGroup(1).next();
        res.subscribe(new Subscriber<HttpObject>() {
            private Subscription s;
            private ResponseHeaders headers;
            private HttpData data;
            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
                currentThreadName("onSubscribe");
            }

            @Override
            public void onNext(HttpObject httpObject) {
                if (httpObject instanceof ResponseHeaders) {
                    headers = (ResponseHeaders) httpObject;
                } else if (httpObject instanceof HttpData) {
                    data = (HttpData) httpObject;
                }
                s.request(1);
                currentThreadName("onNext");
            }

            @Override
            public void onError(Throwable t) {
                currentThreadName("onError");
            }

            @Override
            public void onComplete() {
                future.complete(new MyAggregatedHttpResponse(headers, data));
                currentThreadName("onComplete");
            }
        });
        return future;
    }

    private static void currentThreadName(String method) {
        System.err.println("Name: " + Thread.currentThread().getName() + " (in " + method + ')');
    }

    static class MyAggregatedHttpResponse {

        private final ResponseHeaders responseHeaders;
        private final HttpData httpData;

        MyAggregatedHttpResponse(ResponseHeaders responseHeaders, HttpData httpData) {
            this.responseHeaders = responseHeaders;
            this.httpData = httpData;
        }

        ResponseHeaders headers() {
            return responseHeaders;
        }

        public String contentUtf8() {
            return httpData.toStringUtf8();
        }
    }
}
