package HttpServer;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

public interface JsonHandler {
    String jsonHandler ( Channel channel, FullHttpRequest request, String[] contents );
}
