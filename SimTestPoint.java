// SimTestPoint created Jan 22, 2002 9:24:08 AM
// Copyright (c) 2002, Ward Cunningham

import java.text.Format;
import java.text.DecimalFormat;

class SimTestPoint extends TestPoint {

    void clock() {
        DecimalFormat decimal = new DecimalFormat();
        out.println(decimal.format(Simulator.clock));
    }

    void trace() {
        Simulator.trace("TestPoint", new java.util.Date());
        Simulator.printTrace(out);
    }

    void queuing () {
        out.println("<font size=-1><pre>");
        Simulator.queuing.report(out);
    }
    void routing () {
        out.println("<font size=-1><pre>");
        Simulator.routing.report(out);
    }
    void delivery () {
        out.println("<font size=-1><pre>");
        Simulator.delivery.report(out);
    }

    void hops () {
        out.println("<font size=-1><pre>");
        Simulator.hops.report(out);
    }


}

