SimNet
======

Amateur Radio Network Simulator

I wrote this simulator when I was in college back in 1977.
Home computers were new. CB radio was popular.
Long distance phone calls were expensive.
Electronic mail was only used locally.

I was most interested in how typed messages could be 
routed through a dynamically changing network using
only slow links and limited computers. I chose a 
scheme that works like zip codes: get the message
close based on first digits then dig in deeper.

I wrote a summary of this work for my amateur
radio colleagues. This has been recovered and 
published online:

http://c2.com/~ward/morse/SimNet/

Running SimNet
==============

In 2002 I translated the Pascal program to Java
and added a couple of new visualizations. On my 
mac I run the interactive map view with this command:

  javac SimView.java && java SimView

This displays a resizable basemap with a number of
continuously updated marks:

Red Dots mark each radio station. These are located
in cities largish cities except out west where 
I also included an occasional mountain top.

Black Lines mark reliable radio communication 
pathes. These become bold when the path is in
use carrying a message. One experiment I did was
to connect NY and SF with an extra link and 
see if it could be used.

Red Halos surround radio stations when they 
update their routing tables. They do this a 
lot so expect to see lots of halos.

Yellow Bubbles show stations with a backlog of
messages waiting to be transmitted. More messages
make bigger bubbles.

Drag your mouse between cities to see how messages
will be routed. A Blue Line will show the path
routing tables suggest at any given instant. A
Green Line shows the return path which rarely takes
the reverse route. Green and blue dots mark 
when a route crosses a boundary to areas with 
more specific routing information. A tiny bar graph
shows the estimated remaining delivery time for 
each hop along a route.

Test Points
===========

The SimNet desktop application includes a small
web server that can provide additional diagnostic
information about the simulator. See:

http://c2.com/doc/TestPoint/TestPointInPractice.pdf

Configuration
=============

I configure the simulator with parameters and
feature switches in the Simulator.java module.

I've experimented with an exhaustive all-station
route visualization. Look for a commented out
call on `rivertrace(g, station)` in SimView.java.

