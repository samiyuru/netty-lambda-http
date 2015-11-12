/*
 * Copyright 2013 by Maxim Kalina
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.javaforge.netty.servlet.bridge;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;
import net.javaforge.netty.servlet.bridge.config.WebappConfiguration;
import net.javaforge.netty.servlet.bridge.impl.ServletBridgeWebapp;
import net.javaforge.netty.servlet.bridge.interceptor.ChannelInterceptor;
import net.javaforge.netty.servlet.bridge.interceptor.HttpSessionInterceptor;
import net.javaforge.netty.servlet.bridge.session.DefaultServletBridgeHttpSessionStore;
import net.javaforge.netty.servlet.bridge.session.ServletBridgeHttpSessionStore;

//import io.netty.channel.ChannelPipelineFactory;
//import io.netty.handler.codec.http.HttpChunkAggregator;

//TODO Just fix the compilation error. not right implementation
public class ServletBridgeChannelPipelineFactory extends ChannelInitializer {

    private DefaultEventExecutor eventExecutor = new DefaultEventExecutor();

    private ChannelGroup allChannels = new DefaultChannelGroup(eventExecutor);

    private HttpSessionWatchdog watchdog;

    private final ChannelHandler idleStateHandler;

    private Timer timer;

    public ServletBridgeChannelPipelineFactory(WebappConfiguration config) {

        this.timer = new HashedWheelTimer();
        this.idleStateHandler = new IdleStateHandler(60, 30, 0); // timer
        // must        // be        // shared.
        ServletBridgeWebapp webapp = ServletBridgeWebapp.get();
        webapp.init(config, allChannels);
        new Thread(this.watchdog = new HttpSessionWatchdog()).start();
    }

    public void shutdown() {
        this.watchdog.stopWatching();
        ServletBridgeWebapp.get().destroy();
        this.timer.stop();
        this.allChannels.close().awaitUninterruptibly();
    }

    protected ServletBridgeHttpSessionStore getHttpSessionStore() {
        return new DefaultServletBridgeHttpSessionStore();
    }

    protected ServletBridgeHandler getServletBridgeHandler() {

        ServletBridgeHandler bridge = new ServletBridgeHandler();
        bridge.addInterceptor(new ChannelInterceptor());
        bridge.addInterceptor(new HttpSessionInterceptor(
                getHttpSessionStore()));
        return bridge;
    }

    @Override
    public void initChannel(Channel channel) throws Exception {
        channel.pipeline().addLast("decoder", new HttpRequestDecoder());
        // pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        channel.pipeline().addLast("encoder", new HttpResponseEncoder());
        // Remove the following line if you don't want automatic content compression.
        channel.pipeline().addLast("deflater", new HttpContentCompressor());
        channel.pipeline().addLast("idle", this.idleStateHandler);
        channel.pipeline().addLast("handler", getServletBridgeHandler());
    }

    private class HttpSessionWatchdog implements Runnable {

        private boolean shouldStopWatching = false;

        @Override
        public void run() {

            while (!shouldStopWatching) {
                try {
                    ServletBridgeHttpSessionStore store = getHttpSessionStore();
                    if (store != null) {
                        store.destroyInactiveSessions();
                    }
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public void stopWatching() {
            this.shouldStopWatching = true;
        }

    }
}
