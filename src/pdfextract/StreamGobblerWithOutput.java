package pdfextract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

public class StreamGobblerWithOutput extends Thread{
	  InputStream inputStream;
	  String inputParam;
	  OutputStream outputStream;
	  PrintWriter printWriter;
	  StringBuilder outputBuffer = new StringBuilder();
	  BufferedReader bufferedReader;
	  private boolean sudoIsRequested = true;
	  boolean isError = false;
	  
	  StreamGobblerWithOutput(String name, InputStream inputStream)
	  {
		  super(name);
		  this.inputStream = inputStream;
		  this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

	  }

	  StreamGobblerWithOutput(String name, InputStream inputStream, OutputStream outputStream, String inputParam)
	  {
		super(name);
	    this.inputStream = inputStream;
	    this.outputStream = outputStream;
	    this.printWriter = new PrintWriter(outputStream);
	    this.inputParam = inputParam;
	    this.sudoIsRequested = true;
	    this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	  }
	  
	  public void run()
	  {

	    if (sudoIsRequested &&  null != printWriter)
	    {
//	      doSleep(5000);
	      printWriter.println(inputParam);
	      printWriter.flush();
	    }

	    try
	    {
	      String line = null;
	      
	      int i = 0;
	      while (null !=  this.bufferedReader &&  (line = this.bufferedReader.readLine()) != null )
	      {
	    	i ++ ;
	        outputBuffer.append(line + "\n");

	        if (!bufferedReader.ready()) {
	        	break;
	        }
	      }
	      
	      if (i > 1) {
	    	  isError = true;
	      }else {
	    	  isError = false;
	      }
	    }
	    catch (IOException ioe)
	    {
	      // ignore error
	    }
	    
	  }
	  
	  public String getSentenceJoin(String inputParam) throws Exception {
		  String sOutput = "";
		  if (null != printWriter)
		    {
//		      doSleep(5000);
		      printWriter.println(inputParam);
		      printWriter.flush();
		      
		      try
			    {
			      String line = null;
			      while ((line = this.bufferedReader.readLine()) != null )
			      {
		  	    	  sOutput = line;
		  	    	  if (!bufferedReader.ready()) {
		  	    		  break;
		  	    	  }
			      }
			    }
			    catch (IOException ioe)
			    {
			      throw new Exception(ioe.getMessage());
			    }
		    }

		   return sOutput;
	  }
	  
	  private void doSleep(long millis)
	  {
	    try
	    {
	      Thread.sleep(millis);
	    }
	    catch (InterruptedException e)
	    {
	      // ignore
	    }
	  }
	  
	  public StringBuilder getOutputBuffer()
	  {
	    return outputBuffer;
	  }
	  
	  public void CloseBuffer() {
		   try {
			   this.bufferedReader.close();
			} catch (Exception e) {
				
			}
		   
		   try {
			   this.printWriter.close();
		   } catch (Exception e) {
				
		   }
		   
		   try {
			   this.inputStream.close();
		   } catch (Exception e) {
				
		   }
		   
		   try {
			   this.outputStream.close();
		   } catch (Exception e) {
				
		   }
		   
		   try {
			   this.printWriter.close();
		   } catch (Exception e) {
				
		   }
	   }

		public boolean GetErrorFlag() {
			return isError;
		}
}
