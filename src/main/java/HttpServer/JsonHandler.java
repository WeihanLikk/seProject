package HttpServer;

import io.netty.channel.Channel;

public interface JsonHandler {
    String jsonHandler ( Channel channel );
}
