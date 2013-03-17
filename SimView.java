import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SimView extends JApplet {
    static String usaFile = "usa.gif";
    static SimNet sim = new SimNet();

    public void init() {
        sim.setup();
        Image usa = getImage(getCodeBase(), usaFile);
        MapPanel usaPanel = new MapPanel(usa);
        getContentPane().add(usaPanel, BorderLayout.CENTER);
        fork(usaPanel);
    }


    public static void main(String[] args) throws InterruptedException {
        (new SimTestPoint()).start();
        sim.setup();
        Image usa = Toolkit.getDefaultToolkit().getImage(SimView.usaFile);
        MapPanel usaPanel = new MapPanel(usa);

        JFrame f = new JFrame("Simulation Viewer");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.getContentPane().add(usaPanel, BorderLayout.CENTER);
        f.setSize(621+6, 356+16);
        f.setVisible(true);
        fork(usaPanel);
    }

    static void fork(MapPanel usaPanel) {
        (new Thread(usaPanel, "Map Refresh")).start();
        (new Thread(sim, "Simulator")).start();
    }
}

class MapPanel extends JPanel implements Runnable {
    Image usa;

    public MapPanel(Image image) {
        this.usa = image;
    }

    public void run() {
		try {
            while (true) {
                Thread.sleep(250);
                repaint();
            }
        }
        catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g); //paint background
        Simulator.trace("paint", g.getClipBounds());

        Dimension d = getSize();
        g.drawImage(usa, 0, 0, d.width, d.height, this);


        Station[] station = SimView.sim.station;
        g.setColor(Color.black);
        for (int i=0; i<station.length; i++) {
            Point o = loc(station[i]);
            if (d.height>400) {
                g.drawString(station[i].city, o.x+6, o.y+5);
            }
            int p[] = station[i].path;
            for (int j=0; j<p.length; j++) {
                if (p[j]>0) {
                    Point e = loc(station[p[j]]);
                    g.drawLine(o.x, o.y, e.x, e.y);
                }
            }
        }
        for (int i=0; i<station.length; i++) {
            int queue = station[i].queueLength;
            g.setColor(queue>0 ? Color.yellow : Color.red);
            Point o = loc(station[i]);
            int r = (int)(Math.sqrt(queue+1)*5);
            g.fillOval(o.x-r, o.y-r, 2*r, 2*r);
            g.setColor(Color.black);
            g.drawOval(o.x-r, o.y-r, 2*r, 2*r);
        }
    }

    static double n=51.5, w=127.0, e=64.5, s=23.5;
    Point loc (Station station) {
        Dimension d = getSize();
        int x = (int)(d.width * (w + station.longitude/100.0) / (w-e));
        int y = (int)(d.height * (n - station.latatude/100.0) / (n-s));
        return new Point(x, y);
    }
}