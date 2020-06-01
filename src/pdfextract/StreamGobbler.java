package pdfextract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.management.monitor.Monitor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

public class StreamGobbler extends Thread{

	private  StringBuilder sbText;
	private	 BufferedReader bufReader ;

	   public StreamGobbler(String name, InputStream inStream) {
	      super(name);
	      this.bufReader = new BufferedReader(new InputStreamReader(
	            inStream));
	   }
	   

	   @Override
	   public void run() {
		   GetInputStream();
	   }

	   private void GetInputStream() {
		   try {

			    String sLine = "";
			    while ((sLine = bufReader.readLine()) != null) {
		            getSbText().append(sLine);
		            getSbText().append("\n");
//		            if (!bufReader.ready()) {
//		            	break;
//		            }
		         }
		         
		      }  catch (Exception e) {
		    	  // Ignore error
			  }
	   }

	   public StringBuilder getSbText() {
			if (null == this.sbText) {
				this.sbText = new StringBuilder();
			}
			return this.sbText;
		}
	   
	   public void CloseBuffer() {
		   try {
				bufReader.close();
			} catch (IOException e) {
				
			}
	   }
	
}
