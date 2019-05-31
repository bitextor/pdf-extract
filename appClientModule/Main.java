import com.java.app.PDFExtract;
import com.java.classes.Common;

public class Main {
	public static void main(String[] args) {
		try{

			Common common = new Common();

			if (args == null || args.length == 0 || args[0].equals("--help")) {
				
				common.printHelp();
				System.exit(0);
				
			}

			String input = "", output = "", batchfile = "", options = "", logpath = "", rulepath = "", threadcount = "", language = "";
			String key = "";

			for (String parm : args) {
				
				if (parm.startsWith("-")) {
					key = parm.substring(1);
				}else {
					if (key.equals("I")) {
						input = parm;
					}else if (key.equals("O")) {
						output = parm;
					}else if (key.equals("B")) {
						batchfile = parm;
					}else if (key.equals("L")) {
						logpath = parm;
					}else if (key.equals("R")) {
						rulepath = parm;
					}else if (key.equals("T")) {
						threadcount = parm;
					}else if (key.equals("LANG")) {
						language = parm;
					}else if (key.equals("o")) {
						options = parm;
					}
					key = "";
				}
			}

			if (!common.IsEmpty(input) && !common.IsEmpty(output)) {
				
				try {
					PDFExtract oExtractor = new PDFExtract(logpath);
					oExtractor.Extract(input, output, rulepath, language, options);
				}catch(Exception e) {
					common.print(input, "Extract fail: " + e.getMessage());
				}
				
			}else if (!common.IsEmpty(batchfile)) {

				try {
					PDFExtract oExtractor = new PDFExtract(logpath);
					oExtractor.Extract(batchfile, rulepath, common.getInt(threadcount), language, options);
				}catch(Exception e) {
					common.print(batchfile, "Extract fail: " + e.getMessage());
				}
				
			}else {
				
				common.printHelp();
				
				System.out.println("Cannot start extract: Invalid parameters.");
				System.exit(0);
				
			}
			
			
		}catch(Exception ex){
			System.out.println("Cannot start extract. Error=" + ex.getMessage());
			System.exit(0);
        }finally {
		}
	}

	/* (non-Java-doc)
	 * @see java.lang.Object#Object()
	 */
	public Main() {
		super();
	}

}