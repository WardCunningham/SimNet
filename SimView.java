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
    
        JCheckBox runButton = new JCheckBox("Run");
        add(runButton);
        runButton.setSelected(SimNet.run);
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SimNet.run = ! SimNet.run;
            }
        });

        JCheckBox linkButton = new JCheckBox("Link");
        add(linkButton);
        linkButton.setSelected(SimNet.linked);
        linkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SimNet.linked = ! SimNet.linked;
                if (SimNet.linked) {
                    SimNet.station[from].addPath(to);
                    SimNet.station[to].addPath(from);
                } else {
                    SimNet.station[from].removePath(to);
                    SimNet.station[to].removePath(from);
                }
            }
        });


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

    // public void itemStateChanged(ItemEvent e) {
    //     Object source = e.getItemSelectable();
    //     boolean state = e.getStateChange() != ItemEvent.DESELECTED;
    //     System.out.println("itemStateChanged");
    //     if (source == runButton) {
    //         SimNet.run = state;
    //     } else if (source == linkButton) {
    //         //...make a note of it...
    //     }
    // }

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
                Thread.sleep(50);
                repaint();
            }
        }
        catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D)gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g); //paint background
        Simulator.trace("paint", g.getClipBounds());

        // background
        Dimension d = getSize();
        g.drawImage(usa, 0, 0, d.width, d.height, this);

        // readouts & link
        g.setColor(new Color(0,0,0,127));
        Station[] station = SimView.sim.station;
        g.drawString(station[from].city + " to " + station[to].city, 20, 15);
        String minutes = String.format("%3.2f minutes",SimView.sim.clock);
        g.drawString(minutes,20,d.height-10);

        // labels & paths
        g.setColor(new Color(0,0,0,95));
        for (int i=0; i<station.length; i++) {
            Point o = loc(station[i]);
            if (d.height>800) {
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
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(5));
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
        g.setStroke(new BasicStroke(1));

        // dots & bubbles
        for (int i=0; i<station.length; i++) {
            int queue = station[i].queueLength;
            Point o = loc(station[i]);
            int r = 4;
            g.setColor(Color.red);
            g.fillOval(o.x-r, o.y-r, 2*r, 2*r);
            g.setColor(Color.black);
            g.drawOval(o.x-r, o.y-r, 2*r, 2*r);
            if(station[i].updating) {
                g.setColor(Color.red);
                g.drawOval(o.x-r-2, o.y-r-2, 2*r+4, 2*r+4);
            }
            r = (int)(Math.sqrt(queue+1)*5);
            if (queue>1) {
                g.setColor(new Color(255,255,0,150));
                g.fillOval(o.x-r, o.y-r, 2*r, 2*r);
                g.setColor(Color.black);
                g.drawOval(o.x-r, o.y-r, 2*r, 2*r);
            }
        }

        // traceroute
        trace(g);
        // rivertrace(g, station);

    }

    private void rivertrace(Graphics gg, Station[] station) {
        Graphics2D g = (Graphics2D)gg;
        g.setColor(Color.blue);
        for (int i=0; i<station.length; i++) {
            g.setColor(Color.blue);
            trace(1, g, i, to);
            g.setColor(Color.green);
            //trace(-1, g, to, i);
        }
    }

    private void trace(Graphics gg) {
        Graphics2D g = (Graphics2D)gg;
        for (int i=0; i<3; i++) {
            g.setColor(Color.blue);
            trace(1, g, from, to);
            g.setColor(Color.green);
            trace(-1, g, to, from);
        }
    }

    private void trace(int dir, Graphics2D g, int node, int goal) {
        Dimension d = getSize();
        Station[] station = SimView.sim.station;
        for (int i=0; node!=goal && i<40; i++) {
            Station s = station[node];
            int next = s.nextHop(goal);
            boolean diff = s.zone(goal) != station[next].zone(goal);

            // bar graph
            int x = d.width - 70 + 6*dir;
            int y = d.height - (3*i+5);
            int dx = dir * (1+s.delay(goal));
            g.drawLine(x, y, x+dx, y);
            if(diff) g.fillOval(x-2-3*dir, y-2, 4, 4);

            // route segment
            Point o = loc(s);
            Point e = loc(station[next]);
            g.setStroke(new BasicStroke(3));
            g.drawLine(f(o.x), f(o.y), f(e.x), f(e.y));
            g.setStroke(new BasicStroke(1));
            if (diff) g.fillOval (f((o.x + e.x)/2-4), f((o.y + e.y)/2-4), 9, 9);

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