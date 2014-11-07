sse-demo
========
HTML5 server-sent-event (event source) with spray. 

This project tries to re-make the Tim Perrett's orginal post 2 years ago (see #1) about using spray with HTM5 event source to streaming data to the web page. 
Since the original code was written with earlier version of spray, it's quite hard to get the code function again. 

In this demo, I recreated the project with recent version of Spray, but with original HTML5 page from Tim's post with some miner changes ( describe later) 

The spray code is mostly copied from Spray project's sample code. I changed the code to make the SSE work. 

Lessens and Tips
=================

First, I am using Jetty as the HTTP server /Servlet contiainer. I choose Jetty ( similar with Tomcat) instead of SprayCan, as many applications are still on Tomcat or Jetty, I wants to demo that functionality can be easily applied with spray. This allows mixed usage of Java and Scala. 

Next, I choose to use Spray Routing for the higher level abstraction. 

With Servelet container, the spray routing examples code in the spray project need to be modified. 














Reference 
============
1) http://timperrett.com/2012/03/17/html5-sse-with-scala-spray/
2) http://www.html5rocks.com/en/tutorials/eventsource/basics/#toc-reconnection-timeout
