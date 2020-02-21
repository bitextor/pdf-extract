package pdfextract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

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
			String sCommand[] = new String[] { "pdftohtml", "-q", "-s", "-i", "-noframes", "-xml", "-fontfullname",
					inputPath, outputPath };

			String result = "";
			try {
				result = executeCommand(sCommand);
				result = FileUtils.readFileToString(fTempOut, "UTF-8");
				if (common.IsEmpty(result)) {
					decrypt(inputPath);
					result = executeCommand(sCommand);
				}
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				if (errorMsg.contains("Permission Error:")) {
					decrypt(inputPath);
					result = executeCommand(sCommand);
				} else {
					throw e;
				}
			}

			// if standard output return, use standard output
			if (!common.IsEmpty(result)) {
				sb.append(result);
			} else {
				sb.append(FileUtils.readFileToString(fTempOut, "UTF-8"));
			}

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

	private void decrypt(String file) throws IOException {
		PDDocument document = null;
		try {
			InputStream keyStoreStream = null;
			String password = "";
			String alias = "";
			document = PDDocument.load(new File(file), password, keyStoreStream, alias);
			if (document.isEncrypted()) {
				document.setAllSecurityToBeRemoved(true);
				document.save(file);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (document != null) {
				document.close();
			}
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
