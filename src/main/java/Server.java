import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class Server {

    private final static Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private final int port;
    private final ExecutorService executeIt;

    private final List<String> allowedMethods = List.of("GET", "POST");
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> allHandlers = new ConcurrentHashMap<>();


    public Server(int port, int numberThreads) {
        this.port = port;
        this.executeIt = newFixedThreadPool(numberThreads);
    }


    public void addHandler(String method, String path, Handler handler) {
        var methodMap = allHandlers.get(method);

        if (methodMap == null) {
            methodMap = new ConcurrentHashMap<>();
            methodMap.put(path, handler);
            allHandlers.put(method, methodMap);
        }
        methodMap.put(path, handler);

        LOGGER.info(" * Add new handler: " + method + " : " + path);
    }


    void listen() {

        LOGGER.info("Server starting...");
        LOGGER.info("Using Port: " + port);

        try (var server = new ServerSocket(this.port)) {
            while (true) {
                var socket = server.accept();
                executeIt.execute(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleConnection(Socket socket) {
        try (var in = new BufferedInputStream(socket.getInputStream());
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }

            // читаем request line
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

            LOGGER.info("Connection[" + socket.getInetAddress() + "/" + socket.getPort() + "] * Read " +
                    "requestLine: " + Arrays.deepToString(requestLine));

            if (requestLine.length != 3) {
                badRequest(out);
                return;
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return;
            }

            final var path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                return;
            }

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            LOGGER.info("Connection[" + socket.getInetAddress() + "/" + socket.getPort() + "] * Read headers: " + headers.toString());

            // тело запроса (только для PUSH)
            if (!method.equals("GET")) {
                in.skip(headersDelimiter.length);
                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var body = new String(in.readNBytes(length));
                    LOGGER.info("Connection[" + socket.getInetAddress() + "/" + socket.getPort() + "] * Read body: " + body);
                }
            }

            final var request = new Request(method, path, headers, null);

            if (!allHandlers.containsKey(request.getMethod())) {
                notFound(out);
                return;
            }

            var methodMap = allHandlers.get(method);
            if (methodMap == null) {
                notFound(out);
            }

            var handler = methodMap.get(request.getPath());
            if (handler == null) {
                notFound(out);
                return;
            }

            handler.handle(request, out);

            LOGGER.info("Connection[" + socket.getInetAddress() + "/" + socket.getPort() + "] * Connection Processing Finished.");
        } catch (IOException e) {
            LOGGER.error("Connection[" + socket.getInetAddress() + "/" + socket.getPort() + "]Problem with communication", e);
            e.printStackTrace();
        }
    }


    private void badRequest(BufferedOutputStream out) throws IOException {

        LOGGER.error("400 Bad Request");

        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void notFound(BufferedOutputStream out) throws IOException {

        LOGGER.error("404 Not Found");

        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }


    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}
