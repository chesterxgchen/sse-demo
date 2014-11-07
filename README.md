sse-demo
========
HTML5 server-sent-event (event source) with spray. 

This project tries to re-make the Tim Perrett's orginal post 2 years ago (see #1) about using spray with HTM5 event source to streaming data to the web page. 
Since the original code was written with earlier version of spray, it's quite hard to get the code function again. 

In this demo, I recreated the project with recent version of Spray, but with original HTML5 page from Tim's post with some miner changes ( describe later) 

The spray code is mostly copied from Spray project's sample code. I changed the code to make the SSE work. 

About the demo
=================

First, I am using Jetty as the HTTP server /Servlet contiainer. I choose Jetty ( similar with Tomcat) instead of SprayCan, as many applications are still on Tomcat or Jetty, I wants to demo that functionality can be easily applied with spray. This allows mixed usage of Java and Scala. 

Next, I choose to use Spray Routing for the higher level abstraction. 

With Servelet container, the spray routing examples code in the spray project need to be modified. 

* Integration with Jetty

 

```
   web.xml

    <display-name>sse</display-name>

    <listener>
        <listener-class>spray.servlet.Initializer</listener-class>
    </listener>

    <servlet>
        <servlet-name>SprayConnectorServlet</servlet-name>
        <servlet-class>spray.servlet.Servlet30ConnectorServlet</servlet-class>
        <async-supported>true</async-supported>
    </servlet>

    <servlet-mapping>
        <servlet-name>SprayConnectorServlet</servlet-name>
        <url-pattern>/*</url-pattern>
    </servlet-mapping>
```

Here we simply add spray provided listner to the web.xml and redirect all the traffic to spray with servlet-mapping. Note that not "ALL" http traffic will to spray, this only applies to the traffic within this servlet context. 

The default servlet context will be defined by your .war file name. We will name our applicaton war file as sse.war, so our application context will be "sse". 

all traffic in /sse/* will be go to the spray servelet. 

* deploy the war file

To deploy the application, simply copy the sse.war to the jetty/webapps/sse.war 

* start application 

```
 java -jar start.jar jetty.port=8081
 
```

















Reference 
============
1) http://timperrett.com/2012/03/17/html5-sse-with-scala-spray/
2) http://www.html5rocks.com/en/tutorials/eventsource/basics/#toc-reconnection-timeout
