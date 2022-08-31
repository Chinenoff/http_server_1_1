import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class Server {

    private static List<String> validPaths;


    public void runServer(int portServer, int numberThread, List<String> validPaths) {
        try (ServerSocket server = new ServerSocket(portServer)) {
            ExecutorService executeIt = newFixedThreadPool(numberThread);
            this.validPaths = validPaths;

            while (!server.isClosed()) {
                Socket client = server.accept();
                executeIt.execute(() -> handle(client));
            }
            executeIt.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handle(Socket socket) {

        try (var out = new BufferedOutputStream(socket.getOutputStream());
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             socket) {

            final var requestLine = in.readLine(); // Получаем входящую строку
            final var parts = requestLine.split("\\s+"); // Делим ее по пробелам

            if (parts.length != 3) {
                return;
            }

            final var path = parts[1]; // Проверяем 2й элемент (URL) существует ли у нас такой ресурс, если нет то 404
            if (!validPaths.contains(path)) {
                out.write(("HTTP/1.1 404 Not Found\r\n" + // Формируем response status line
                        "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                out.flush();
                return;
            }

            // Если данный ресурс существует:
            final var filePath = Path.of(".", "public", path); // Получаем путь до файла на винте .public + /path
            final var mimeType = Files.probeContentType(filePath); // Определяем тип файла

            // special case for classic или добавим интерактива
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath); // Читаем файл в виде строки
                final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes(); // Заменяем в нем якорь {time} на то что нам нужно и переводим в байты
                out.write(("HTTP/1.1 200 OK\r\n" +
                        "Content-type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                out.write(content);
                out.flush();
                System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
                return;
            }

            final var length = Files.size(filePath); // Получаем размер файла в байтах, для передачи в Content-Length
            out.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-type: " + mimeType + "\r\n" +
                    "Content-Length: " + length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            Files.copy(filePath, out); // Копируем файл в выходной поток (отправляем клиенту)
            out.flush();
            System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
