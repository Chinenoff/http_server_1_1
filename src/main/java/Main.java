

public class Main {

    private static final int SERVERPORT = 9999;
    private static final int NUMBERTHREADS = 64;

    public static void main(String[] args) {

        final var server = new Server(SERVERPORT, NUMBERTHREADS);

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/messages", (request, out) -> {
            out.write((
                    "HTTP/1.1 201 Created\r\n" +
                            "Content-Type: text/plain" + "\r\n" +
                            "Content-Length: " + 14 + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n" +
                            "Hello from GET"
            ).getBytes());
            out.flush();
        });
        server.addHandler("POST", "/messages", (request, out) -> {
            out.write((
                    "HTTP/1.1 201 Accepted\r\n" +
                            "Content-Type: " + 0 + "\r\n" +
                            "Content-Length: " + 0 + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        });
        server.listen();
    }
}