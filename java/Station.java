
import java.io.*;

public class Station extends Simulator {
    int nodeNum;        // ordinal number
    int numPath = 0;    // connectivity
    int path[] =        new int [maxPath];              // specific connections
    int zip[] =         new int [maxLevel];             // this stations routing code
    int route[][] =     new int [maxLevel][maxOrder];   // routing tables
    int delay[][] =     new int [maxLevel][maxOrder];   // most recent information about peers
    int promise[][] =   new int [maxLevel][maxOrder];   // last thing I promised
    boolean updating;   // update in progress
    Message nextMsg;    // message queue pointers
    Message lastMsg;
    int queueLength;    // number in message queue
    int useCount;       // message count for utilization
    double useTime;     // time using or waiting to use channel
    String city;        // unique city name
    boolean tracing;    // debug output flag
    int population;     // population in thousands
    int latatude;       // location;
    int longitude;
    int displacement;   // legend tweek for map

    void debug() {
        System.err.format("nodeNum: %d %s\n", nodeNum, city);
        for (int i=0; i<maxLevel; i++) {
            for (int j=0; j<maxOrder; j++) {
                System.err.format("\t%d", route[i][j]);
            }
            System.err.format("\n");
        }
    }

    void debugroute(int row, int col) {
        System.err.format("nodeNum: %d %s\n", nodeNum, city);
        for (int i=0; i<maxLevel; i++) {
            for (int j=0; j<maxOrder; j++) {
                System.err.format("\t%d%s", route[i][j], (row==i&&col==j)?" <":"  ");
            }
            System.err.format("\n");
        }
    }
    
    void debugtrace(int toward) {
        int row = zone(toward);
        int col = station[toward].zip[row];
        int nxt = route[row][col];
        debugroute(row,col);
        if (nodeNum == toward) {
            System.err.format("destination\n");
            return;
        }
        System.err.format("row: %d, col: %d, nxt: %d\n", row, col, nxt);
        station[nextHop(toward)].debugtrace(toward);
    }

    void addPath(int city) {
        path[numPath++] = city;
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

    void scheduleUpdate() {
        if (!dynamic) return;
        if (updating) return;
        // trace("consider update", this);
        int load = queueLength + 1;
        for (int l=0; l<maxLevel; l++) {
            // look for changes
            for (int z=0; z<maxOrder; z++) {
                double now = delay[l][z]+load;
                double past = promise[l][z];
                if (past == 0) {
                    updating = true;
                } else {
                    double diff = Math.abs(now-past)/past;
                    updating |= diff > 0.20;
                }
            }
        }
        if (!updating) return;

        // trace("schedule update", this);
        for (int l=0; l<maxLevel; l++) {
            for (int z=0; z<maxOrder; z++) {
                promise[l][z] = delay[l][z]+load;
            }
        }
        queue(EventBlock.newUpdate(clock+0.2, nodeNum));
    }

    void scheduleMessage() {
        trace("schedule message", this);
        // compute interference
        double success = 1.0;
        for (int i=0; i<numPath; i++) {
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
        queuing.mark (clock, queueLength);
        if (nextMsg == null) {
            nextMsg = m;
            scheduleMessage();
        } else {
            lastMsg.next = m;
        }
        lastMsg = m;
        scheduleUpdate();
    }

    Message dequeueMsg() {
        Message msg = nextMsg;
        nextMsg = msg.next;
        if (nextMsg == null) {
            lastMsg = null;
        }
        queueLength--;
        trace("dequeue", msg);
        queuing.mark(clock, queueLength);
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

  // routing table access

    int delay (int i) {
        return delay(station[i].zip);
    }

    int delay (int[] toZip) {
        int z = zone(toZip);
        return delay[z][toZip[z]];
    }

    int promise (int i) {
        return promise(station[i].zip);
    }

    int promise (int[] toZip) {
        int z = zone(toZip);
        return promise[z][toZip[z]];
    }

    boolean[] pathsInUse () {
        boolean result[] = new boolean[numPath];
        for (Message m = nextMsg; m != null; m = m.next) {
            if (m.destZip != zip) {
                int hop = nextHop(m.destZip);
                for (int i=0; i<numPath; i++) {
                    if (path[i] == hop) {
                        result[i] = true;
                    }
                }
            }
        }
        return result;
    }

    int nextHop (int i) {
        return nextHop(station[i].zip);
    }

    int nextHop (int[] toZip) {
        int z = zone(toZip);
        return route[z][toZip[z]];
    }

    int zone(int i) {
        return zone(station[i].zip);
    }

    int zone(int[] toZip) {
        int i = 0;
        while (i < (maxLevel-1) && zip[i] == toZip[i]) {
            i++;
        }
        return i;
    }

// events

    void doXmit () {
        Message thisMsg = dequeueMsg();
        if (nextMsg != null) {
            scheduleMessage();
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
            if (station[receiver].queueLength < queueLimit) {
                station[receiver].queueMsg(thisMsg);
                thisMsg.hopCount++;
            } else {
                trace("blocked by", station[receiver]);
                queueMsg(thisMsg);  // blocked (no ack) so retry
            }
        }
        scheduleUpdate();
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
        for (int i=0; i<numPath; i++) {
            int receiver = path[i];
            station[receiver].updateFrom(nodeNum);
        }
        updating = false;
        updateCount++;
    }

    void updateFrom(int sender) {
        int changes = 0;
        for (int level=0; level<maxLevel; level++) {
            for (int zip=0; zip<maxOrder; zip++) {
                int[] offer = station[sender].promise[level];
                if (offer[zip] != 0) {
                  // known region
                   if (offer[zip] < delay[level][zip] || route[level][zip] == sender) {
                       // better offer or current dest
                       if (station[sender].route[level][zip] != nodeNum) {
                           // no flip
                           if (delay[level][zip] != offer[zip])
                               changes++;
                           delay[level][zip] = offer[zip];
                           route[level][zip] = sender;
                       }
                   }
               }
            }
            if (station[sender].zip[level] != zip[level]) {
                break;
            }
        }
        if (changes > 0) {
            // trace ("update from", station[sender], new Integer(changes));
            scheduleUpdate();
        }
    }
}
