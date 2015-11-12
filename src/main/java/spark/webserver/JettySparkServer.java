/*
 * Copyright 2011- Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spark.webserver;

import io.netty.channel.socket.SocketChannel;
import net.javaforge.netty.servlet.bridge.ServletBridgeChannelPipelineFactory;
import net.javaforge.netty.servlet.bridge.config.WebappConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.kernel.transports.TransportManager;
import org.wso2.carbon.transport.http.netty.internal.NettyTransportDataHolder;
import org.wso2.carbon.transport.http.netty.internal.config.ListenerConfiguration;
import org.wso2.carbon.transport.http.netty.listener.CarbonNettyServerInitializer;
import org.wso2.carbon.transport.http.netty.listener.NettyListener;
import spark.SparkServer;
import spark.ssl.SslStores;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Spark server implementation
 *
 * @author Per Wendel
 */
public class JettySparkServer implements SparkServer {

    private static final int SPARK_DEFAULT_PORT = 4567;
    private static final String NAME = "Spark";
    private TransportManager transportManager = new TransportManager();

    private JettyHandler handler;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public JettySparkServer(JettyHandler handler) {
        this.handler = handler;
        System.setProperty("org.mortbay.log.class", "spark.JettyLogger");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ignite(String host,
                       int port,
                       SslStores sslStores,
                       CountDownLatch latch,
                       int maxThreads,
                       int minThreads,
                       int threadIdleTimeoutMillis,
                       Map<String, Class<?>> webSocketHandlers,
                       Optional<Integer> webSocketIdleTimeoutMillis) {

//        if (port == 0) {
//            try (ServerSocket s = new ServerSocket(0)) {
//                port = s.getLocalPort();
//            } catch (IOException e) {
//                logger.error("Could not get first available port (port set to 0), using default: {}", SPARK_DEFAULT_PORT);
//                port = SPARK_DEFAULT_PORT;
//            }
//        }
//
//        server = JettyServerFactory.createServer(maxThreads, minThreads, threadIdleTimeoutMillis);
//
//        ServerConnector connector;
//
//        if (sslStores == null) {
//            connector = SocketConnectorFactory.createSocketConnector(server, host, port);
//        } else {
//            connector = SocketConnectorFactory.createSecureSocketConnector(server, host, port, sslStores);
//        }
//
//        server = connector.getServer();
//        server.setConnectors(new Connector[] {connector});
//
//        ServletContextHandler webSocketServletContextHandler =
//                WebSocketServletContextHandlerFactory.create(webSocketHandlers, webSocketIdleTimeoutMillis);
//
//        // Handle web socket routes
//        if (webSocketServletContextHandler == null) {
//            server.setHandler(handler);
//        } else {
//            List<Handler> handlersInList = new ArrayList<>();
//            handlersInList.add(handler);
//
//            // WebSocket handler must be the last one
//            if (webSocketServletContextHandler != null) {
//                handlersInList.add(webSocketServletContextHandler);
//            }
//
//            HandlerList handlers = new HandlerList();
//            handlers.setHandlers(handlersInList.toArray(new Handler[handlersInList.size()]));
//            server.setHandler(handlers);
//        }
//
//        try {
//            logger.info("== {} has ignited ...", NAME);
//            logger.info(">> Listening on {}:{}", host, port);
//
//            server.start();
//            latch.countDown();
//            server.join();
//        } catch (Exception e) {
//            logger.error("ignite failed", e);
//            System.exit(100); // NOSONAR
//        }


        NettyTransportDataHolder nettyTransportDataHolder = NettyTransportDataHolder.getInstance();
        ListenerConfiguration listenerConfiguration =
                new ListenerConfiguration("netty-" + port, "0.0.0.0", port);
        NettyListener listener = new NettyListener(listenerConfiguration);
        transportManager.registerTransport(listener);
        nettyTransportDataHolder.
                addNettyChannelInitializer(listenerConfiguration.getId(),
                        new CarbonNettyServerInitializer() {
                            ServletBridgeChannelPipelineFactory servletBridgeChannelPipelineFactory;

                            @Override
                            public void setup(Map<String, String> map) {
                                servletBridgeChannelPipelineFactory =
                                        new ServletBridgeChannelPipelineFactory(new WebappConfiguration()
                                                .addHttpServlet(new HttpServlet() {
                                                    @Override
                                                    protected void doGet(HttpServletRequest req,
                                                                         HttpServletResponse resp)
                                                            throws ServletException, IOException {
                                                        handler.doHandle(null, req, resp);
                                                    }
                                                }));
                            }

                            @Override
                            public void initChannel(SocketChannel socketChannel) {
                                try {
                                    servletBridgeChannelPipelineFactory.initChannel(socketChannel);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
        transportManager.startTransports();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        logger.info(">>> {} shutting down ...", NAME);
        transportManager.stopTransports();
        logger.info("done");
    }


}
