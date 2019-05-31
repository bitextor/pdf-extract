package com.java.app;

import java.util.List;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.fit.pdfdom.PDFToHTML;

import com.java.classes.Common;
import com.java.classes.NormalizedHTML;
import com.java.classes.read2map;

public class PDFExtract {
	//
	private final boolean isRunable = true;
	//
	private String logPath = "";
	private String customScript = "";
	//
	private boolean bLog = true;
	private boolean jsEngineFail = false;
	private boolean isBatch = false;
    //
	private Object _lockerExtract = new Object();
    //
	private int iCountThreadExtract = 0;
	private int iMaxThreadExtract = 1;
    //
	private Thread tExtract = null;
	NormalizedHTML oNormalized = null;
    read2map oR2M = null;
	private Invocable jsEngine = null;
	//
	private Common common = new Common();
	//
	
	public PDFExtract() throws Exception {
		bLog = false;
	}
	public PDFExtract(String logpath) throws Exception {
		if (common.IsEmpty(logpath)) {
			bLog = false;
		}else {
			bLog = true;
			
			if (common.IsEmpty(common.getExtension(logpath))){
				logpath += ".log";
			}
			
			logPath = logpath;

			if (isRunable) common.print("Log File: " + logPath);
		}
	}
	
	public void Extract(String inputFile, String outputFile, String rulePath, String language, String options) throws Exception {
		
		try {
			if (bLog) {
				if (isRunable) common.print(inputFile, "Start extract");
				common.writeLog(logPath, inputFile, "Start extract", false);
			}else {
				common.print(inputFile, "Start extract");
			}
			
			//--- Validate parameters
			//- Check input file exist
			if (!common.IsExist(inputFile)) {
				throw new Exception("Input file does not exist.");
			}
			
			//- Check input file extension
			if (!common.getExtension(inputFile).toLowerCase().equals("pdf")) {
				throw new Exception("Input file extension is not pdf.");
			}
			//--- End Validate parameters

			//
			if (isRunable) {
				common.checkPermissions(inputFile);
			}	
			//

			//
			String inputName = common.getBaseName(inputFile);
			String outputName = common.getBaseName(outputFile);
			String outputExt = common.getExtension(outputFile);
			String outputPath = common.getParentPath(outputFile);
			if (!common.IsEmpty(outputPath) && !common.IsExist(outputPath)) {
				common.createDir(outputPath);
			}
			//

			//read custom script
			//--- Get custom rule script
            synchronized (_lockerExtract) { 
            	customScript = common.getCustomScript(rulePath, customScript);
            }
			
			if (common.IsNull(jsEngine) && !common.IsEmpty(customScript) && !jsEngineFail) {
				if (jsEngine == null && !jsEngineFail) {
					jsEngine = common.getJSEngine(customScript);
					if (jsEngine == null) jsEngineFail = true;
				}
			}
            
			//--- Call PDF -> HTML process
			String outputHtml = outputName + ".org." + outputExt;
			if (!common.IsEmpty(outputPath)) {
				outputHtml = common.combine(outputPath, outputHtml);
			}
			try {
				PDFToHTML.run(new String[] { inputFile, outputHtml, "-im=IGNORE" });
			}catch(Exception e) {
				//System.out.println("Error: PDFToHTML: " + e.getMessage());
				throw new Exception("Convert pdf to html fail.: " + e.getMessage());
			}
            //
			
			//--- Call HTML -> HTML with Box process
			String inputHtmlBox = outputHtml;
			String outputHtmlBox = outputName + ".box." + outputExt;
			if (!common.IsEmpty(outputPath)) {
				outputHtmlBox = common.combine(outputPath, outputHtmlBox);
			}
			
			if (oR2M == null) {
        		oR2M = new read2map();
			}
            try {
            	read2map oR2M = new read2map(); 
            	oR2M.process(inputHtmlBox, outputHtmlBox);
            }catch(Exception e) {
				throw e;
            }
	        //
	        common.deleteFile(inputHtmlBox);
			//--- End PDF -> HTML process
			
			//--- Call Normalize HTML process
			String inputNormalize = outputHtmlBox;
	        //String inputNormalize = "pdf.box.html";
			String outputNormalize = outputName + ".normalized." + outputExt;
			if (!common.IsEmpty(outputPath)) {
				outputNormalize = common.combine(outputPath, outputNormalize);
			}

			if (oNormalized == null) {
				oNormalized = new NormalizedHTML(language, jsEngine);
			}
            try {
            	//NormalizedHTML oNormalized = new NormalizedHTML(language, jsEngine);
    			oNormalized.Process(inputNormalize, outputNormalize);
            }catch(Exception e) {
				throw e;
            }
			//
			common.deleteFile(inputNormalize);
			//--- End Normalize HTML process
			
			//--- Write output
			common.moveFile(outputNormalize, outputFile);
			//--- End Write output

			if (bLog) {
				if (isRunable) common.print(inputFile, "Extract success. -> " + outputFile + "");
				common.writeLog(logPath, inputFile, "Extract success. -> " + outputFile + "", false);
			}else {
				common.print(inputFile, "Extract success. -> " + outputFile + "");
			}
			
		}catch(Exception e) {
			String message = e.getMessage();
			if (bLog) {
				common.writeLog(logPath, inputFile, "Error: " + message, true);
			}else {
				if (!isRunable) common.print(inputFile, "Error: " + message);
			}

			throw e;
		}finally {
			if (isBatch) {
	            synchronized (_lockerExtract) { iCountThreadExtract--; }
			}
		}

	}
	public Object ExecuteScript(String scriptText, String function, Object... args) throws Exception {
		Object result = null;
		try {

            synchronized (_lockerExtract) { 
				if (jsEngine == null && !jsEngineFail) {
					ScriptEngineManager manager = new ScriptEngineManager();
					ScriptEngine engine = manager.getEngineByName("JavaScript");
					//
					jsEngine = (Invocable) engine;
					Compilable compEngine = (Compilable) engine;
					CompiledScript script = compEngine.compile(scriptText);
					script.eval();
				}
            }
            if (jsEngine != null) {
    			result = jsEngine.invokeFunction(function, args);
            }

		} catch (Exception e) {	
			result = null;
			jsEngineFail = true;
		}
		return result;
	}
	public void Extract(String batchFile, String rulePath, int threadCount, String language, String options) throws Exception {
		try {
			isBatch = true;
			
			if (bLog) {
				if (isRunable) common.print("Start extract batch file: " + batchFile);
				common.writeLog(logPath, "Start extract batch file: " + batchFile);
			}else {
				common.print("Start extract batch file: " + batchFile);
			}
			
			//--- Validate parameters
			//- check input file exist
			if (!common.IsExist(batchFile)) {
				throw new Exception("Input batch file does not exist.");
			}

			//read custom script
			//--- Get custom rule script
			if (common.IsEmpty(customScript)) {
				customScript = common.getCustomScript(rulePath, customScript);
			}
			
			if (!common.IsEmpty(customScript)) {
				jsEngine = common.getJSEngine(customScript);
				if (jsEngine == null) jsEngineFail = true;
			}
			//--- End Validate parameters

			//
    		oR2M = new read2map();
			oNormalized = new NormalizedHTML(language, jsEngine);
			//
			if (threadCount == 0) threadCount = 1;
	        int iMaxThread = threadCount;
			//
			
			List<String> lines = common.readLines(batchFile);
			
			int ind = 0, len = lines.size();
			while (ind < len) {
				
				if (iCountThreadExtract < iMaxThread) {
					String line = lines.get(ind);
					
					AddThreadExtract(ind, line, rulePath, language, options);

					ind++;
				}
				
				Thread.sleep(10);
			}

			while (iCountThreadExtract > 0) {
				Thread.sleep(10);
			}
			
		}catch(Exception e) {
			String message = e.getMessage();
			if (bLog) {
				common.writeLog(logPath, message, true);
			}else {
				if (!isRunable) common.print("Error: " + e.getMessage());
			}
			throw e;
		}finally {
			if (bLog) {
				if (isRunable) common.print("Finish extract batch file: " + batchFile);
				common.writeLog(logPath, "Finish extract batch file: " + batchFile);
			}else {
				common.print("Finish extract batch file: " + batchFile);
			}
		}
	}
	
    private void AddThreadExtract(int index, String line, String rulePath, String language, String options)
    {
        try
        {
            String sThreadName = "ext-" + index;
            
            synchronized (_lockerExtract) { iCountThreadExtract++; }
            tExtract = new Thread(sThreadName) {
    			public void run() {

    				String inputFile = "", outputFile = "";
    				try {
	    				//--- Validate line
	    				String[] cols = line.split("\t");
	    				
	    				if (cols == null || cols.length < 2) {
	    					throw new Exception("Invalid batch line: " + line);
	    				}
	    				//--- End Validate line
	    				
	    				inputFile = cols[0];
	    				outputFile = cols[1];
	    				
	    				Extract(inputFile, outputFile, rulePath, language, options);

    				}catch(Exception e) {
    					String message = e.getMessage();
    					if (bLog) {
    						if (isRunable) common.print(inputFile, "Error: " + message);
    						common.writeLog(logPath, inputFile, "Error: " + message, true);
    					}else {
    						common.print(inputFile, "Error: " + message);
    					}

    				}
    			}
    		};
    		tExtract.start();

        }
        catch (Exception ex)
        {
			if (isBatch) {
	            synchronized (_lockerExtract) { iCountThreadExtract--; }
			}
			String message = ex.toString();
			if (bLog) {
				if (isRunable) common.print("Batch line: " + line + ", Error: " + message);
				common.writeLog(logPath, "Batch line: " + line + ", Error: " + message, true);
			}else {
				common.print("Batch line: " + line + ", Error: " + message);
			}
        }
    }
}
