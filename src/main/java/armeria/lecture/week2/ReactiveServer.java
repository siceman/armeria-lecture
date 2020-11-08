package armeria.lecture.week2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.file.HttpFile;

public class ReactiveServer {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveServer.class);

    public static void main(String[] args) {
        int port = 8081;
        int interval = 300;
        startServer(8081, 100);
        startServer(8082, 300);
        startServer(8083, 500);
    }

    private static void startServer(int port, int interval) {
        final Server server = Server.builder()
                                    .http(port)
                                    .requestTimeoutMillis(0)
                                    .service("/html", HttpFile.builder(ReactiveServer.class.getClassLoader(), "index.html").build().asService())
                                    .service("/animation", new AnimationService(interval))
                                    .build();
        server.start().join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            logger.info("Server has been stopped.");
        }));
    }

}
