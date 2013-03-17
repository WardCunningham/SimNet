
import java.io.PrintStream;

public class Scatter extends Simulator {

	static int maxXScat = 100;
	static int maxYScat = 51;


	String xLabel;
    String yLabel;
    int count = 0;
	double sumX = 0;
	double sumY = 0;
	double xInc;
	double yInc;
	boolean marks[][] = new boolean [maxXScat][maxYScat];

	Scatter (String xLabel, double xMax, String yLabel, double yMax) {
        this.xLabel = xLabel;
        this.yLabel = yLabel;
		xInc = xMax / maxXScat;
		yInc = yMax / (maxYScat-1);
		for (int i=0; i<maxXScat; i++) {
			for (int j=0; j<maxYScat; j++) {
				marks[i][j]=false;
			}
		}
        reports.add(this);
	}

	void mark (double x, double y) {
		count++;
		sumX += x;
		sumY += y;
		marks[limit(x/xInc,maxXScat)][limit(y/yInc,maxYScat)] = true;
	}

	void report () {
        PrintStream out = output(yLabel);
		out.println(runName);
		out.println();
		for (int i=maxYScat-1; i>=0; i--) {
			int k = (maxYScat-1) - i;
			if (k < yLabel.length()) {
				out.print(" "+yLabel.charAt(k)+"  ");
			} else {
				out.print("    ");
			}
			if (i%5 == 0) {
				out.print(w(i*yInc,9));
			} else {
				out.print("         ");
			}
			for (int j=0; j<maxXScat; j++) {
				if (marks[j][i]) {
					out.print('*');
				} else {
					out.print(' ');
				}
			}
			out.println();
		}
		out.println();
		out.print("           ");
		for (int j=0; j<=maxXScat; j+=10) {
			out.print(w(j*xInc,9));
		}
		out.println();
		out.println();
		out.println("            ");
		for (int k=0; k<xLabel.length(); k++) {
			out.print(" "+xLabel.charAt(k));
		}
		out.println();
		out.println();
		if (count>0) {
			out.println(w(sumX/count, 7) + " mean " + xLabel);
			out.println(w(sumY/count, 7) + " mean " + yLabel);
		}
	}
}
