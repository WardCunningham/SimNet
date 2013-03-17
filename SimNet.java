
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.awt.*;

public class SimNet extends Simulator implements Runnable {


// construction

	SimNet () {
		station = new Station [173];
		for (int i=0; i<station.length; i++) {
			station[i] = new Station();
			station[i].nodeNum = i;
		}
	}

    static int ny = 10-1;		// new york
    static int sf = 169-1;		// san francisco

	void readInput() {

		totalPop = 0;
		for (int i=0; i<173; i++) {
			station[i].readInput();
            totalPop += station[i].population;
		}
		meanPop = totalPop / station.length;
		if (linked) {
			station[ny].addPath(sf);
			station[sf].addPath(ny);
		}
	}


// simulation

	void initialArrivals () {
		for (int i=0; i<station.length; i++) {
			EventBlock e = EventBlock.newArrival(clock + station[i].arrivalWait(), i);
			if (e.time <= lastArrival) {
				queue(e);
			} else {
				trace("no arrival", station[i]);
			}
		}
	}

	void initialSample () {
		EventBlock s = EventBlock.newSample(clock);
		queue(s);
	}

	void simulate(double period) {
        double endTime = clock + period;
		while (!eventQueue.isEmpty() && queueTime()<endTime){
            dispatch();
        }
        clock = endTime;
	}

    void dispatch() {
        if (eventQueue.isEmpty()) return;
        thisEvent = dequeue();
        clock = thisEvent.time;
        trace("dispatch", thisEvent);
        thisEvent.dispatch();
        eventCount++;
    }

    public void run() {
        try {
            Thread.sleep(2000);
            while (true) {
                simulate(0.02);     // minutes
                Thread.sleep(50);   // milliseconds
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }


// reporting

	void report () {
        PrintStream out = output("summary");
		Scatter throughput = new Scatter("message count", 500, "radio utilization", 100);
		out.println(runName);
		out.println();
		out.println(clock + " minutes simulated");
		out.println(lastArrival + " last arrival");
		out.println(interArrival + " mean inter-arrival period");
		out.println(arrivalTime + " minimum inter-arrival period");
		out.println(queueLimit + " buffer limit");
		out.println(dynamic + " dynamic routing");
		out.println(linked + " trans-con data link");
		out.println(popBased + " population based (" + totalPop + ") load");
        out.println(transitTime.count + " messages delivered");
		out.println();
		out.println();
		out.println(" message counts and utilizations");
		for (int i=0; i<station.length; i++) {
			if (i%5 == 0) {
				out.println();
			}
			out.print(
				w(station[i].useCount, 4) + " " +
				w(station[i].useTime/clock*100.0, 5) + " " +
				w(station[i].city, 14));
			if(transitTime.count!=0) {
				throughput.mark(
					station[i].useCount,
					station[i].useTime/clock*100.0);
			}
		}
		out.println();
	}

// main

    void setup() {
        out = System.out;
        try {in = new FileInputStream("data.txt");}
        catch (FileNotFoundException e) {throw new Error("data.txt not found");}
        readInput();
        initialSample();
        initialArrivals();
    }

    void finish() {
        while(!eventQueue.isEmpty()) {
			thisEvent = dequeue();
			trace("leftover", thisEvent);
		}
		report();
        super.report();
        printTrace();
    }

	void main () {
        setup();
		simulate(maxClock);
        finish();
	}

	public static void main (String[] argv) {
		(new SimNet()).main();
	}
}

