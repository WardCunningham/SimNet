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

    int from=SimNet.ny, to=SimNet.sf;
    public MapPanel(Image image) {
        this.usa = image;
        addMouseListener(new MouseAdapter() {
            public void mousePressed (MouseEvent e) {
                from = hit(e.getPoint());
            }
            public void mouseReleased (MouseEvent e) {
                to = hit(e.getPoint());
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged (MouseEvent e) {
                to = hit(e.getPoint());
            }
        });
     }

     int hit (Point p) {
         Station s[] = SimNet.station;
         int k=-1, z = 9999999;
         for (int i = 0; i < s.length; i++) {
             Point q = loc(s[i]);
             int d = Math.abs(p.x-q.x) + Math.abs(p.y-q.y);
             if (d<z) {
                 k=i;
                 z=d;
             }
         }
         return k;
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
        //Simulator.trace("paint", g.getClipBounds());

        // background
        Dimension d = getSize();
        g.drawImage(usa, 0, 0, d.width, d.height, this);

        // labels & paths
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

        // paths in use
        g.setColor(Color.yellow);
        for (int i=0; i<station.length; i++) {
            Point o = loc(station[i]);
            int p[] = station[i].path;
            boolean u[] = station[i].pathsInUse();
            for (int j=0; j<u.length; j++) {
                if (u[j]) {
                    Point e = loc(station[p[j]]);
                    g.drawLine(o.x, o.y, e.x, e.y);
                }
            }
        }

        // dots & bubbles
        for (int i=0; i<station.length; i++) {
            int queue = station[i].queueLength;
            boolean updateing = station[i].updating;
            g.setColor(updateing ? Color.magenta : queue>0 ? Color.yellow : Color.red);
            Point o = loc(station[i]);
            int r = (int)(Math.sqrt(queue+1)*5);
            g.fillOval(o.x-r, o.y-r, 2*r, 2*r);
            g.setColor(Color.black);
            g.drawOval(o.x-r, o.y-r, 2*r, 2*r);
        }

        // traceroute
        for (int i=0; i<3; i++) {
            int node = from;
            int goal = to;
            g.setColor(Color.blue);
            trace(1, g, node, goal, station);
            g.setColor(Color.green);
            trace(-1, g, goal, node, station);
        }
    }

    private void trace(int dir, Graphics g, int node, int goal, Station[] station) {
        for (int i=0; node!=goal && i<30; i++) {
            Station s = station[node];

            // bar graph
            int y = 3*i+5;
            int dx = dir * (1+s.delay(goal));
            g.drawLine(200, y, 200+dx, y);

            // route segment
            int next = s.nextHop(goal);
            Point o = loc(s);
            Point e = loc(station[next]);
            g.drawLine(f(o.x), f(o.y), f(e.x), f(e.y));

            node = next;
      }
    }

    int f(int n) {
        return (int)(n + Simulator.normal());
    }

    static double n=51.5, w=127.0, e=64.5, s=23.5;
    Point loc (Station station) {
        Dimension d = getSize();
        int x = (int)(d.width * (w + station.longitude/100.0) / (w-e));
        int y = (int)(d.height * (n - station.latatude/100.0) / (n-s));
        return new Point(x, y);
    }
}