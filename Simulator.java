
import java.io.*;
import java.util.*;

public class Simulator {

	static String runName = " population based load";

// sizes

	static int maxPath = 9;
	static int maxLevel = 3;
	static int maxOrder = 9;
	static int maxDelay = 255;
    static double maxClock = 300.0;

// xmit scheduling parameters

	static double waitTime = 0.50;
	static double xmitTime = 0.13;

// node buffer limit

	static int queueLimit = 2000;

// load specifications

	static double lastArrival = 240.0;
	static double interArrival = 30.0;
	static double arrivalTime = 2.0;
	static boolean dynamic = true;	// load dependent routing
	static boolean linked = false;	// trans-con link
	static boolean popBased = true;	// population based routing
	static boolean plotting = false;	// message plot

// major state

	static double clock = 0.0;				// simulated time
	static int meanPop = 0;	// mean population
	static int totalPop = 0;				// total population

	static int eventCount = 0;				// sample statistics
	static int msgCount = 0;
	static int updateCount = 0;

// tracing

    static class Trace {
        double time;
        String label;
        Object a, b;

        public String toString() {
            return
                td(w(time, 8)) +
                td(label) +
                td(a.toString()) +
                td(b.toString());
        }

        String td (String d) {
            return "<td valign=top>" + d + "</td>";
        }
    }

    static Trace traces[] = new Trace [200];
    static int nextTrace = 0;

    static void saveTrace() {
        PrintStream out;
        double clock=0;
        int tick=0;
        try {out = new PrintStream(new FileOutputStream("output/trace.html"));}
        catch (FileNotFoundException e) {out=System.out;}
        out.println("<table CELLSPACING=0 CELLPADDING=2>");
        for (int i=nextTrace; ((i+1)%traces.length)!=nextTrace; i=(i+1)%traces.length) {
            Trace t = traces[i];
            if (t==null) break;
            if (t.time != clock) {
                clock = t.time;
                tick++;
            }
            String color = tick%2==0 ? "#cfffcf" : "#afffaf";
            out.println("<tr bgcolor=" + color + ">" + t + "</tr>");
        }
        out.println("</table>");
    }

    static void trace (String label, Object a, Object b) {
        Trace newTrace = traces[nextTrace];
        if (newTrace == null) {
            newTrace = traces[nextTrace] = new Trace();
        }
        nextTrace=(nextTrace+1)%traces.length;
        newTrace.time = clock;
        newTrace.label = label;
        newTrace.a = a;
        newTrace.b = b;
    }

    static void trace (String label, Object a) {
        trace(label, a, "");
    }
    static void trace (String label) {
        trace(label, "", "");
    }


// event queue

	static EventBlock thisEvent;		// event queue (holds blocks)
	static SortedMap eventQueue = new TreeMap();

    static void queue (EventBlock entry) {
		trace("queue", entry);
		double fuzz = rand.nextDouble()/1000.0;	// avoid collisions
		eventQueue.put(new Double(entry.time + fuzz), entry);
	}

	static EventBlock dequeue() {
		Double key = (Double)(eventQueue.firstKey());
		return (EventBlock)(eventQueue.remove(key));
	}

// reports

    static List reports = new LinkedList();

    void report() {
        for (Iterator iterator = reports.iterator(); iterator.hasNext();) {
            Simulator s = (Simulator) iterator.next();
            s.report();
        }
    }

    static InputStream in;
    static PrintStream out;
	static PrintStream plotfil;		// trace output for plot
	static Station station[];					// all stations

	static Graph retrys = new Graph("retransmissions", 2.0);
	static Graph queueTime = new Graph("queueing delay", 5.0);
	static Graph transitTime = new Graph("transit time", 50.0);
	static Graph hops = new Graph("hop count", 50);

	static Scatter queueing = new Scatter("elapsed time", maxClock, "message queue length", 100.0);
	static Scatter backlog = new Scatter("elapsed time", maxClock, "messages in transit", 300.0);
	static Scatter routing = new Scatter("sample time", maxClock, "routing broadcasts", 15.0);
	static Scatter delivery = new Scatter("start time", maxClock, "transit time", 50.0);

    static PrintStream output(String name) {
        String path = "output/" + name + ".txt";
        PrintStream out;
        try {out = new PrintStream(new FileOutputStream(path));}
        catch (FileNotFoundException e) {throw new Error("can't open " + path);}
        return out;
    }



// utilities

    static Random rand = new Random();

	static double uniform () {
		return rand.nextDouble();
	}

	static double exponential () {
		return - Math.log(uniform());
	}

	static int limit (double r, int max) {
		int i = (int)Math.round(r);
		return i<0 ? 0 : i>=max ? max-1 : i;
	}

	static String w(String s, int w) {
		return (s+"                       ").substring(0, w-1)+" ";
	}

	static String w(double d, int w) {
		return w(Double.toString(d), w);
	}

	static String w(int i, int w) {
		return w(Integer.toString(i), w);
	}
}