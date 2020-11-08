package armeria.lecture.week2;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.endpoint.EndpointGroup;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;

public class ProxyServer {

    public static void main(String[] args) {
        final Server server = Server.builder()
                                    .http(8080)
                                    .requestTimeoutMillis(0)
                                    .serviceUnder("/", new ProxyService())
                                    .build();
        server.start().join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            System.err.println("Server has been stopped.");
        }));
    }

    private static class ProxyService implements HttpService {
        private final WebClient client;

        ProxyService() {
            final EndpointGroup endpointGroup = EndpointGroup.of(Endpoint.of("127.0.0.1", 8081),
                                                                 Endpoint.of("127.0.0.1", 8082),
                                                                 Endpoint.of("127.0.0.1", 8083));
            client = WebClient.of(SessionProtocol.HTTP, endpointGroup);
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            return client.execute(req);
        }
    }
}
