import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;


/**
 * Created by maanadev on 5/18/17.
 */
public class ServerHandlersInit extends ChannelInitializer<SocketChannel> {


    public ServerHandlersInit() {

    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {

        SslHandler sslHandler = SSLHandlerProvider.getSSLHandler();

        socketChannel.pipeline().addLast(
                sslHandler,
                new HttpServerCodec(),
                new HttpObjectAggregator(1048576),
                new SimpleChannelInboundHandler<FullHttpRequest>() {

                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
                        channelHandlerContext.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                        channelHandlerContext.channel().close();
                    }
                });
    }

}