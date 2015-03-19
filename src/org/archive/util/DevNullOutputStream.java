/** For avoiding disk I/O.
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.util;

import java.io.OutputStream;

public class DevNullOutputStream extends OutputStream {
	public void write ( int b ) { } 
}
