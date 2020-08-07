import { readJson, readJsonSync } from "https://deno.land/std/fs/read_json.ts";

class Graph {
    constructor (label, maximum) {
        this.label = label
        reports.push(this)
    }

    report() {

    }
}

class Scatter {
    constructor (xLabel, xMax, yLabel, yMax) {
        this.xLabel = xLabel
        this.yLabel = yLabel
        reports.push(this)
    }

    mark(x, y) {

    }

    report() {

    }
}


const eventNames = ["arrival", "xmit", "sample", "update"];

class EventBlock {
    constructor (clock, nodeNum) {
        this.time = clock
        this.node = nodeNum
        this.type = null
    }

    toString() {
        let result = this.type+"("+w(this.time,8);
        if (this.node>=0) result += " "+station[this.node];
        return result+")";
    }

    static newUpdate(clock, nodeNum) {
        let e = new EventBlock(clock, nodeNum);
        e.type = 'update';
        return e;
    }

    static newXmit(clock, nodeNum) {
        let e = new EventBlock(clock, nodeNum);
        e.type = 'xmit';
        return e;
    }

    static newArrival(clock, nodeNum) {
        let e = new EventBlock(clock, nodeNum);
        e.type = 'arrival';
        return e;
    }

    static newSample(clock) {
        let e = new EventBlock(clock, -1);
        e.type = 'sample';
        return e;
    }

    dispatch () {
         switch (this.type) {
            case 'xmit': {
                station[this.node].doXmit();
                break;
            }
            case 'arrival': {
                station[this.node].doArrival();
                break;
            }
            case 'sample': {
                this.doSample();
                break;
            }
            case 'update': {
                station[this.node].doUpdate();
                break;
            }
        }
    }

    doSample () {
        let interval = 1.0;
        routing.mark(clock, 60/interval * updateCount/station.length);
        backlog.mark(clock, msgCount);
        trace("sample", "events: " + eventCount + " updates: " + updateCount + " messages: " + msgCount);
        eventCount = 0;
        updateCount = 0;
        if (clock < lastArrival || msgCount > 0) {
            queue(EventBlock.newSample(clock+interval));
        }
    }

}


class Message {
    track;       // path tracking
    destZip;     // destination
    hopCount;
    queued;      // received time
    start;       // start time
    from;
    to;

    toString() {
        return "Message("+this.from.city+" to "+this.to.city+" started "+w(this.start,8)+")";
    }

}



const runName = " population based load";

// sizes

const maxPath = 9;
const maxLevel = 3;
const maxOrder = 9;
const maxDelay = 255;
const maxClock = 300.0;

// xmit scheduling parameters

const waitTime = 0.50;
const xmitTime = 0.13;

// node buffer limit

const queueLimit = 95;

// load specifications

const lastArrival = 240.0;
const interArrival = 20.0; // typ 30.0
const arrivalTime = 2.0;
const dynamic = true;      // load dependent routing
const linked = true;       // trans-con link
const popBased = true;     // population based routing

// major state

let clock = 0.0;           // simulated time
let meanPop = 0;           // mean population
let totalPop = 0;          // total population

let eventCount = 0;        // sample statistics
let msgCount = 0;
let updateCount = 0;

// tracing

    // static class Trace {
    //     double time;
    //     String label;
    //     Object a, b;

    //     public String toString() {
    //         return
    //             td(w(time, 8)) +
    //             td(label) +
    //             td(a.toString()) +
    //             td(b.toString());
    //     }

    //     String td (String d) {
    //         return "<td valign=top>" + d + "</td>";
    //     }
    // }

    // static Trace traces[] = new Trace [200];
    // static int nextTrace = 0;

    // static void halt (String err) {
    //     trace("Halt", err);
    //     printTrace();
    //     System.err.println(err + " (see trace)");
    //     System.exit(-1);
    // }

    // static void printTrace() {
    //     PrintStream out;
    //     try {out = new PrintStream(new FileOutputStream("output/trace.html"));}
    //     catch (FileNotFoundException e) {out=System.out;}
    //     printTrace(new PrintWriter(out));
    // }

    // static void printTrace(PrintWriter out) {
    //     out.println("<table CELLSPACING=0 CELLPADDING=2>");
    //     double clock=0;
    //     int tick=0;
    //     for (int i=nextTrace+1; (i%traces.length)!=nextTrace; i=(i+1)%traces.length) {
    //         Trace t = traces[i];
    //         if (t==null) continue;
    //         if (t.time != clock) {
    //             clock = t.time;
    //             tick++;
    //         }
    //         String color = tick%2==0 ? "#cfffcf" : "#afffaf";
    //         out.println("<tr bgcolor=" + color + ">" + t + "</tr>");
    //     }
    //     out.println("</table>");
    //     out.flush();
    // }

function trace(label, a, b) {
    console.log(`trace ${clock.toFixed(0)} ${label} ${a.toString()}`)
}
    // static void trace (String label, Object a, Object b) {
    //     Trace newTrace = traces[nextTrace];
    //     if (newTrace == null) {
    //         newTrace = traces[nextTrace] = new Trace();
    //     }
    //     nextTrace=(nextTrace+1)%traces.length;
    //     newTrace.time = clock;
    //     newTrace.label = label;
    //     newTrace.a = a;
    //     newTrace.b = b;
    // }

    // static void trace (String label, Object a) {
    //     trace(label, a, "");
    // }
    // static void trace (String label) {
    //     trace(label, "", "");
    // }


// event queue

    let thisEvent;        // event queue (holds blocks)
    let eventQueue = [];

    function queue (entry) {
        trace("queue", entry);
        eventQueue.push(entry);
        eventQueue.sort((a,b) => a.time - b.time)
    }

    function dequeue() {
        return eventQueue.shift();
    }

    function queueTime() {
        return eventQueue[0].time;
    }

// reports

    let reports = [];

    function report() {
        for (let s of reports) {
            s.report();
        }
    }

    let input;
    let output;
    let plotfil;     // trace output for plot
    let station = [];                   // all stations

    let retrys = new Graph("retransmissions", 2.0);
    let queuedTime = new Graph("queueing delay", 5.0);
    let transitTime = new Graph("transit time", 50.0);
    let hops = new Graph("hop count", 50);

    let queuing = new Scatter("elapsed time", maxClock, "message queue length", 100.0);
    let backlog = new Scatter("elapsed time", maxClock, "messages in transit", 300.0);
    let routing = new Scatter("sample time", maxClock, "routing broadcasts", 500.0);
    let delivery = new Scatter("start time", maxClock, "transit time", 50.0);

    console.log(reports)

    // static PrintStream output(String name) {
    //     String path = "output/" + name + ".txt";
    //     PrintStream out;
    //     try {out = new PrintStream(new FileOutputStream(path));}
    //     catch (FileNotFoundException e) {throw new Error("can't open " + path);}
    //     return out;
    // }



// utilities

const uniform = () => Math.random();

const exponential = () => - Math.log(uniform());

const normal = () => [0,0,0,0,0,0,0,0,0,0,0,0].reduce(sum => sum+uniform(), 0) - 6

const limit = (r, max) => Math.max(Math.min(Math.round(r),max),0)

const w = (s, w) => (s.toString()+"                       ").substring(0, w-1)+" ";


// station

class Station {
    nodeNum;        // ordinal number
    numPath = 0;    // connectivity
    path = [];      // specific connections
    zip = [];       // this stations routing code
    route = [];     // routing tables [maxLevel][maxOrder]
    delay = [];     // most recent information about peers [maxLevel][maxOrder]
    promise = [];   // last thing I promised [maxLevel][maxOrder]
    updating = false;   // update in progress
    queueLength = 0;    // number in message queue
    useCount = 0;       // message count for utilization
    useTime = 0.0;      // time using or waiting to use channel
    city = '';          // unique city name
    tracing = false;    // debug output flag
    population = 0;     // population in thousands
    latatude = 0;       // location;
    longitude = 0;
    displacement = 0;   // legend tweek for map

    messageQueue = []    // replaces linked list

    constructor (props) {
        this.nodeNum = props.node;
        this.city = props.city;
        this.zip = props.details[0]
        this.population = props.details[1];
        this.latatude = props.details[2];
        this.longitude = -props.details[3];
        this.displacement = props.details[4];
        this.path = props.path.filter(n => n>0).map(n => n-1)
        this.route = props.route.map(r => r.filter(n => n>0).map(n => n-1))
        this.delay = this.route.map(r => r.map(n => 0))
        this.promise = this.route.map(r => r.map(n => 0))
    }

    addPath(node) {
        this.path.push(node);
    }


    scheduleUpdate() {
    //     if (!dynamic) return;
    //     if (updating) return;
    //     // trace("consider update", this);
    //     int load = queueLength + 1;
    //     for (int l=0; l<maxLevel; l++) {
    //         // look for changes
    //         for (int z=0; z<maxOrder; z++) {
    //             double now = delay[l][z]+load;
    //             double past = promise[l][z];
    //             if (past == 0) {
    //                 updating = true;
    //             } else {
    //                 double diff = Math.abs(now-past)/past;
    //                 updating |= diff > 0.20;
    //             }
    //         }
    //     }
    //     if (!updating) return;

    //     // trace("schedule update", this);
    //     for (int l=0; l<maxLevel; l++) {
    //         for (int z=0; z<maxOrder; z++) {
    //             promise[l][z] = delay[l][z]+load;
    //         }
    //     }
    //     queue(EventBlock.newUpdate(clock+0.2, nodeNum));
    }

    // void scheduleMessage() {
    //     trace("schedule message", this);
    //     // compute interference
    //     double success = 1.0;
    //     for (int i=0; i<numPath; i++) {
    //         if (station[path[i]].hasMsg()) {
    //             success *= waitTime / (xmitTime + waitTime);
    //         }
    //     }
    //     double thisRetries = 1.0 / success - 1.0;
    //     double totalTime =  xmitTime + thisRetries * (xmitTime+waitTime);
    //     EventBlock e = EventBlock.newXmit(clock+totalTime, nodeNum);

    //     // collect stats
    //     retrys.count(thisRetries);
    //     queueTime.count(clock - nextMsg.queued);
    //     useCount++;
    //     useTime += (e.time - clock);
    //     queue(e);
    // }

    queueMsg(m) {
        m.queued = clock;
        this.queueLength++;
        trace("queue", m);
        queuing.mark (clock, this.queueLength);
        this.messageQueue.push(m)
        this.scheduleUpdate();
    }

    // Message dequeueMsg() {
    //     Message msg = nextMsg;
    //     nextMsg = msg.next;
    //     if (nextMsg == null) {
    //         lastMsg = null;
    //     }
    //     queueLength--;
    //     trace("dequeue", msg);
    //     queuing.mark(clock, queueLength);
    //     return msg;
    // }

    arrivalWait () {
        let t;
        if (this.popBased) {
            t = interArrival * meanPop / population - arrivalTime;
        } else {
            t = interArrival - arrivalTime;
        }
        if (t<0) t = 0;
        return exponential()*t + arrivalTime;
    }

  // routing table access

//     int delay (int i) {
//         return delay(station[i].zip);
//     }

//     int delay (int[] toZip) {
//         int z = zone(toZip);
//         return delay[z][toZip[z]];
//     }

//     int promise (int i) {
//         return promise(station[i].zip);
//     }

//     int promise (int[] toZip) {
//         int z = zone(toZip);
//         return promise[z][toZip[z]];
//     }

//     boolean[] pathsInUse () {
//         boolean result[] = new boolean[numPath];
//         for (Message m = nextMsg; m != null; m = m.next) {
//             if (m.destZip != zip) {
//                 int hop = nextHop(m.destZip);
//                 for (int i=0; i<numPath; i++) {
//                     if (path[i] == hop) {
//                         result[i] = true;
//                     }
//                 }
//             }
//         }
//         return result;
//     }

//     int nextHop (int i) {
//         return nextHop(station[i].zip);
//     }

//     int nextHop (int[] toZip) {
//         int z = zone(toZip);
//         return route[z][toZip[z]];
//     }

//     int zone(int i) {
//         return zone(station[i].zip);
//     }

//     int zone(int[] toZip) {
//         int i = 0;
//         while (i < (maxLevel-1) && zip[i] == toZip[i]) {
//             i++;
//         }
//         return i;
//     }

// // events

//     void doXmit () {
//         Message thisMsg = dequeueMsg();
//         if (nextMsg != null) {
//             scheduleMessage();
//         }

//         boolean match = true;
//         for (int i=0; i<maxLevel; i++) {
//             match &= (thisMsg.destZip[i] == zip[i]);
//         }
//         if (match) {
//             // at destination
//             double total = clock - thisMsg.start;
//             delivery.mark(thisMsg.start, total);
//             transitTime.count(total);
//             hops.count(thisMsg.hopCount);
//             trace("deliver", thisMsg);
//             msgCount--;
//         } else {
//             // en route
//             int receiver = nextHop(thisMsg.destZip);
//             trace("forward", station[receiver]);
//             if (station[receiver].queueLength < queueLimit) {
//                 station[receiver].queueMsg(thisMsg);
//                 thisMsg.hopCount++;
//             } else {
//                 trace("blocked by", station[receiver]);
//                 queueMsg(thisMsg);  // blocked (no ack) so retry
//             }
//         }
//         scheduleUpdate();
//     }

    doArrival () {
        let newMsg = new Message();
        let dest = 0;
        if (popBased) {
            let object = Math.floor(uniform()*totalPop);
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
        if (queue.length < queueLimit) {
            this.queueMsg(newMsg);
            msgCount++;
        } else {
            trace("blocked at", newMsg);
        }
        thisEvent.time = clock + this.arrivalWait();
        if (thisEvent.time <= lastArrival) {
            queue(thisEvent);
        }
    }

//     void doUpdate () {
//         for (int i=0; i<numPath; i++) {
//             int receiver = path[i];
//             station[receiver].updateFrom(nodeNum);
//         }
//         updating = false;
//         updateCount++;
//     }

//     void updateFrom(int sender) {
//         int changes = 0;
//         for (int level=0; level<maxLevel; level++) {
//             for (int zip=0; zip<maxOrder; zip++) {
//                 int[] offer = station[sender].promise[level];
//                 if (offer[zip] != 0) {
//                   // known region
//                    if (offer[zip] < delay[level][zip] || route[level][zip] == sender) {
//                        // better offer or current dest
//                        if (station[sender].route[level][zip] != nodeNum) {
//                            // no flip
//                            if (delay[level][zip] != offer[zip])
//                                changes++;
//                            delay[level][zip] = offer[zip];
//                            route[level][zip] = sender;
//                        }
//                    }
//                }
//             }
//             if (station[sender].zip[level] != zip[level]) {
//                 break;
//             }
//         }
//         if (changes > 0) {
//             // trace ("update from", station[sender], new Integer(changes));
//             scheduleUpdate();
//         }
//     }
}

// construction

function SimNet () {
    station = new Array(173);
    for (let i=0; i<station.length; i++) {
        station[i] = new Station();
        station[i].nodeNum = i;
    }
}

const ny = 10-1;       // new york
const sf = 169-1;      // san francisco

function readInput() {
    const data = readJsonSync("./data.json");
    totalPop = 0;
    for (let props of data) {
        station[props.node-1] = new Station(props)
        totalPop += station[props.node-1].population;
    }
    meanPop = totalPop / station.length;
    if (linked) {
        station[ny].addPath(sf);
        station[sf].addPath(ny);
    }
}


// simulation

function initialArrivals () {
    for (let i=0; i<station.length; i++) {
        let e = EventBlock.newArrival(clock + station[i].arrivalWait(), i);
        if (e.time <= lastArrival) {
            queue(e);
        } else {
            trace("no arrival", station[i]);
        }
    }
}

function initialSample () {
    let s = EventBlock.newSample(clock);
    queue(s);
}

function simulate(period) {
    let endTime = clock + period;
    while (eventQueue.length > 0 && queueTime()<endTime){
        dispatch();
    }
    clock = endTime;
}

function dispatch() {
    if (eventQueue.length == 0) return;
    thisEvent = dequeue();
    clock = thisEvent.time;
    trace("dispatch", thisEvent);
    thisEvent.dispatch();
    eventCount++;
}

    // public void run() {
    //     try {
    //         Thread.sleep(2000);
    //         while (true) {
    //             simulate(0.02);     // minutes
    //             Thread.sleep(50);   // milliseconds
    //         }
    //     } catch (InterruptedException e) {
    //         System.out.println(e.getMessage());
    //     }
    // }


// reporting

    // void report () {
    //     PrintStream out = output("summary");
    //     Scatter throughput = new Scatter("message count", 500, "radio utilization", 100);
    //     out.println(runName);
    //     out.println();
    //     out.println(clock + " minutes simulated");
    //     out.println(lastArrival + " last arrival");
    //     out.println(interArrival + " mean inter-arrival period");
    //     out.println(arrivalTime + " minimum inter-arrival period");
    //     out.println(queueLimit + " buffer limit");
    //     out.println(dynamic + " dynamic routing");
    //     out.println(linked + " trans-con data link");
    //     out.println(popBased + " population based (" + totalPop + ") load");
    //     out.println(transitTime.count + " messages delivered");
    //     out.println();
    //     out.println();
    //     out.println(" message counts and utilizations");
    //     for (int i=0; i<station.length; i++) {
    //         if (i%5 == 0) {
    //             out.println();
    //         }
    //         out.print(
    //             w(station[i].useCount, 4) + " " +
    //             w(station[i].useTime/clock*100.0, 5) + " " +
    //             w(station[i].city, 14));
    //         if(transitTime.count!=0) {
    //             throughput.mark(
    //                 station[i].useCount,
    //                 station[i].useTime/clock*100.0);
    //         }
    //     }
    //     out.println();
    // }

// main

function setup() {
    readInput();
    initialSample();
    initialArrivals();
}

function finish() {
        while(eventQueue.length>0) {
            thisEvent = dequeue();
            trace("leftover", thisEvent);
        }
        report();
        // printTrace();
    }

function main () {
        setup();
        simulate(maxClock);
        finish();
    }

main()

