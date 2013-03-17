// TestPoint Diagnostic Web Server
// (c) 2002, Cunningham & Cunningham, Inc.

import java.net.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

public class TestPoint implements Runnable {

    PrintWriter out;        // write html here
    BufferedReader in;      // read POST input here (optional)
    String request;         // read URI here (optional)

	void date() {
		out.println(new java.util.Date());
	}

    void index() {
        Method m[] = getClass().getDeclaredMethods();
        for (int i=0; i<m.length; i++) {
            if (m[i].getModifiers() != 0) continue;
            String name = m[i].getName();
            out.println("<li><a href=" + name + ">" + name + "</a> ");
        }
        out.println("<p><font size=-1>&copy; <a href=//c2.com/doc/TestPoint/>c2.com</a></font>");
    }

    public void run () {
        Class empty[] = {};
		try {
			ServerSocket server = new ServerSocket(8080);
            System.out.println("TestPoint http://" + InetAddress.getLocalHost().getHostName() + ":" + server.getLocalPort());
			while (true) {
				Socket client = server.accept();
				out = new PrintWriter(client.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				request = in.readLine();
				System.out.println("TestPoint " + request);
                StringTokenizer tokens = new StringTokenizer(request);
                String type = tokens.nextToken();
                String name = tokens.nextToken().substring(1);
                try {
                    Method handler = getClass().getDeclaredMethod(name, empty);
                    if (handler.getModifiers() != 0) throw new Exception();
                    handler.invoke(this, empty);
                } catch (Exception e) {
                    index();
                }
				in.close();
				out.close();
				client.close();
			}
		} catch (IOException e) {
			System.out.println("TestPoint quit: " + e.getMessage());
		}
	}

    public void start() {
         (new Thread(this, getClass().getName() + " Web Server")).start();
    }

	public static void main (String argv[]) {
		(new TestPoint()).start();
	}
}
