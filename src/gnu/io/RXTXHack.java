package gnu.io;

/*
 * https://forums.oracle.com/thread/1294323
 * Whenever a read or write is called on an RXTXPort then a field called IOLocked will be incremented and when the operation is finished
 *  this field will be decremented. However this incrementation and decrementation is not properly synchronized so 
 *  when running code where one thread reads and another one writes I got the the IOLocked field in a state where the value was != 0. 
 *  The problem with this is that the close method will wait forever while the IOLocked field has a value != 0. 
 *  The workaround for this is a rather nasty hack. Since the IOLocked field has default access then you can modify it 
 *  from outside the class. So I reset the IOLocked field before the close method is called.
 */
public final class RXTXHack {

	public static void closeRxtxPort(RXTXPort port) {
		port.IOLocked = 0;
		port.close();
	}

	private RXTXHack() {

	}
}