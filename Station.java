
import java.io.*;

public class Station extends Simulator {
    int nodeNum;		// ordinal number
    int lastPath = 0;	// connectivity
    int path[] = 		new int [maxPath];				// specific connections
    int zip[] = 		new int [maxLevel];				// this stations routing code
    int route[][] = 	new int [maxLevel][maxOrder];	// routing tables
    int delay[][] = 	new int [maxLevel][maxOrder];
    int promise[][] = 	new int [maxLevel][maxOrder];
    boolean updating;	// update in progress
    Message nextMsg;	// message queue pointers
    Message lastMsg;
    int queueLength;	// number in message queue
    int useCount;		// message count for utilization
    double useTime;		// time using or waiting to use channel
    String city;		// unique city name
    boolean tracing;	// debug output flag
    int population;		// population in thousands
    int	latatude;		// location;
    int longitude;
    int displacement;	// legend tweek for map

    void addPath(int city) {
        path[lastPath++] = city;
    }

    boolean hasMsg() {
        return nextMsg != null;
    }

    public String toString() {
        return "Station("+city+")";
    }

    void readInput() {

        // line 1

        if(nodeNum != readInt()-1) {
            throw new Error("input out of sync");
        }
        city = readString(20);
        for (int i=0; i<maxLevel; i++) {
            zip[i] = readInt()-1;
        }
        population = readInt();
        latatude = readInt();
        longitude = -readInt();
        displacement = readInt();

        // line 2

        for (int i=0; i<maxPath; i++) {
            int p = readInt()-1;
            if (p >= 0) addPath(p);
        }

        // lines 3, 4, 5

        for (int i=0; i<maxLevel; i++) {
            for (int j=0; j<maxOrder; j++) {
                route[i][j] = readInt()-1;
                delay[i][j] = 0;
                promise[i][j] = 0;
            }
        }

        nextMsg = null;
        updating = false;
        queueLength = 0;
        useCount = 0;
        useTime = 0.0;
        tracing = false;
    }


    int readInt () {
		int ch;
		int assembly=0;
		try {
			ch = in.read();
			while(ch==' ' || ch=='\n' || ch=='\r') {
				ch = in.read();
			}
			if (ch == '-') {
				return -readInt();
			}
			while(ch>='0' && ch<='9') {
				assembly = assembly * 10 + (ch - '0');
				ch = in.read();
			}
		} catch (IOException e) {};
		return assembly;
	}

	String readString (int max) {
		int ch, length=0;
		char assembly[] = new char [max];
		try {
			ch = in.read();
			while(ch==' ' || ch=='\n' || ch=='\r') {
				ch = in.read();
			}
			for (int i=0; i<max; i++) {
				assembly[i] = (char)ch;
				if (!(ch==' ' || ch=='\n' || ch=='\r')) {
					length = i+1;
				}
				ch = in.read();
			}
		} catch (IOException e) {};
		return new String(assembly, 0, length);
	}

    void propagate() {
        if (!dynamic) return;
        if (updating) return;
        trace("propagate", this);
        int load = queueLength + 1;
        for (int l=0; l<maxLevel; l++) {
            // look for changes
            for (int z=0; z<maxOrder; z++) {
                double diff = (promise[l][z]-delay[l][z]-load)/(delay[l][z]+20);
                updating |= (Math.abs(diff) > 0.10);
            }
        }
        if (!updating) return;
        queue(EventBlock.newUpdate(clock+0.2, nodeNum));
    }

    void schedule() {
        trace("schedule", this);
        // compute interference
        double success = 1.0;
        for (int i=0; i<lastPath; i++) {
            if (station[path[i]].hasMsg()) {
                success *= waitTime / (xmitTime + waitTime);
            }
        }
        double thisRetries = 1.0 / success - 1.0;
        double totalTime =  xmitTime + thisRetries * (xmitTime+waitTime);
        EventBlock e = EventBlock.newXmit(clock+totalTime, nodeNum);

        // collect stats
        retrys.count(thisRetries);
        queueTime.count(clock - nextMsg.queued);
        useCount++;
        useTime += (e.time - clock);
        queue(e);
    }

    void queueMsg(Message m) {
        m.next = null;
        m.queued = clock;
        queueLength++;
        trace("queue", m);
        queueing.mark (clock, queueLength);
        if (nextMsg == null) {
            nextMsg = m;
            schedule();
        } else {
            lastMsg.next = m;
        }
        lastMsg = m;
        propagate();
    }

    Message dequeueMsg() {
        Message msg = nextMsg;
        nextMsg = msg.next;
        if (nextMsg == null) {
            lastMsg = null;
        }
        queueLength--;
        trace("dequeue", msg);
        queueing.mark(clock, queueLength);
        return msg;
    }

    double arrivalWait () {
        double t;
        if (popBased) {
            t = interArrival * meanPop / population - arrivalTime;
        } else {
            t = interArrival - arrivalTime;
        }
        if (t<0) t = 0;
        return exponential()*t + arrivalTime;
    }

    int nextHop (int[] toZip) {
        int i = 0;
        while (zip[i] == toZip[i]) {
            i++;
        }
        return route[i][toZip[i]];
    }

// events

    void doXmit () {
        Message thisMsg = dequeueMsg();
        if (nextMsg != null) {
            schedule();
        }

        boolean match = true;
        for (int i=0; i<maxLevel; i++) {
            match &= (thisMsg.destZip[i] == zip[i]);
        }
        if (match) {
            // at destination
            double total = clock - thisMsg.start;
            delivery.mark(thisMsg.start, total);
            transitTime.count(total);
            hops.count(thisMsg.hopCount);
            trace("deliver", thisMsg);
            msgCount--;
        } else {
            // en route
            int receiver = nextHop(thisMsg.destZip);
            trace("forward", station[receiver]);
            if (plotting) {
                plotfil.print(clock);
                plotfil.print(" "+latatude);
                plotfil.print(" "+longitude);
                plotfil.print(" "+station[receiver].latatude);
                plotfil.println(" "+station[receiver].longitude);
            }
            if (station[receiver].queueLength < queueLimit) {
                station[receiver].queueMsg(thisMsg);
                thisMsg.hopCount++;
            } else {
                trace("blocked by", station[receiver]);
                queueMsg(thisMsg);	// blocked (no ack) so retry
            }
        }
        propagate();
    }

    void doArrival () {
        Message newMsg = new Message();
        int dest = 0;
        if (popBased) {
            int object = (int)(uniform()*totalPop);
            while (object>station[dest].population) {
                dest++;
                object -= station[dest].population;
            }
        } else {
            dest = (int)(uniform()*station.length);
        }
        newMsg.from = this;
        newMsg.to = station[dest];
        newMsg.destZip = station[dest].zip;
        newMsg.start = clock;
        newMsg.track = Math.abs(clock-217.479) < 0.01;
        newMsg.hopCount = 0;
        trace("arrival", newMsg);
        if (queueLength < queueLimit) {
            queueMsg(newMsg);
            msgCount++;
        } else {
            trace("blocked at", newMsg);
        }
        thisEvent.time = clock + arrivalWait();
        if (thisEvent.time <= lastArrival) {
            queue(thisEvent);
        }
    }

    void doUpdate () {
        for (int i=0; i<lastPath; i++) {
            int receiver = path[i];
            station[receiver].updateFrom(nodeNum);
        }
        updating = false;
        updateCount++;
    }

    void updateFrom(int sender) {
        boolean change = false;
        for (int l=0; l<maxLevel; l++) {
            for (int z=0; z<maxOrder; z++) {
                change = updateStation(sender, l, z, change);
            }
            if (station[sender].zip[l] != zip[l]) {
                break;
            }
        }
        if (change) {
            propagate();
        }
    }

    private boolean updateStation(int sender, int l, int z, boolean change) {
        int[] offer = station[sender].promise[l];
        if (offer[z] != 0) {
          // known region
           if (offer[z] < delay[l][z] || route[l][z] == sender) {
               // better offer or current dest
               if (station[sender].route[l][z] != nodeNum) {
                   // no flip
                   trace("update", station[sender]);
                   change |= delay[l][z] != offer[z];
                   delay[l][z] = offer[z];
                   route[l][z] = sender;
               }
           }
       }
        return change;
    }

}
