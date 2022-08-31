import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Request {

    private final static Logger LOGGER = LoggerFactory.getLogger(Request.class);

    private final String method;
    private final String path;
    private final List<String> headers;
    private final String body;

    public Request(String method, String path, List<String> headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        LOGGER.info("Create new Request: " + this);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String toString() {
        return "Метод запроса: " + method + "\n\n" +
                "Путь: " + path + "\n\n" +
                "Заголовки: " + headers.toString() + "\n\n" +
                "Тело запроса: " + body;
    }
}