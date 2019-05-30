package HttpServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;

public class HttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    @Override
    protected void channelRead0 ( ChannelHandlerContext ctx, HttpObject msg ) throws Exception {
        if ( msg instanceof HttpRequest ) {

            HttpRequest httpRequest = (HttpRequest) msg;

            System.out.println( httpRequest.method().name() );

            URI uri = new URI( httpRequest.uri() );

            if ( "/favicon.ico".equals( uri.getPath() ) ) {

            }

            ByteBuf content = Unpooled.copiedBuffer( "Hello world", CharsetUtil.UTF_8 );
            FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content );
            response.headers().set( HttpHeaderNames.CONTENT_TYPE, "text/plain" );
            response.headers().set( HttpHeaderNames.CONTENT_LENGTH, content.readableBytes() );

            ctx.writeAndFlush( response );
            //ctx.channel().close();
        }
    }
}
