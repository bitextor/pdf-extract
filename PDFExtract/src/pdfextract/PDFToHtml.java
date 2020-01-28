package pdfextract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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
		try {
			if (common.IsEmpty(outputPath)) {
				String name = common.getName(inputPath);

				// create temporary output file
				fTempOut = File.createTempFile(name, ".xml");
				fTempOut.deleteOnExit();
				outputPath = fTempOut.getPath();
			}

			StringBuffer sb = new StringBuffer();

			String sCommand[] = new String[] { "pdftohtml", "-q", "-stdout", "-s", "-i", "-noframes", "-xml",
					"-fontfullname", inputPath, outputPath };
			String result = executeCommand(sCommand);

			// if standard output return, use standard output
			if (!common.IsEmpty(result)) {
				sb.append(result);
			} else {
				sb.append(FileUtils.readFileToString(fTempOut, "UTF-8"));
			}

			return sb;
		} catch (Exception e) {
			return new StringBuffer();
		} finally {
			if (fTempOut != null) {
				common.deleteFile(fTempOut);
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
