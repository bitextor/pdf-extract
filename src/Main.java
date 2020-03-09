import pdfextract.Common;
import pdfextract.PDFExtract;

/**
 * @author MickeyVI
 */
public class Main {
	public static void main(String[] args) {
		PDFExtract oExtractor = null;
		try {

			Common common = new Common();

			if (args == null || args.length == 0 || args[0].equals("--help")) {

				/**
				 * Print out help
				 */
				common.printHelp();
				System.exit(0);

			}

			String input = "", output = "", batchfile = "", logpath = "", threadcount = "", verbose = "",
					keepbrtags = "", getperms = "", configfile = "";
			String key = "";

			/**
			 * Get command-line arguments
			 */
			for (String parm : args) {

				if (parm.startsWith("--")) {
					key = parm.substring(2);
					if (key.equals("keepbrtags")) {
						keepbrtags = "1";
						key = "";
					} else if (key.equals("getperms")) {
						getperms = "1";
						key = "";
					}
				} else if (parm.startsWith("-")) {
					key = parm.substring(1);
					if (key.equals("v")) {
						verbose = "1";
						key = "";
					}
				} else {
					if (key.equals("I")) {
						input = parm;
					} else if (key.equals("O")) {
						output = parm;
					} else if (key.equals("B")) {
						batchfile = parm;
					} else if (key.equals("L")) {
						logpath = parm;
					} else if (key.equals("T")) {
						threadcount = parm;
					} else if (key.equals("C")) {
						configfile = parm;
					}
					key = "";
				}
			}

			common.setVerbose(common.getInt(verbose));
			if (!common.IsEmpty(input) && !common.IsEmpty(output)) {
				/**
				 * Call function to extract single PDF file
				 */
				try {
					oExtractor = new PDFExtract(logpath, common.getInt(verbose), configfile);
					oExtractor.Extract(input, output, common.getInt(keepbrtags), common.getInt(getperms));
				} catch (Exception e) {
					common.print("File: " + input + ", " + e.getMessage());
				}

			} else if (!common.IsEmpty(batchfile)) {
				/**
				 * Call function to extract PDF with batch file
				 */
				try {
					oExtractor = new PDFExtract(logpath, common.getInt(verbose), configfile);
					oExtractor.Extract(batchfile, common.getInt(threadcount), common.getInt(keepbrtags),
							common.getInt(getperms));
				} catch (Exception e) {
					common.print("File: " + batchfile + ", " + e.getMessage());
				}

			} else {
				common.print("Cannot start extract: Invalid parameters.");
				System.exit(0);
			}

		} catch (Exception ex) {
			System.out.println("Cannot start extract. Error=" + ex.getMessage());
			System.exit(0);
		} finally {
			if (oExtractor != null) {
				try {
					oExtractor.shutdownProcess();
				} catch (Exception e) {
					e.printStackTrace();
				}
				oExtractor = null;
				System.gc();
			}
		}
	}

	/*
	 * (non-Java-doc)
	 * 
	 * @see java.lang.Object#Object()
	 */
	public Main() {
		super();
	}

}
