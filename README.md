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

for example
```
cp sse-demo_2.10-0.1-SNAPSHOT.war jetty/webapps/sse.war
```
* start application 

```
 java -jar start.jar jetty.port=8081
 
```

* Build Project

 We use Buils.scala file with xsbt-web-plugin 
 you can create a war file using package command
 ```
 ᚛ ~/projects/sse-demo
᚛ |master #|$ sbt package
Loading /usr/local/Cellar/sbt/0.13.1/bin/sbt-launch-lib.bash
[info] Loading project definition from /Users/chester/projects/html5-sse/project
[info] Set current project to sse-demo (in build file:/Users/chester/projects/html5-sse/)
[info] Updating {file:/Users/chester/projects/html5-sse/}sse-demo...
[info] Resolving org.fusesource.jansi#jansi;1.4 ...
[info] Done updating.
[info] Compiling 2 Scala sources to /Users/chester/projects/html5-sse/target/scala-2.10/classes...
[info] Packaging /Users/chester/projects/html5-sse/target/scala-2.10/sse-demo_2.10-0.1-SNAPSHOT.war ...
[info] Done packaging.
[success] Total time: 10 s, completed Nov 7, 2014 7:59:19 AM
᚛ ~/projects/sse-demo
᚛ |master #|$ 

 ```
 
 * Setup Spray Boot
 The Boot class is very simple, directly copied from Spray example

```

import akka.actor.{ActorSystem, Props}
import spray.servlet.WebBoot


// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class Boot extends WebBoot {

  // we need an ActorSystem to host our application in
  val system = ActorSystem("example")

  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(Props[DemoServiceActor])

  system.registerOnTermination {
    // put additional cleanup code here
    system.log.info("Application shut down")
  }
}
```
 To associate the Boot Class with the web app, Spray uses TypeSafe Configuration file, defined in 
 
 application.conf
 ```
 akka {
  loglevel = INFO
  event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
}

# check the reference.conf in /spray-servlet/main/resources for all defined settings
spray.servlet {
  boot-class = "example.Boot"
  request-timeout = 6s
}

```
notice the spray.servlet.boot-class point to the example.Boot class. 

* define the Spray Routing

All the spray routing are defined in DemoService the same as Spray sample code, there are a few minus tips that can waste you 
a lot of times if you miss it. 

** since all example is under "sse" context, the route path must relative to that path. 
```
val demoRoute = {
    get {
      path("sse" / "ping") {
          complete("PONG!")
      } 
    }
    
    //....
  }
```
Here notice path("ping") need to be changed to path("sse" / "ping") 

** for Server Sent Event streaming 
  I need to define a text/event-stream media type and registered the type. 
  
  ```
  val `text/event-stream` =  MediaType.custom("text/event-stream")
  MediaTypes.register(`text/event-stream`)
  ```

This type will be used in the following directive
```
  def respondAsEventStream =
    respondWithHeader(`Cache-Control`(`no-cache`)) &
      respondWithHeader(`Connection`("Keep-Alive")) &
      respondWithMediaType(`text/event-stream`)

```
 
 ** Working with static content
 Tim's example uses CSS, jquery and load the index.html file in the resources directory. We need to make sure the contents can be loaded using spray. To load the index.html under resource root www, we can use 
 
 ```
   getFromResource("www/index.html")
   
 ```
  The loading will triggle loading contents under www/css and www/js directories, so we need to define the following routes
  
  ```
        path("sse" / "css" / Segment) { path =>
         getFromResource("www/css/" + path)
        } ~
        path("sse" / "js" / Segment) { path =>
          getFromResource("www/js/" + path)
        }
  ```

  ** defining HTML event source 
  
  in index.html, we define the HTML5 EventSoure, notice here we need also added context "sse/streaming" instead just   "/streaming"
  ```
   var source = new EventSource("/sse/streaming")
  ```
  
  One important magic for HTML5 event source is that the browser will re-connect every 3 seconds or so. so the streaming will be re-trigger to replay again and again and never stop. 

If you application represents progress bar, then this may not be desirable, we need to stop it once it progress is 100%. so we change the java scripts to the following to stop the event source streaming

```
    //<![CDATA[
    var source = new EventSource("/sse/streaming")
    source.onopen = function(){
      $('.bar').css('width', '0%');
    }
    source.onmessage = function(message){
      var n = message.data;
      console.log("message '", n, "'");
      if (n.toString().indexOf("Finish") >=0 )  {
        source.close();
      }

      if(!isNaN(n)){
        $('.bar').css('width', n+'%');
      } 
    }

    //]]>
```
Here we use "Finish" (Finished) to indidcate the end of the progress and call source.close to stop the streaming. 

 ** Server-Sent-Event routing
 With above preparation, we can handle the SSE streaming
 
 ```
        path("sse" / "server-sent-event") {
          getFromResource("www/index.html")
        } ~ path("sse" / "streaming") {
          respondAsEventStream {
            sendSSE
          }
        }
        
   def sendSSE(ctx: RequestContext): Unit =
    actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {
          // we use the successful sending of a chunk as trigger for scheduling the next chunk
          val responseStart = HttpResponse(entity = HttpEntity(`text/event-stream`, "data: start\n\n"))
          log.info(" start chunk response  with 10 iterations")
          ctx.responder ! ChunkedResponseStart(responseStart).withAck(Ok(10))

          def receive = {
            case Ok(0) =>
             log.info(" going to stop it " )
              ctx.responder ! MessageChunk("data: Finished.\n\n")
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)
            case Ok(remaining) =>
              log.info(" got ok remaining " + remaining)
              in(Duration(500, MILLISECONDS)) {
                val nextChunk = MessageChunk("data: " + (10 - remaining)*10 + "\n\n")
                ctx.responder ! nextChunk.withAck(Ok(remaining - 1))
              }

            case ev: Tcp.ConnectionClosed =>
              log.warning("Stopping response streaming due to {}", ev)
          }
        }
      }
    }

 ```
 We use the spray Chunk response example to send the progress bar. 

Reference 
============
1) http://timperrett.com/2012/03/17/html5-sse-with-scala-spray/
2) http://www.html5rocks.com/en/tutorials/eventsource/basics/#toc-reconnection-timeout
