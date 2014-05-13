// Message created Dec 8, 2001 2:57:37 PM
// Copyright (c) 2002, Ward Cunningham

public class Message extends Simulator {
    Message next;       // forward links only
    boolean track;      // path tracking
    int destZip[] = new int [maxLevel]; // destination
    int hopCount;
    double queued;      // received time
    double start;       // start time
    Station from, to;

    public String toString() {
        return "Message("+from.city+" to "+to.city+" started "+w(start,8)+")";
    }

}
