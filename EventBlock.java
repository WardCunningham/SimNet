
public class EventBlock extends Simulator {

// kinds of events

    final static int arrivalEvent = 0;
	final static int xmitEvent = 1;
	final static int sampleEvent = 2;
	final static int updateEvent = 3;
	final static String[] eventNames = {"arrival", "xmit", "sample", "update"};

// instance variables

    double time;	// time of event
    int type;		// kind of event
    int node;		// object of event

    EventBlock (double clock, int node) {
        this.time = clock;
        this.node = node;
    }

    public String toString() {
        String result = eventNames[type]+"("+w(time,8);
        if (node>=0) result += " "+station[node];
        return result+")";
    }

    static EventBlock newUpdate(double clock, int nodeNum) {
        EventBlock e = new EventBlock(clock, nodeNum);
        e.type = updateEvent;
        return e;
    }

    static EventBlock newXmit(double clock, int nodeNum) {
        EventBlock e = new EventBlock(clock, nodeNum);
        e.type = xmitEvent;
        return e;
    }

    static EventBlock newArrival(double clock, int nodeNum) {
        EventBlock e = new EventBlock(clock, nodeNum);
        e.type = arrivalEvent;
        return e;
    }

    static EventBlock newSample(double clock) {
        EventBlock e = new EventBlock(clock, -1);
        e.type = sampleEvent;
        return e;
    }

    void dispatch () {
         switch (type) {
            case xmitEvent: {
                station[node].doXmit();
                break;
            }
            case arrivalEvent: {
                station[node].doArrival();
                break;
            }
            case sampleEvent: {
                doSample();
                break;
            }
            case updateEvent: {
                station[node].doUpdate();
                break;
            }
        }
    }

    void doSample () {
		double interval = 1.0;
		routing.mark(clock, 60/interval * updateCount/station.length);
		backlog.mark(clock, msgCount);
        trace("sample", "events: " + eventCount + " updates: " + updateCount + " messages: " + msgCount);
		eventCount = 0;
		updateCount = 0;
		if (clock < lastArrival || msgCount > 0) {
			queue(newSample(clock+interval));
		}
	}

}
