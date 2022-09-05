import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

public class Request {

    private final static Logger LOGGER = LoggerFactory.getLogger(Request.class);

    private final String method;
    private final String path;
    private final List<String> headers;
    private final String body;
    private List<NameValuePair> params;

    public Request(String method, String path, List<String> headers,
                   String body) throws URISyntaxException {
        this.method = method;
        this.path = this.getShortPath(path);
        this.headers = headers;
        this.body = body;
        this.params = URLEncodedUtils.parse(this.getRequestParams(path), Charset.forName("UTF-8"));

        System.out.println("Request Params Parsing...");

        for (NameValuePair param : params) {
            System.out.println(param.getName() + " : " + param.getValue());
        }
        System.out.println("");

        LOGGER.info(" Create new Request: \n" + this);
    }

    public String getShortPath(String path) {
        if (path.indexOf('?') != -1) {
            final var pathParams = path.split("\\?");
            var pathDelParams = pathParams[0];
            LOGGER.info(" PATH: " + pathParams[0]);
            return pathDelParams;
        }
        return path;
    }

    public String getRequestParams(String path) {
        if (path.indexOf('?') != -1) {
            final var pathParams = path.split("\\?");
            var stringParams = pathParams[1];
            LOGGER.info(" PARAMS: " + pathParams[1]);
            return stringParams;
        }
        return null;
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
        return "Метод запроса: " + method + "\n" +
                "Путь: " + path + "\n" +
                "Заголовки: " + headers.toString() + "\n" +
                "Тело запроса: " + body + "\n";
    }
}