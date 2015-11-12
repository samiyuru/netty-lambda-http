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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple Jetty Handler
 *
 * @author Per Wendel
 */
public class JettyHandler {

    private Filter filter;

    public JettyHandler(Filter filter) {
        this.filter = filter;
    }

    public void doHandle(
            String target,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
        filter.doFilter(wrapper, response, null);

//        if (wrapper.notConsumed()) {
//            baseRequest.setHandled(false);
//        } else {
//            baseRequest.setHandled(true);
//        }

    }

}