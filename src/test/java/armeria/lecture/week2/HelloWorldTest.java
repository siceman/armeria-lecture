package armeria.lecture.week2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

public class HelloWorldTest {
    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/api", new HttpService() {
                @Override
                public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
//                    final HttpResponseWriter future = HttpResponse.streaming();
//                    final CompletableFuture<AggregatedHttpRequest> aggregated = req.aggregate();
//                    aggregated.thenAccept(aggregatedHttpRequest -> {
//                        final String content = aggregatedHttpRequest.contentUtf8();
//                        future.write();
//                        future.close();
//                        ...
//                    });
//                     
                    final CompletableFuture<HttpResponse> future  = new CompletableFuture<>();
                    final CompletableFuture<AggregatedHttpRequest> aggregated = req.aggregate();
                    aggregated.thenAccept(aggregatedHttpRequest -> {
                       final String content = aggregatedHttpRequest.contentUtf8();
                       future.complete(HttpResponse.of("Hello "+ content));
                    });
                    return HttpResponse.from(future);
                }
            });
        }

        @Override
        protected boolean runForEachTest() {
            // test method마다 서버를 새로 띄우고 싶으면 true로 설정
            return false;
        }
    };

    @Test
    void aaa() {
        final WebClient client = WebClient.of(server.httpUri());
        final String content = client.post("/api", "Armeria").aggregate().join().contentUtf8();
        assertThat(content).isEqualTo("Hello Armeria");
    }
}
