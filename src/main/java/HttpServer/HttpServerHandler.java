package HttpServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    private static final Pattern INSECURE_URI = Pattern.compile( ".*[<>&\"].*" );
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile( "[^-\\._]?[^<>&\\\"]*" );
    private FullHttpRequest request;

    private static String sanitizeUri ( String uri ) {
        // Decode the path.
        try {
            uri = URLDecoder.decode( uri, "UTF-8" );
        } catch ( UnsupportedEncodingException e ) {
            throw new Error( e );
        }

        if ( uri.isEmpty() || uri.charAt( 0 ) != '/' ) {
            return null;
        }

        // Convert file separators.
        uri = uri.replace( '/', File.separatorChar );

        // Simplistic dumb security check.
        // You will have to do something serious in the production environment.
        if ( uri.contains( File.separator + '.' ) ||
                uri.contains( '.' + File.separator ) ||
                uri.charAt( 0 ) == '.' || uri.charAt( uri.length() - 1 ) == '.' ||
                INSECURE_URI.matcher( uri ).matches() ) {
            return null;
        }

        // Convert to absolute path.
        return SystemPropertyUtil.get( "user.dir" ) + uri;
    }

    private static void setDateHeader ( FullHttpResponse response ) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat( HTTP_DATE_FORMAT, Locale.US );
        dateFormatter.setTimeZone( TimeZone.getTimeZone( HTTP_DATE_GMT_TIMEZONE ) );

        Calendar time = new GregorianCalendar();
        response.headers().set( HttpHeaderNames.DATE, dateFormatter.format( time.getTime() ) );
    }

    private static void setContentTypeHeader ( HttpResponse response, File file ) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        mimeTypesMap.addMimeTypes( "text/css css CSS" );
        mimeTypesMap.addMimeTypes( "text/javascript js JS map" );
        System.out.println( file.getPath() );
        response.headers().set( HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType( file.getPath() ) );
    }

    private static void setDateAndCacheHeaders ( HttpResponse response, File fileToCache ) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat( HTTP_DATE_FORMAT, Locale.US );
        dateFormatter.setTimeZone( TimeZone.getTimeZone( HTTP_DATE_GMT_TIMEZONE ) );

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set( HttpHeaderNames.DATE, dateFormatter.format( time.getTime() ) );

        // Add cache headers
        time.add( Calendar.SECOND, HTTP_CACHE_SECONDS );
        response.headers().set( HttpHeaderNames.EXPIRES, dateFormatter.format( time.getTime() ) );
        response.headers().set( HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS );
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format( new Date( fileToCache.lastModified() ) ) );
    }

    @Override
    protected void channelRead0 ( ChannelHandlerContext ctx, FullHttpRequest request ) throws Exception {
        this.request = request;
        if ( !request.decoderResult().isSuccess() ) {
            sendError( ctx, BAD_REQUEST );
            return;
        }

        if ( GET.equals( request.method() ) ) {
            handleGet( ctx, request );
        } else if ( POST.equals( request.method() ) ) {
            handlePost( ctx, request );
        }
    }

    private void handlePost ( ChannelHandlerContext ctx, FullHttpRequest request ) {
        final boolean keepAlive = HttpUtil.isKeepAlive( request );
        final String uri = request.uri();
        final String path = sanitizeUri( uri );

        if ( path == null ) {
            this.sendError( ctx, FORBIDDEN );
            return;
        }

    }

    private void handleGet ( ChannelHandlerContext ctx, FullHttpRequest request ) throws ParseException, IOException {
        final boolean keepAlive = HttpUtil.isKeepAlive( request );
        final String uri = request.uri();
        final String path = sanitizeUri( uri );

        //System.out.println( "check path: " + path );
        if ( path == null ) {
            this.sendError( ctx, FORBIDDEN );
            return;
        }

        File file = new File( path );
        if ( file.isHidden() || !file.exists() ) {
            this.sendError( ctx, NOT_FOUND );
            return;
        }

        if ( file.isDirectory() ) {
            if ( uri.endsWith( "/" ) ) {
                this.sendListing( ctx, file, uri );
            } else {
                this.sendRedirect( ctx, uri + '/' );
            }
            return;
        }

        if ( !file.isFile() ) {
            sendError( ctx, FORBIDDEN );

            return;
        }

        String ifModifiedSince = request.headers().get( HttpHeaderNames.IF_MODIFIED_SINCE );
        if ( ifModifiedSince != null && !ifModifiedSince.isEmpty() ) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat( HTTP_DATE_FORMAT, Locale.US );
            Date ifModifiedSinceDate = dateFormatter.parse( ifModifiedSince );

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if ( ifModifiedSinceDateSeconds == fileLastModifiedSeconds ) {
                this.sendNotModified( ctx );
                return;
            }
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile( file, "r" );
            System.out.println( "here" );
        } catch ( FileNotFoundException ignore ) {
            sendError( ctx, NOT_FOUND );
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse( HTTP_1_1, OK );
        HttpUtil.setContentLength( response, fileLength );
        setContentTypeHeader( response, file );
        setDateAndCacheHeaders( response, file );

        if ( !keepAlive ) {
            response.headers().set( HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE );
        } else if ( request.protocolVersion().equals( HTTP_1_0 ) ) {
            response.headers().set( HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE );
        }

        ctx.write( response );

        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if ( ctx.pipeline().get( SslHandler.class ) == null ) {
            sendFileFuture =
                    ctx.write( new DefaultFileRegion( raf.getChannel(), 0, fileLength ), ctx.newProgressivePromise() );
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush( LastHttpContent.EMPTY_LAST_CONTENT );
        } else {
            sendFileFuture =
                    ctx.writeAndFlush( new HttpChunkedInput( new ChunkedFile( raf, 0, fileLength, 8192 ) ),
                            ctx.newProgressivePromise() );
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }

        sendFileFuture.addListener( new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed ( ChannelProgressiveFuture future, long progress, long total ) {
                if ( total < 0 ) { // total unknown
                    System.err.println( future.channel() + " Transfer progress: " + progress );
                } else {
                    System.err.println( future.channel() + " Transfer progress: " + progress + " / " + total );
                }
            }

            @Override
            public void operationComplete ( ChannelProgressiveFuture future ) {
                System.err.println( future.channel() + " Transfer complete." );
            }

        } );

        if ( !keepAlive ) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener( ChannelFutureListener.CLOSE );
        }
    }

    @Override
    public void exceptionCaught ( ChannelHandlerContext ctx, Throwable cause ) {
        cause.printStackTrace();
        if ( ctx.channel().isActive() ) {
            sendError( ctx, INTERNAL_SERVER_ERROR );
        }
    }

    private void sendListing ( ChannelHandlerContext ctx, File dir, String dirPath ) {
        FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, OK );
        response.headers().set( HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8" );

        StringBuilder buf = new StringBuilder()
                .append( "<!DOCTYPE html>\r\n" )
                .append( "<html><head><meta charset='utf-8' /><title>" )
                .append( "Listing of: " )
                .append( dirPath )
                .append( "</title></head><body>\r\n" )

                .append( "<h3>Listing of: " )
                .append( dirPath )
                .append( "</h3>\r\n" )

                .append( "<ul>" )
                .append( "<li><a href=\"../\">..</a></li>\r\n" );

        for ( File f : dir.listFiles() ) {
            if ( f.isHidden() || !f.canRead() ) {
                continue;
            }

            String name = f.getName();
            if ( !ALLOWED_FILE_NAME.matcher( name ).matches() ) {
                continue;
            }

            buf.append( "<li><a href=\"" )
                    .append( name )
                    .append( "\">" )
                    .append( name )
                    .append( "</a></li>\r\n" );
        }

        buf.append( "</ul></body></html>\r\n" );
        ByteBuf buffer = Unpooled.copiedBuffer( buf, CharsetUtil.UTF_8 );
        response.content().writeBytes( buffer );
        buffer.release();

        this.sendAndCleanupConnection( ctx, response );
    }

    private void sendRedirect ( ChannelHandlerContext ctx, String newUri ) {
        FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, FOUND );
        response.headers().set( HttpHeaderNames.LOCATION, newUri );

        this.sendAndCleanupConnection( ctx, response );
    }

    private void sendNotModified ( ChannelHandlerContext ctx ) {
        FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, NOT_MODIFIED );
        setDateHeader( response );

        this.sendAndCleanupConnection( ctx, response );
    }

    private void sendError ( ChannelHandlerContext ctx, HttpResponseStatus status ) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer( "Failure: " + status + "\r\n", CharsetUtil.UTF_8 ) );
        response.headers().set( HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8" );

        this.sendAndCleanupConnection( ctx, response );
    }

    private void sendAndCleanupConnection ( ChannelHandlerContext ctx, FullHttpResponse response ) {
        final FullHttpRequest request = this.request;
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

//    private String getContent ( String url ) {
//
//        if ( url.equals( "/favicon.ico" ) ) {
//            return "";
//        }
//
//        try {
//            System.out.println( url );
//            BufferedReader buffer = new BufferedReader( new InputStreamReader( new FileInputStream( System.getProperty( "user.dir" ) + "/src/main" + url ), "UTF-8" ) );
//            StringBuilder target = new StringBuilder();
//            String line;
//            while ( ( line = buffer.readLine() ) != null ) {
//                target.append( line );
//            }
//            return target.toString();
//        } catch ( IOException e ) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    private FullHttpResponse getResponse ( String content, HttpResponseStatus status, String contentType ) {
//        ByteBuf target = Unpooled.copiedBuffer( content, CharsetUtil.UTF_8 );
//        FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, status, target );
//        response.headers().set( HttpHeaderNames.CONTENT_TYPE, contentType );
//
//        //System.out.println( "length: " + target.readableBytes() );
//        response.headers().set( HttpHeaderNames.CONTENT_LENGTH, target.readableBytes() );
//
//        return response;
//    }
//
//    private String getContentType ( String url ) {
//        String type = url.substring( url.lastIndexOf( "." ) );
//
//        System.out.println( "type is: " + type );
//
//        if ( type.equals( ".css" ) ) {
//            return "text/css";
//        } else if ( type.equals( ".js" ) ) {
//            return "application/x-javascript";
//        } else if ( type.equals( ".html" ) ) {
//            return "text/html";
//        } else if ( type.equals( ".ico" ) ) {
//            return "image/png";
//        } else {
//            return "text/plain";
//        }
//    }


}
