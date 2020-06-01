package pdfextract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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
		// create random string for naming input temporary file
		String rand = common.getStr(Math.random());

		File fTempIn = null;

		try {
			// create temporary input file
			fTempIn = File.createTempFile("pdf-", rand);
			fTempIn.deleteOnExit();

			// write input stream to temporary input file
			IOUtils.copy(bIn, new FileOutputStream(fTempIn));

			return extract(fTempIn.getPath());
		} catch (Exception e) {
			return new StringBuffer();
		} finally {
			if (fTempIn != null) {
				common.deleteFile(fTempIn);
			}
		}
	}

	/**
	 * Extract pdf file to html
	 */
	public StringBuffer extract(String inputPath) throws Exception {

		StringBuffer sb = null;

		try {
			sb = new StringBuffer();

//			String sCommand[] = new String[] { "pdftohtml", "-s", "-i", "-noframes", "-xml", "-fontfullname", inputPath,
//					outputPath };
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
					try {
						sb.append(executeCommand(sCommand).toString());
					} catch (Exception ex) {
						throw ex;
					}
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

//			proc = Runtime.getRuntime().exec("pdftohtml -xml -stdout -fontfullname /home/ramoslee/work/pdfExtract/testing/jordan17p.pdf");
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
			
			tError.interrupt();
			tInput.interrupt();
			
			tError.join();
			tInput.join();
			errorStreamGobbler.CloseBuffer();
			sbError = errorStreamGobbler.getSbText();
			sb = inputStreamGobbler.getSbText();
			
//			System.out.println(sb.toString());
			
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
				e.printStackTrace();
				throw e;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			proc.destroy();
		}
	}
}
