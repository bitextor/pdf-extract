package pdfextract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

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
		return extract(inputPath, null);
	}

	/**
	 * Extract pdf file to html
	 */
	public StringBuffer extract(String inputPath, String outputPath) throws Exception {

		File fTempOut = null;
		String inputPathUnlocked = inputPath + ".unlocked";
		try {
			if (common.IsEmpty(outputPath)) {
				String name = common.getName(inputPath);

				// create temporary output file
				fTempOut = File.createTempFile(name, ".xml");
				fTempOut.deleteOnExit();
				outputPath = fTempOut.getPath();
			}

			StringBuffer sb = new StringBuffer();
			String sCommand[] = new String[] { "pdftohtml", "-s", "-i", "-noframes", "-xml", "-fontfullname", inputPath,
					outputPath };

			try {
				executeCommand(sCommand);
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				if (errorMsg.contains("Permission Error:")) {
					decrypt(inputPath);
					executeCommand(sCommand);
				} else {
					throw e;
				}
			}

			sb.append(FileUtils.readFileToString(fTempOut, "UTF-8"));
			
			return sb;
		} catch (Exception e) {
			throw e;
		} finally {
			if (fTempOut != null) {
				common.deleteFile(fTempOut);
			}
			common.deleteFile(inputPathUnlocked);
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
	private String executeCommand(String... command) throws Exception {

		Process proc = null;
		try {
			StringBuilder sb = new StringBuilder("");

			proc = Runtime.getRuntime().exec(command);

			// Read result
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				sb.append(line + '\n');
			}

			// Read any errors from the attempted command
			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			StringBuilder sbError = new StringBuilder();
			String sProcessError = null;
			while ((sProcessError = stdError.readLine()) != null) {
				sbError.append(sProcessError + '\n');
			}

			if (sbError.length() == 0) {
				// success
				return sb.toString();
			} else {
				throw new Exception(sbError.toString());
			}
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			throw e;
		} finally {
			proc.destroy();
		}
	}
}
