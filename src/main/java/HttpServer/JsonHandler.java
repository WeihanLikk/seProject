package HttpServer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.sql.SQLException;

public interface JsonHandler {
    String jsonHandler ( FullHttpRequest request, String[] contents ) throws SQLException;
}
