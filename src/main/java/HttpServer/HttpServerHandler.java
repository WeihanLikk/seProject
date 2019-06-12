package HttpServer;

import Class._Class;
import DataBase.DataBase;
import Homework.Homework;
import Question.Question;
import User.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URLDecoder;
import java.sql.SQLException;
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
    private static final Pattern STUDENT_ID = Pattern.compile( "^[3][0-9]*$" );
    private static final Pattern TEACHER_ID = Pattern.compile( "^[1][0-9]*$" );
    private static final Pattern TA_ID = Pattern.compile( "^[2][0-9]*$" );
    private static final Pattern ADMIN_ID = Pattern.compile( "^[0][0-9]*$" );
    private static final Pattern EQUAL_STR = Pattern.compile( "^.*\\?.*" );
    private static final Pattern CLASS_STR = Pattern.compile( "^.*\\?class=.*" );
    private static HashMap<String, JsonHandler> jsonHandlerHashMap;
    private static DataBase db;
    private static Administrator admin;

    static {
        db = new DataBase();
        admin = new Administrator( (long) 1000, "admin", null, "admin" );

        jsonHandlerHashMap = new HashMap<>();

        jsonHandlerHashMap.put( "/client/json/course/", ( ( request, contents ) -> {

            long userId;

            userId = getUserIdFromCookie( request );

            ArrayList<_Class> arrayList = User.findUser( userId ).getClassArrayList();

            JSONObject jsonObject = new JSONObject( true );
            JSONArray classes = new JSONArray();
            for ( _Class co : arrayList ) {
                classes.add( co.getName() );
            }
            jsonObject.put( "courses", classes );

            return jsonObject.toJSONString();
        } ) );

        jsonHandlerHashMap.put( "/client/json/questions/", ( ( request, contents ) -> Question.allToJsonObject().toJSONString() ) );

        jsonHandlerHashMap.put( "/client/json/homework/generate/", ( request, contents ) -> {

            Homework homework = new Homework( Homework.geneId(), "test name" ); // 1 for hw name

            for ( int i = 0; i < contents.length; i++ ) {

                //System.out.println( "fuck check: "+Question.getFuckID() );

                Question question = Question.getQuestion( Long.parseLong( contents[ i ] ) );
                if ( question != null ) {
                    homework.addQuestion( question );
                } else {
                    System.out.println( "Question id: " + contents[ i ] + " not exist!" );
                }
            }

            long userId = getUserIdFromCookie( request );
            User user = User.findUser( userId );
            String tableType = "fuck";
            if ( TEACHER_ID.matcher( String.valueOf( userId ) ).matches() ) {
                tableType = "TEACLASS";
            } else if ( TA_ID.matcher( String.valueOf( userId ) ).matches() ) {
                tableType = "TACLASS";
            }

            //System.out.println( user.getId() + " " + user.getClassPosition() + " " + tableType );

            long classId = DataBase.selectClassIdByUserAndClassName( user.getId(), user.getClassPosition(), tableType );

            //System.out.println( "Classid: " + classId );

            _Class.get_Class( classId ).addHomeWork( homework ); // 0 for class id

            homework.setTotalMarks();
            Homework.addHomework( homework );

            return homework.toJsonObject().toJSONString();
        } );

        jsonHandlerHashMap.put( "/client/json/homework/list/", ( request, contents ) ->
                _Class.get_Class( Long.parseLong( contents[ 0 ] ) ).toHomeworkList().toJSONString() );

        jsonHandlerHashMap.put( "/client/json/courseDesc/", ( ( request, contents ) -> {

            long userId = getUserIdFromCookie( request );

            User user = User.findUser( userId );

            String tableType = "";
            if ( STUDENT_ID.matcher( String.valueOf( userId ) ).matches() ) {
                tableType = "STUCLASS";
            } else if ( TEACHER_ID.matcher( String.valueOf( userId ) ).matches() ) {
                tableType = "TEACLASS";
            } else if ( TA_ID.matcher( String.valueOf( userId ) ).matches() ) {
                tableType = "TACLASS";
            }


            long classId = DataBase.selectClassIdByUserAndClassName( user.getId(), user.getClassPosition(), tableType );

            JSONObject classDesc = new JSONObject( true );
            classDesc.put( "description", _Class.get_Class( classId ).getDescription() );

            System.out.println( classDesc.toJSONString() );


            return classDesc.toJSONString();
        } ) );

    }

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
        //System.out.println( file.getPath() );
        //System.out.println( mimeTypesMap.getContentType( file.getPath() ) );
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

    private static long getUserIdFromCookie ( FullHttpRequest request ) {
        long userId = 0;
        String cookieString = request.headers().get( HttpHeaderNames.COOKIE );
        if ( cookieString != null ) {
            Set<io.netty.handler.codec.http.cookie.Cookie> cookies = ServerCookieDecoder.STRICT.decode( cookieString );
            if ( !cookies.isEmpty() ) {
                // Reset the cookies if necessary.
                for ( Cookie cookie : cookies ) {
                    System.out.println( "Cookie:  " + cookie );
                    userId = Long.parseLong( cookie.value() );
                    //response.headers().add( HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode( cookie ) );
                }
            } else {
                System.out.println( "cookie is fycj" );
            }
        } else {
            System.out.println( "cookie is null" );
        }

        return userId;
    }

    private String[] getPostInfo ( String content ) {
        String[] contents = URLDecoder.decode( content ).split( "&" );

        for ( int i = 0; i < contents.length; i++ ) {
            contents[ i ] = contents[ i ].substring( contents[ i ].lastIndexOf( "=" ) + 1 );
        }

        return contents;
    }

    @Override
    public void channelRead0 ( ChannelHandlerContext ctx, FullHttpRequest request ) throws ParseException, SQLException, IOException {
        this.request = request;
        if ( !request.decoderResult().isSuccess() ) {
            sendError( ctx, BAD_REQUEST );
            return;
        }
        HttpResponse response = new DefaultHttpResponse( HTTP_1_1, OK );

        if ( GET.equals( request.method() ) ) {
            handleGet( ctx, request, response );
        } else if ( POST.equals( request.method() ) ) {
            handlePost( ctx, request, response );
        }
    }

    private void handlePost ( ChannelHandlerContext ctx, FullHttpRequest request, HttpResponse response ) throws IOException, ParseException, SQLException {

        String content = request.content().toString( CharsetUtil.UTF_8 );//.split( "&" );

        String signin = "/client/html/signin/index.html";
        String signup = "/client/html/signup/index.html";
        String stumain = "/client/html/student/stumain/index.html";
        String teamain = "/client/html/teacher/teamain/index.html";
        //String tamain = "/client/html/teacher/teamain/index.html";
//        String stuhw = "/client/json/homework/generate/";
//        String stuhwList = "/client/json/homework/list/";
//        String stuhwJudge = "/client/json/homework/summarize/";


        String uri = request.uri();
        if ( uri.equals( signin ) ) {
            if ( userRegister( getPostInfo( content ) ) == -1 ) {
                this.sendRedirect( ctx, signup );
                return;
            }
        } else if ( uri.equals( stumain ) || uri.equals( teamain ) ) {
            if ( userLogin( getPostInfo( content ), response, request ) == -1 ) {
                this.sendRedirect( ctx, signin );
                return;
            }
        }

        handleGet( ctx, request, response );
    }

    private void handleGet ( ChannelHandlerContext ctx, FullHttpRequest request, HttpResponse response ) throws ParseException, IOException, SQLException {

        final boolean keepAlive = HttpUtil.isKeepAlive( request );
        final String uri = request.uri();

        String path = sanitizeUri( uri );
        String classTarget;

        if ( CLASS_STR.matcher( path ).matches() ) {
            classTarget = path.substring( path.lastIndexOf( "=" ) + 1 );

            long userId = getUserIdFromCookie( request );

            User tea = User.findUser( userId );
            tea.setClassPosition( classTarget );

            path = path.substring( 0, path.lastIndexOf( "?" ) );
        } else if ( EQUAL_STR.matcher( path ).matches() ) {
            path = path.substring( 0, path.lastIndexOf( "?" ) );
        }

        //System.out.println( path );

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
                this.sendJson( ctx, uri, request );
                //this.sendListing( ctx, file, uri );
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
        } catch ( FileNotFoundException ignore ) {
            sendError( ctx, NOT_FOUND );
            return;
        }
        long fileLength = raf.length();

        //HttpResponse response = new DefaultHttpResponse( HTTP_1_1, OK );
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

    private int userRegister ( String[] contents ) throws SQLException {
        // TO DO judge the type of user
        String id = contents[ 0 ];

        if ( STUDENT_ID.matcher( id ).matches() ) {
            Student stu = new Student( Long.parseLong( id ), "", contents[ 1 ], contents[ 2 ] );

            //System.out.println( "id: " + id + " email: " + contents[ 1 ] + " P: " + contents[ 2 ] );
            if ( db.insertUserInfo( stu ) == -1 ) {
                return -1;
            }
            Student.addUser( stu );
        } else if ( TEACHER_ID.matcher( id ).matches() ) {
            Teacher teacher = new Teacher( Long.parseLong( id ), "", contents[ 1 ], contents[ 2 ] );
            if ( db.insertUserInfo( teacher ) == -1 ) {
                return -1;
            }
            Teacher.addUser( teacher );
        } else if ( TA_ID.matcher( id ).matches() ) {
            TA ta = new TA( Long.parseLong( id ), "", contents[ 1 ], contents[ 2 ] );
            if ( db.insertUserInfo( ta ) == -1 ) {
                return -1;
            }
            TA.addUser( ta );
        } else if ( ADMIN_ID.matcher( id ).matches() ) {

        }
        return 0;
    }

    private int userLogin ( String[] contents, HttpResponse response, FullHttpRequest request ) {
        String id = contents[ 0 ];
        User user;
        user = User.findUser( Long.parseLong( id ) );

        if ( ADMIN_ID.matcher( id ).matches() ) {

        }

        if ( user == null ) {
            return -1; // not find this user
        }
        if ( !user.getPassword().equals( contents[ 1 ] ) ) {
            //System.out.println( "P: " + students.get( 0 ).getPassword() + "con1: " + contents[ 1 ] );
            return -1;
        }
        user.setActive( true );

        String cookieString = request.headers().get( HttpHeaderNames.COOKIE );
        if ( cookieString != null ) {
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode( cookieString );
            if ( !cookies.isEmpty() ) {
                // Reset the cookies if necessary.
                Boolean s = false;
                for ( Cookie cookie : cookies ) {
                    if ( cookie.name().equals( "userKey" ) ) {
                        System.out.println( "In login" );
                        cookie.setPath( "/" );
                        cookie.setDomain( "localhost" );
                        System.out.println( "fuck value: " + user.getId() );
                        cookie.setValue( user.getId().toString() );
                        //response.headers().remove( HttpHeaderNames.COOKIE );
                        response.headers().add( HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode( cookie ) );
                    }
                }
            }
        } else {
            // Browser sent no cookie.  Add some.
            Cookie cookie = new DefaultCookie( "userKey", user.getId().toString() );
            System.out.println( "fuck value 2: " + user.getId() );
            cookie.setPath( "/" );
            cookie.setDomain( "localhost" );
            response.headers().add( HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode( cookie ) );
            //response.headers().add( HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode( "key2", "value2" ) );
        }
        return 0;
    }


    private void sendJson ( ChannelHandlerContext ctx, String uri, FullHttpRequest request ) throws SQLException {
        System.out.println( "check in sendJson: " + uri );

        String[] contents = getPostInfo( request.content().toString( CharsetUtil.UTF_8 ) );

        if ( jsonHandlerHashMap.containsKey( uri ) ) {
            String json = jsonHandlerHashMap.get( uri ).jsonHandler( request, contents );
            FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, OK );
            response.headers().set( HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8" );
            ByteBuf buffer = Unpooled.copiedBuffer( json, CharsetUtil.UTF_8 );
            response.content().writeBytes( buffer );
            buffer.release();

            this.sendAndCleanupConnection( ctx, response );
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
}