import junit.framework.*;
import java.util.*;
import java.io.*;

public class SimNetTest extends TestCase {

	SimNet s;

	public SimNetTest(String name) {
		super(name);
	}
	protected void setUp() {
		s = new SimNet();
	}
	public static Test suite() {
		return new TestSuite(SimNetTest.class);
	}


// Tests

public void testSystem () {
	assertEquals("java.io.BufferedInputStream", System.in.getClass().getName());
	assertEquals("java.io.PrintStream", System.out.getClass().getName());
}

public void testFile () throws Exception {
	InputStream in = new FileInputStream("data.txt");
	assertEquals(' ', in.read());
	assertEquals(' ', in.read());
	assertEquals('1', in.read());
	assertEquals(' ', in.read());
	assertEquals('P', in.read());
	in.close();
}

public void testRead () throws Exception {
	InputStream in = new FileInputStream("data.txt");
    Station ss = new Station();
	ss.in = in;
	assertEquals(1, ss.readInt());
	assertEquals("Portland", ss.readString(20));
	assertEquals(1, ss.readInt());
	assertEquals(1, ss.readInt());
	assertEquals(1, ss.readInt());
	assertEquals(1080, ss.readInt());
	assertEquals(4366, ss.readInt());
	assertEquals(7026, ss.readInt());
	assertEquals(0, ss.readInt());
	assertEquals(4, ss.readInt()); // next line
	assertEquals(2, ss.readInt());
}

public void testReadNode () throws Exception {
	InputStream in = new FileInputStream("data.txt");
	s.in = in;
	int i=0; Station n;
	n = s.station[i++]; n.readInput();
	assertEquals("Portland", n.city);
	n = s.station[i++]; n.readInput();
	assertEquals("Boston", n.city);
	assertEquals(-1, n.displacement);
	n = s.station[i++]; n.readInput();
	assertEquals("Fall River", n.city);
}

public void testReadInput () throws Exception {
	assertEquals(44,s.station[44].nodeNum);
	InputStream in = new FileInputStream("data.txt");
	s.in = in;
	s.readInput();
	assertEquals("Eureka", s.station[172].city);
	assertEquals(45,s.station[45].nodeNum);
}

}
