package com.crossoverjie.cim.server.server;

import com.alibaba.fastjson.JSON;
import com.crossoverjie.cim.common.protocol.BaseRequestProto;
import com.crossoverjie.cim.server.util.NettySocketHolder;
import com.crossoverjie.cim.server.vo.req.SendMsgReqVO;
import com.crossoverjie.cim.common.pojo.CustomProtocol;
import com.crossoverjie.cim.server.init.HeartbeatInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 21/05/2018 00:30
 * @since JDK 1.8
 */
@Component
public class HeartBeatServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(HeartBeatServer.class);

    private EventLoopGroup boss = new NioEventLoopGroup();
    private EventLoopGroup work = new NioEventLoopGroup();


    @Value("${netty.server.port}")
    private int nettyPort;


    /**
     * 启动 cim server
     *
     * @return
     * @throws InterruptedException
     */
    @PostConstruct
    public void start() throws InterruptedException {

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(nettyPort))
                //保持长连接
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new HeartbeatInitializer());

        ChannelFuture future = bootstrap.bind().sync();
        if (future.isSuccess()) {
            LOGGER.info("启动 cim server 成功");
        }
    }


    /**
     * 销毁
     */
    @PreDestroy
    public void destroy() {
        boss.shutdownGracefully().syncUninterruptibly();
        work.shutdownGracefully().syncUninterruptibly();
        LOGGER.info("关闭 cim server 成功");
    }


    /**
     * 发送消息
     *
     * @param customProtocol
     */
    public void sendMsg(CustomProtocol customProtocol) {
        NioSocketChannel socketChannel = NettySocketHolder.get(customProtocol.getId());

        if (null == socketChannel) {
            throw new NullPointerException("没有[" + customProtocol.getId() + "]的socketChannel");
        }

        ChannelFuture future = socketChannel.writeAndFlush(Unpooled.copiedBuffer(customProtocol.toString(), CharsetUtil.UTF_8));
        future.addListener((ChannelFutureListener) channelFuture ->
                LOGGER.info("服务端手动发消息成功={}", JSON.toJSONString(customProtocol)));
    }

    /**
     * 发送 Google Protocol 编码消息
     * @param sendMsgReqVO 消息
     */
    public void sendGoogleProtoMsg(SendMsgReqVO sendMsgReqVO){
        NioSocketChannel socketChannel = NettySocketHolder.get(sendMsgReqVO.getId());

        if (null == socketChannel) {
            throw new NullPointerException("没有[" + sendMsgReqVO.getId() + "]的socketChannel");
        }
        BaseRequestProto.RequestProtocol protocol = BaseRequestProto.RequestProtocol.newBuilder()
                .setRequestId((int) sendMsgReqVO.getId())
                .setReqMsg(sendMsgReqVO.getMsg())
                .build();

        ChannelFuture future = socketChannel.writeAndFlush(protocol);
        future.addListener((ChannelFutureListener) channelFuture ->
                LOGGER.info("服务端手动发送 Google Protocol 成功={}", sendMsgReqVO.toString()));
    }
}