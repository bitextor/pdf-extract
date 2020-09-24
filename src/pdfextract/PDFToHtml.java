package pdfextract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;

import com.itextpdf.text.pdf.PdfEncryptor;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * @author MickeyVI
 */
public class PDFToHtml {

	Common common = new Common();
	private long timeout = 30;

	public PDFToHtml(long timeout_) {
		if (timeout_ > 0)
			timeout = timeout_;
	}

	/**
	 * Extract pdf stream to html
	 */
	public StringBuffer extract(ByteArrayInputStream bIn) throws Exception {
		StringBuffer sbuf = new StringBuffer();
		try {
			
			return sbuf.append(executeCommandByteArrayInputStream(bIn).toString());
		} catch (Exception e) {
			return new StringBuffer();
		}
	}

	/**
	 * Extract pdf file to html
	 */
	public StringBuffer extract(String inputPath) throws Exception {

		StringBuffer sb = null;

		try {
			sb = new StringBuffer();

			String sCommand[] = new String[] { "pdftohtml", "-stdout", "-i", "-noframes", "-xml", "-fontfullname", inputPath};

			try {
//				executeCommand(sCommand);
				//#42 Use stdout for pdftohtml 
				sb.append(executeCommand(sCommand).toString());
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				if (errorMsg.contains("Permission Error:")) {
					decrypt(inputPath);
					sb = new StringBuffer();
					sb.append(executeCommand(sCommand).toString());
				} else {
					throw e;
				}
			}
			return sb;
		} catch (Exception e) {
			throw e;
		} 
	}

	public void decrypt(String file) throws IOException {
		decrypt(null, file);
	}

	public void decrypt(PdfReader reader, String file) throws IOException {
		File fTemp = null;
		try {

			if (reader == null) {
				reader = new PdfReader(file);
				PdfReader.unethicalreading = true;
			}

			fTemp = File.createTempFile(new File(file).getName(), ".unlocked");
			fTemp.deleteOnExit();

			String inputPathUnlocked = fTemp.getPath();
			PdfEncryptor.encrypt(reader, new FileOutputStream(inputPathUnlocked), null, null,
					PdfWriter.ALLOW_ASSEMBLY | PdfWriter.ALLOW_COPY | PdfWriter.ALLOW_DEGRADED_PRINTING
							| PdfWriter.ALLOW_FILL_IN | PdfWriter.ALLOW_MODIFY_ANNOTATIONS
							| PdfWriter.ALLOW_MODIFY_CONTENTS | PdfWriter.ALLOW_PRINTING
							| PdfWriter.ALLOW_SCREENREADERS,
					false);

			common.moveFile(fTemp.getPath(), file);
		} catch (Exception e) {
		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
			if (fTemp != null)
				common.deleteFile(fTemp);
		}

	}

	/**
	 * Execute command to extract
	 */
	private StringBuilder executeCommand(String... command) throws Exception {

		Process proc = null;
		boolean bTimeout = false;
		StringBuilder sbError = null;
		StringBuilder sb = null;
		try {
			sb = new StringBuilder();
			sbError = new StringBuilder();

			proc = Runtime.getRuntime().exec(command);
			
			//#38 Deadlock on stderr from pdftohtml
			StreamGobbler errorStreamGobbler = new StreamGobbler("ErrorStream", proc.getErrorStream());
			StreamGobbler inputStreamGobbler = new StreamGobbler("InputStream", proc.getInputStream());
			
			Thread tInput = new Thread(inputStreamGobbler);
			Thread tError = new Thread(errorStreamGobbler);
			tError.start();
			tInput.start();
			
			if (!proc.waitFor(timeout, TimeUnit.SECONDS)) {
				// timeout - kill the process.
				bTimeout = true;
				proc.destroyForcibly();
			}
			
			tError.join();
			tInput.join();
			
			errorStreamGobbler.CloseBuffer();
			inputStreamGobbler.CloseBuffer();
			
			sbError = errorStreamGobbler.getSbText();
			sb = inputStreamGobbler.getSbText();
			
			if (sbError.length() > 0) {
				String sTemp = sbError.toString();
				// Skip the warning meesage
				if (!proc.isAlive() && !StringUtils.isEmpty(sTemp) && proc.exitValue() != 0)
					throw new Exception(sTemp.toString());
				else {
					return sb;
				}
			}else {
				// success
				return sb;
			}
			
		} catch (IOException e) {
			if (bTimeout) {
				throw new TimeoutException(
						"Timed out waiting for poppler extract pdf reach. (" + timeout + " seconds)");
			} else {
				throw e;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			proc.destroy();
		}
	}
	
	private StringBuilder executeCommandByteArrayInputStream(ByteArrayInputStream bIn) throws Exception {

		Process proc = null;
		boolean bTimeout = false;
		StringBuilder sbError = null;
		StringBuilder sb = null;
		try {
			sb = new StringBuilder();
			sbError = new StringBuilder();
			
			String[] commands = new String[6];
						
			commands[0] = "pdftohtml";
			commands[1] = "-xml";
			commands[2] = "-fontfullname";
			commands[3] = "-stdout";
			commands[4] = "-";
			commands[5] = "nonsense";
			
			ProcessBuilder procBuilder = new ProcessBuilder(commands);
			proc = procBuilder.start();

			//#38 Deadlock on stderr from pdftohtml
			StreamGobbler errorStreamGobbler = new StreamGobbler("ErrorStreamST", proc.getErrorStream());
			StreamGobbler inputStreamGobbler = new StreamGobbler("InputStreamST", proc.getInputStream(),
					proc.getOutputStream(), bIn);
			inputStreamGobbler.SetOutputStream();
			Thread tInput = new Thread(inputStreamGobbler);
			Thread tError = new Thread(errorStreamGobbler);
			tError.start();
			tInput.start();
			
			if (!proc.waitFor(timeout, TimeUnit.SECONDS)) {
				// timeout - kill the process.
				bTimeout = true;
				proc.destroyForcibly();
			}
			
			tError.join();
			tInput.join();
			
			errorStreamGobbler.CloseBuffer();
			inputStreamGobbler.CloseBuffer();
			
			sbError = errorStreamGobbler.getSbText();
			sb = inputStreamGobbler.getSbText();

			if (sbError.length() > 0) {
				throw new Exception(sbError.toString());
			}else {
				// success
				return sb;
			}
			
		} catch (IOException e) {
			if (bTimeout) {
				throw new TimeoutException(
						"Timed out waiting for poppler extract pdf reach. (" + timeout + " seconds)");
			} else {
				throw e;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			proc.destroy();
		}
	}
}
