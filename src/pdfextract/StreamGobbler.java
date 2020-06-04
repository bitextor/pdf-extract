package pdfextract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.management.monitor.Monitor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

public class StreamGobbler extends Thread {

	private StringBuilder sbText;
	private BufferedReader bufReader;
	private OutputStream outputStream;
	private ByteArrayInputStream bInput;

	StreamGobbler(String name, InputStream inStream) {
		super(name);
		this.bufReader = new BufferedReader(new InputStreamReader(inStream));
	}

	StreamGobbler(String name, InputStream inputStream, OutputStream outputStream, ByteArrayInputStream bIn) {

		super(name);
		this.outputStream = outputStream;
		this.bInput = bIn;
		this.bufReader = new BufferedReader(new InputStreamReader(inputStream));
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
			}

		} catch (Exception e) {
			// Ignore error
		}
	}

	public StringBuilder getSbText() {
		if (null == this.sbText) {
			this.sbText = new StringBuilder();
		}
		return this.sbText;
	}

	public void SetOutputStream() throws Exception {
		if (null != bInput && null != outputStream) {
			try {
				byte[] bytes = new byte[2048];
				int index;
				while ((index = bInput.read(bytes)) != -1) {
					outputStream.write(bytes, 0, index);
				}

			} catch (IOException e) {
				throw e;
			} finally {
				try {
					this.outputStream.flush();
					this.outputStream.close();
				} catch (IOException e) {

				}

			}
		}
	}

	public void CloseBuffer() {
		try {
			if (null != this.bufReader)
				bufReader.close();
		} catch (IOException e) {

		}

		try {
			if (null != this.bInput)
				this.bInput.close();
		} catch (IOException e) {

		}

		try {
			if (null != this.outputStream)
				this.outputStream.close();
		} catch (IOException e) {

		}
	}

}
