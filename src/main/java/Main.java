

public class Main {
    public static void main(String[] args) {
        //Запуск сервера с передачей параметров запуска через командную строку
        //(номер_порта, число_потоков_в_ThreadPool, путь_к_папке_с_файлами)
        //Например  ->  9999 64 D:\00_java\All_My_Project\http-server_v11\files
        var port = Integer.parseInt(args[0]);
        var numberThreads = Integer.parseInt(args[1]);
        //var directory = args[2];
        final var server = new Server(port, numberThreads);

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