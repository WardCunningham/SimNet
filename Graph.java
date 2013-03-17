
import java.io.*;

public class Graph extends Simulator {

	static int maxCounter = 50;
	static int maxColumn = 100;

	String label;
    double maximum;

	int count = 0;
	double highest = 0.0;
	double sum = 0.0;
	double sumsq = 0.0;
	int counter[] = new int [maxCounter];

	Graph (String label, double maximum) {
		this.label = label;
        this.maximum = maximum;
        reports.add(this);
	}

	void count (double item) {
		count++;
		sum = sum + item;
		sumsq = sumsq + item*item;
		highest = item > highest ? item : highest;
		int i = limit(item/maximum*maxCounter, maxCounter);
		counter[i]++;
	}

	void report () {
        PrintStream out = output(label);
		out.println(runName);
		out.println();
		int max = 0;
		for (int i=0; i<maxCounter; i++) {
			if (counter[i] > max) max = counter[i];
		}
		if (max > 0) {
			for (int i=0; i<maxCounter; i++) {
				if (i<label.length()) {
					out.print("   " + label.charAt(i) + "  ");
				} else {
					out.print("      ");
				}
				if (i%5 == 0) {
					out.print(w(i*maximum/maxColumn, 4));
				} else {
					out.print("    ");
				}
				int pos = (int)(counter[i]*maxColumn/max);
				for (int j=0; j<pos; j++)
					out.print('*');
				out.println();
			}
			out.println();
			out.println();
			out.println(count+" total items");
			if (count>0) {
				double mean=sum/count;
				out.println(w(highest, 8) + " maximum");
				out.println(w(mean, 8) + " arithmetic mean");
				out.println(w(Math.sqrt(sumsq/count - mean*mean), 8) + " standard deviation");
			}
		} else {
			out.println("No record of "+label);
		}
	}
}
