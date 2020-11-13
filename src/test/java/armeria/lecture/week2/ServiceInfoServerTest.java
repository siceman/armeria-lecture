package armeria.lecture.week2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

public class ServiceInfoServerTest {
    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
//            sb.annotatedService(new Object() {
//                // 스프링 스타일로 annotation 기반으로 처리
//                @Post("/registration")
//               public HttpResponse register (AggregatedHttpRequest req) {
//                   return null;
//               }
//            });

            // route를 활용하여 post만 받을수 있도록 지정하는 방식
           // sb.route().post("/registration").methods(HttpMethod.POST).build((ctx, req) -> {});
            final Queue<String> list = new ConcurrentLinkedQueue<>();
            sb.service("/registration", new HttpService() {
                @Override
                public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                    final RequestHeaders headers = req.headers();
                    final HttpMethod method = headers.method();
                    if (HttpMethod.POST != method) {
                        return HttpResponse.of(500);
                    }
                    final CompletableFuture<HttpResponse> future = new CompletableFuture<>();
                    final CompletableFuture<AggregatedHttpRequest> aggregated = req.aggregate();
                    aggregated.thenAccept(aggregatedHttpRequest -> {
                        final String content = aggregatedHttpRequest.contentUtf8();
                        list.add(content);
                        System.out.println(list);
                        future.complete(HttpResponse.of(200));
                    });
                    return HttpResponse.from(future);
                }
            });

            sb.route().get("/discovery").methods(HttpMethod.GET).build((ctx, req) -> {
               return HttpResponse.of(list.toString());
            });
        }
    };

    @Test
    void registerIp() {
        final WebClient client = WebClient.of(server.httpUri());
        client.post("/registration", "127.0.0.1:8080").aggregate().join();
        client.post("/registration", "127.0.0.1:8081").aggregate().join();
        final String content = client.get("/discovery").aggregate().join().contentUtf8();
        assertThat(content).isEqualTo("127.0.0.1:8080,127.0.0.1:8081");
    }
}
