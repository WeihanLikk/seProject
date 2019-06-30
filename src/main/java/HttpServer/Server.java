package HttpServer;

import DataBase.DataBase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.SystemPropertyUtil;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;
import java.sql.SQLException;

public class Server {

    static final boolean SSL = System.getProperty( "ssl" ) != null;
    static final int PORT = Integer.parseInt( System.getProperty( "port", SSL ? "443" : "8888" ) );

    public static void main ( String[] args ) throws InterruptedException, CertificateException, SSLException, SQLException {
        final SslContext sslCtx;
        File certificate = new File( SystemPropertyUtil.get( "user.dir" ) + "/ssl/zjudream.top.cer" ); // this is only for windows, for linux please use zjudream.top_ca.crt
        File key = new File( SystemPropertyUtil.get( "user.dir" ) + "/ssl/zjudream.top.key" );
        if ( SSL ) {
//            SelfSignedCertificate ssc = new SelfSignedCertificate();
//            sslCtx = SslContextBuilder.forServer( ssc.certificate(), ssc.privateKey() )
//                    .sslProvider( SslProvider.JDK ).build();
            sslCtx = SslContextBuilder.forServer( certificate, key ).build();
        } else {
            sslCtx = null;
            System.out.println( "ssl is unavailable" );
        }

        DataBase.loadInfo();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group( bossGroup, workerGroup )
                    .channel( NioServerSocketChannel.class )
                    .handler( new LoggingHandler( LogLevel.INFO ) )
                    .childHandler( new serverInitializer( sslCtx ) );

            Channel ch = b.bind( PORT ).sync().channel();

            System.err.println( "Open your web browser and navigate to " +
                    PORT + '/' );

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
