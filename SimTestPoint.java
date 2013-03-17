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

    void queueing () {
        out.println("<font size=-1><pre>");
        Simulator.queueing.report(out);
    }

}

