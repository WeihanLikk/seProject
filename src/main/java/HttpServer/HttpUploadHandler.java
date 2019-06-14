package HttpServer;

import FileManger.FileManger;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpUploadHandler extends SimpleChannelInboundHandler<HttpObject> {


    private static final HttpDataFactory factory = new DefaultHttpDataFactory( true );
    private static final String FILE_UPLOAD = "\\client\\resources\\files\\";
    private static final String URI = "/upload/fileUpload";
    HttpRequest request;
    private HttpPostRequestDecoder httpDecoder;

    @Override
    protected void channelRead0 ( final ChannelHandlerContext ctx, final HttpObject httpObject )
            throws Exception {
        if ( httpObject instanceof HttpRequest ) {
            request = (HttpRequest) httpObject;


            if ( request.uri().startsWith( URI ) && request.method().equals( HttpMethod.POST ) ) {
                //System.out.println( "in upload " + request.uri() );
                httpDecoder = new HttpPostRequestDecoder( factory, request );
                httpDecoder.setDiscardThreshold( 0 );
            } else {
                //传递给下一个Handler
                ctx.fireChannelRead( httpObject );
            }
        }
        if ( httpObject instanceof HttpContent ) {
            if ( httpDecoder != null ) {
                //System.out.println( "here in content" );
                final HttpContent chunk = (HttpContent) httpObject;
                httpDecoder.offer( chunk );
                if ( chunk instanceof LastHttpContent ) {
                    //System.out.println( "here in lastContent" );
                    writeChunk( ctx );
                    //关闭httpDecoder
                    httpDecoder.destroy();
                    httpDecoder = null;
                    //ReferenceCountUtil.release( httpObject );
                }
                //
            } else {
                ctx.fireChannelRead( httpObject );
            }
        }

    }

    private void writeChunk ( ChannelHandlerContext ctx ) throws IOException {
        while ( httpDecoder.hasNext() ) {
            InterfaceHttpData data = httpDecoder.next();
            if ( data != null && InterfaceHttpData.HttpDataType.FileUpload.equals( data.getHttpDataType() ) ) {
                final FileUpload fileUpload = (FileUpload) data;
                //System.out.println( "file path: " + SystemPropertyUtil.get( "user.dir" ) + FILE_UPLOAD + fileUpload.getFilename() );
                final File file = new File( SystemPropertyUtil.get( "user.dir" ) + FILE_UPLOAD + fileUpload.getFilename() );
                FileManger.addFile( fileUpload.getFilename(), FILE_UPLOAD + fileUpload.getFilename() );
                //log.info("upload file: {}", file);
                try ( FileChannel inputChannel = new FileInputStream( fileUpload.getFile() ).getChannel();
                      FileChannel outputChannel = new FileOutputStream( file ).getChannel() ) {
                    outputChannel.transferFrom( inputChannel, 0, inputChannel.size() );
                    //ResponseUtil.response(ctx, request, new GeneralResponse(HttpResponseStatus.OK, "SUCCESS", null));
                }
            }
        }

        this.sendRedirect( ctx, "/client/html/teacher/teafiledownload/index.html" );

    }

    private void sendRedirect ( ChannelHandlerContext ctx, String newUri ) {
        FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, FOUND );
        response.headers().set( HttpHeaderNames.LOCATION, newUri );

        final boolean keepAlive = HttpUtil.isKeepAlive( request );
        HttpUtil.setContentLength( response, response.content().readableBytes() );
        if ( !keepAlive ) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set( HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE );
        } else if ( request.protocolVersion().equals( HTTP_1_0 ) ) {
            response.headers().set( HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE );
        }

        ChannelFuture flushPromise = ctx.writeAndFlush( response );
        if ( !keepAlive ) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener( ChannelFutureListener.CLOSE );
        }
    }


    @Override
    public void exceptionCaught ( ChannelHandlerContext ctx, Throwable cause ) throws Exception {
        //log.warn( "{}", cause );
        //System.out.println( cause );
        ctx.channel().close();
    }

    @Override
    public void channelInactive ( ChannelHandlerContext ctx ) throws Exception {
        if ( httpDecoder != null ) {
            httpDecoder.cleanFiles();
        }
    }

}
