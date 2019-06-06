package com.java.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;
import org.fit.pdfdom.PDFDomTreeConfig;
import org.fit.pdfdom.resource.HtmlResourceHandler;
import org.fit.pdfdom.resource.IgnoreResourceHandler;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.java.classes.Common;
import com.java.classes.HTMLObject;
import com.java.classes.HTMLObject.BolderObject;

import ch.qos.logback.classic.Level;

/**
 * @author      Anonymous
 * @version     1.0
 * @since       1.0
 */
public class PDFExtract {
	/**
	 * Set for build with runnable or not
	 */
	private final boolean runnable = true;
	
	private String logPath = "";
	private String customScript = "";
	private String pdfLanguage = "";
	private boolean writeLogFile = true;
	private boolean loadEngineFail = false;
	private boolean batchMode = false;
	private Object lockerExtract = new Object();
	private int debugMode = 1;
	private int countThreadExtract = 0;
	private Thread threadExtract = null;
	private Invocable scriptEngine = null;
	private List<String> failFunctionList = new ArrayList<String>();
    private Pattern patternP = Pattern.compile("<div class=\"p\"");
    private Pattern patternPage = Pattern.compile("<div class=\"page\"");
    private Pattern patternBlankSpace = Pattern.compile("<div class=\"r\"");
    private Pattern patternImage = Pattern.compile("<img.*");
    private static final String REGEX_TOP = ".*top:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_LEFT = ".*left:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_HEIGHT = ".*height:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_WIDTH = ".*width:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_FONTSIZE = ".*font-size:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_FONTFAMILY = ".*font-family:([a-zA-Z\\s\\-]+).*";
    private static final String REGEX_WORD = ".*>(.*?)<.*";
    private static final String REGEX_COLOR = "(.*color:)(#[a-z]+)(;.*)$";
    private static final String REGEX_SIZE = "(.*font-size:)([0-9.]+)(pt.*)$";
    private static final String REGEX_RESIZE = "(.*font-size:)([0-9.]+)(pt.*)$";
    private static final String REGEX_WORDSPACING = ".*word-spacing:([\\-\\+0-9]+.[0-9]+).*";
	private Common common = new Common();

	private void initial(String logFilePath) throws Exception {
		ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(Level.toLevel("off"));

		if (common.IsEmpty(logFilePath)) {
			writeLogFile = false;
		}else {

			if (common.IsEmpty(common.getExtension(logFilePath))){
				logFilePath += ".log";
			}

			/**
			 * Validate log file
			 */
			boolean logFileValid = common.validateFile(logFilePath);
			if (!logFileValid) {
				throw new Exception("Invalid log file path or permission denied.");
			}
			
			writeLogFile = true;
			
			logPath = logFilePath;

			if (runnable) common.print("Log File: " + logPath);
		}

	}
	
    /**
   	Initializes a newly created PDFExtract object.
    @throws Exception
    */
	public PDFExtract() throws Exception {
		initial("");
	}
	
	/**
   	Initializes a newly created PDFExtract object.
    @param logFilePath The path to write the log file to.
    @throws Exception
    */
	public PDFExtract(String logFilePath) throws Exception {
		initial(logFilePath);
	}
	
	/**
	 * PDFExtract is a PDF parser that converts and extracts PDF content into a HTML format that is optimized for easy alignment across multiple language sources.
	 *
	 * @param inputFile	 The path to the source PDF file process for extraction
	 * @param outputFile The path to the output HTML file after extraction
	 * @param rulePath   The path of a custom set of rules to process joins between lines. If no path is specified, then PDFExtract.js will be loaded from the same folder as the PDFExtract.jar execution. If the PDFExtract.js file cannot be found, then processing will continue without analyzing the joins between lines.
	 * @param language   The language of the file using ISO-639-1 codes when processing. If not specified then the default language rules will be used.
	 * @param options    The control parameters
	 * @param debug      Enable Debug/Display mode. This changes the output to a more visual format that renders as HTML in a browser.
	 * 
	 * @since            1.0
	 */
	public void Extract(String inputFile, String outputFile, String rulePath, String language, String options, int debug) throws Exception {
		
		try {
			if (writeLogFile) {
				if (runnable) common.print(inputFile, "Start extract");
				common.writeLog(logPath, inputFile, "Start extract", false);
			}else {
				common.print(inputFile, "Start extract");
			}
			
			/**
			 * Check input file exists
			 */
			if (!common.IsExist(inputFile)) {
				throw new Exception("Input file does not exist.");
			}
			
			/**
			 * Check input file extension
			 */
			if (!common.getExtension(inputFile).toLowerCase().equals("pdf")) {
				throw new Exception("Input file extension is not pdf.");
			}

			if (runnable) {
				/**
				 * Check input file permission
				 */
				common.checkPermissions(inputFile);
			}
			
			if (!batchMode) {
				pdfLanguage = language;
				debugMode = debug;
			}

			/**
			 * Get output file name without extension
			 */
			String outputName = common.getBaseName(outputFile);
			
			/**
			 * Get output file extension
			 */
			String outputExt = common.getExtension(outputFile);

			/**
			 * Get output directory from outputFile and create it if not exists
			 */
			String outputPath = common.getParentPath(outputFile);
			if (!common.IsEmpty(outputPath) && !common.IsExist(outputPath)) {
				common.createDir(outputPath);
			}

			/**
			 * Read rule script and load into object
			 */
            synchronized (lockerExtract) { 
            	customScript = common.getCustomScript(rulePath, customScript);
            }
			if (common.IsNull(scriptEngine) && !common.IsEmpty(customScript) && !loadEngineFail) {
				synchronized (lockerExtract) {
					if (scriptEngine == null && !loadEngineFail) {
						scriptEngine = common.getJSEngine(customScript);
						if (scriptEngine == null) loadEngineFail = true;
					}
				}
			}

			/**
			 * Call function to convert PDF to HTML
			 */
			String outputHtml = outputName + ".org." + outputExt;
			if (!common.IsEmpty(outputPath)) {
				outputHtml = common.combine(outputPath, outputHtml);
			}
			try {
				convertPdfToHtml(inputFile, outputHtml);
			}catch(Exception e) {
				throw e;
			}
            
			/**
			 * Call function to paint html box
			 */
			//common.writeLog(logPath, inputFile, "Start paintHtmlBox...", false);
			String inputHtmlBox = outputHtml;
			String outputHtmlBox = outputName + ".box." + outputExt;
			if (!common.IsEmpty(outputPath)) {
				outputHtmlBox = common.combine(outputPath, outputHtmlBox);
			}
            try {
        		paintHtmlBox(inputHtmlBox, outputHtmlBox);	
            }catch(Exception e) {
				throw new Exception("paintHtmlBox fail.: " + e.getMessage());
            }
			common.deleteFile(inputHtmlBox);

			
            if (debugMode == 1) {

    			/**
    			 * If debugMode is 1, will write the html box version to output  
    			 */
    			common.moveFile(outputHtmlBox, outputFile);

            }else {

    			/**
    			 * Call function to normalize html  
    			 */
				//common.writeLog(logPath, inputFile, "Start Normalize...", false);
    			String inputNormalize = outputHtmlBox;
    			String outputNormalize = outputName + ".normalized." + outputExt;
    			if (!common.IsEmpty(outputPath)) {
    				outputNormalize = common.combine(outputPath, outputNormalize);
    			}
                try {
                	Normalize(inputNormalize, outputNormalize);
                }catch(Exception e) {
    				throw new Exception("Normalize fail.: " + e.getMessage());
                }
    			common.deleteFile(inputNormalize);
    			
    			/**
    			 * Write to output file.  
    			 */
    			common.moveFile(outputNormalize, outputFile);

            }

			if (writeLogFile) {
				if (runnable) common.print(inputFile, "Extract success. -> " + outputFile + "");
				common.writeLog(logPath, inputFile, "Extract success. -> " + outputFile + "", false);
			}else {
				common.print(inputFile, "Extract success. -> " + outputFile + "");
			}
			
		}catch(Exception e) {
			String message = e.getMessage();
			if (writeLogFile) {
				common.writeLog(logPath, inputFile, "Error: " + message, true);
			}else {
				if (!runnable) common.print(inputFile, "Error: " + message);
			}

			throw e;
		}finally {
			if (batchMode) {
	            synchronized (lockerExtract) { countThreadExtract--; }
			}
		}

	}

	/**
	 * PDFExtract is a PDF parser that converts and extracts PDF content into a HTML format that is optimized for easy alignment across multiple language sources.
	 *
	 * @param batchFile	 The path to the batch file for processing list of files. The input file and output file are specified on the same line delimited by a tab. Each line is delimited by a new line character.
	 * @param rulePath   The path of a custom set of rules to process joins between lines. If no path is specified, then PDFExtract.js will be loaded from the same folder as the PDFExtract.jar execution. If the PDFExtract.js file cannot be found, then processing will continue without analyzing the joins between lines.
	 * @param threadCount The number of threads to run concurrently when processing PDF files. One file can be processed per thread. If not specified, then the default valur of 1 thread is used.
	 * @param language   The language of the file using ISO-639-1 codes when processing. If not specified then the default language rules will be used.
	 * @param options    The control parameters
	 * @param debug      Enable Debug/Display mode. This changes the output to a more visual format that renders as HTML in a browser.
	 * 
	 * @since            1.0
	 */
	public void Extract(String batchFile, String rulePath, int threadCount, String language, String options, int debug) throws Exception {
		try {
			batchMode = true;
			
			if (writeLogFile) {
				if (runnable) common.print("Start extract batch file: " + batchFile);
				common.writeLog(logPath, "Start extract batch file: " + batchFile);
			}else {
				common.print("Start extract batch file: " + batchFile);
			}

			/**
			 * Check input file exists
			 */
			if (!common.IsExist(batchFile)) {
				throw new Exception("Input batch file does not exist.");
			}

			/**
			 * Read rule script and load into object
			 */
			if (common.IsEmpty(customScript)) {
				customScript = common.getCustomScript(rulePath, customScript);
			}
			if (!common.IsEmpty(customScript)) {
				scriptEngine = common.getJSEngine(customScript);
				if (scriptEngine == null) loadEngineFail = true;
			}

			pdfLanguage = language;
			debugMode = debug;
			if (threadCount == 0) threadCount = 1;
	        int maxThreadCount = threadCount;

			/**
			 * Read batch file
			 */	
	        List<String> lines = common.readLines(batchFile);
			
			int ind = 0, len = lines.size();
			while (ind < len) {
				
				if (countThreadExtract < maxThreadCount) {
					String line = lines.get(ind);
					
					AddThreadExtract(ind, line, rulePath, language, options, debug);

					ind++;
				}
				
				Thread.sleep(100);
			}

			/**
			 * Wait for all thread finish
			 */	
			while (countThreadExtract > 0) {
				Thread.sleep(100);
			}
			
		}catch(Exception e) {
			String message = e.getMessage();
			if (writeLogFile) {
				common.writeLog(logPath, message, true);
			}else {
				if (!runnable) common.print("Error: " + e.getMessage());
			}
			throw e;
		}finally {
			if (writeLogFile) {
				if (runnable) common.print("Finish extract batch file: " + batchFile);
				common.writeLog(logPath, "Finish extract batch file: " + batchFile);
			}else {
				common.print("Finish extract batch file: " + batchFile);
			}
		}
	}

	/**
	 * Add new thread pdf extract
	 *
	 * @since 1.0
	 */
    private void AddThreadExtract(int index, String line, String rulePath, String language, String options, int debug)
    {
        try
        {
            String threadName = "ext-" + index;
            
            synchronized (lockerExtract) { countThreadExtract++; }
            threadExtract = new Thread(threadName) {
    			public void run() {

    				String inputFile = "", outputFile = "";
    				try {
    					
    					/**
    					 * Validate line to process
    					 */	
	    				String[] cols = line.split("\t");
	    				
	    				if (cols == null || cols.length < 2) {
	    					throw new Exception("Invalid batch line: " + line);
	    				}
	    				inputFile = cols[0];
	    				outputFile = cols[1];

    					/**
    					 * Call function to extract
    					 */	
	    				Extract(inputFile, outputFile, rulePath, language, options, debug);

    				}catch(Exception e) {
    					String message = e.getMessage();
    					if (writeLogFile) {
    						if (runnable) common.print(inputFile, "Error: " + message);
    						common.writeLog(logPath, inputFile, "Error: " + message, true);
    					}else {
    						common.print(inputFile, "Error: " + message);
    					}

    				}
    			}
    		};
    		threadExtract.start();

        }
        catch (Exception ex)
        {
            synchronized (lockerExtract) { countThreadExtract--; }
			String message = ex.toString();
			if (writeLogFile) {
				if (runnable) common.print("Batch line: " + line + ", Error: " + message);
				common.writeLog(logPath, "Batch line: " + line + ", Error: " + message, true);
			}else {
				common.print("Batch line: " + line + ", Error: " + message);
			}
        }
    }
    

	/**
	 * Convert PDF to HTML file
	 *
	 * @since 1.0
	 */
    private void convertPdfToHtml(String inputFile, String outputFile) throws Exception {
    	PDDocument pdf = null;
		Writer output = null;
		try {
            //loads the PDF file, parse it and gets html file
            pdf = PDDocument.load(new java.io.File(inputFile));
            HtmlResourceHandler handler  = new IgnoreResourceHandler();
            PDFDomTreeConfig config = PDFDomTreeConfig.createDefaultConfig();
            config.setImageHandler(handler);
            PDFDomTree parser = new PDFDomTree(config);
            output = new PrintWriter(outputFile, "utf-8");
            parser.writeText(pdf, output);
		}catch(Exception e) {
			throw new Exception("Convert pdf to html fail.: " + e.getMessage());
		}finally {
			if (pdf != null) { pdf.close(); pdf = null; }
			if (output != null) { output.close(); output = null; }
		}
    }
    
	/**
	 * Paint HTML box
	 *
	 * @since 1.0
	 */
    private void paintHtmlBox(String inputFile, String outputFile) throws Exception {
        BufferedReader b_in = null;
        HashMap<Integer, List<HtmlTagValues>> hList = new HashMap<Integer, List<HtmlTagValues>>();
        int hPage = -1;
        try {
			
	        InputStreamReader i_in = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);
	        b_in = new BufferedReader(i_in);
	        double pageWidth = 0;
	        double pageHeight = 0;
	        String line = "";

	        while ((line = b_in.readLine()) != null) {
	            double maxFont = 0;
	            double maxTop = 0;
	            double maxLeft = 0;
	            Matcher m = this.patternP.matcher(line);
	            Matcher m1 = this.patternPage.matcher(line);
	
	            if (m.find()) {
	
	                HtmlTagValues v = new HtmlTagValues();
	
	                v.Top = line.replaceAll(REGEX_TOP, "$1");
	                v.Left = line.replaceAll(REGEX_LEFT, "$1");
	                v.Height = line.replaceAll(REGEX_HEIGHT, "$1");
	                v.Width = line.replaceAll(REGEX_WIDTH, "$1");
	                v.FontFamily = line.replaceAll(REGEX_FONTFAMILY, "$1");
	                v.FontSize = line.replaceAll(REGEX_FONTSIZE, "$1");
	                v.Word = line.replaceAll(REGEX_WORD, "$1");
	                v.WordSpacing = line.replaceAll(REGEX_WORDSPACING, "$1");
	                v.Color = line.replaceAll(REGEX_COLOR, "$1");
	                if (maxTop < Double.valueOf(v.Top)) {
	                    v.maxTop = v.Top;
	                }
	                if (maxFont < Double.valueOf(v.FontSize)) {
	                    v.maxFont = v.FontSize;
	                }
	                if (maxLeft < Double.valueOf(v.Left)) {
	                    v.maxLeft = v.Left;
	                }
	                
	                if (!checkLineAdd(pageWidth, pageHeight, v)) {
	                    //
	                    continue;
	                }
	
	                hList.get(hPage).add(v);
	                
	            } else if (m1.find()) {

	            	hPage++;
	            	hList.put(hPage, new ArrayList<HtmlTagValues>());

	            	pageWidth = Double.valueOf(line.replaceAll(REGEX_WIDTH, "$1"));
	                pageHeight = Double.valueOf(line.replaceAll(REGEX_HEIGHT, "$1"));
	            }
	        }
        }finally {
        	if (b_in != null) {
        		b_in.close();
        		b_in = null;
        	}
        }
        
        try{
        	LinkedHashMap<Integer, ArrayList<String>> model = getBox(hList);
        	drawBox(inputFile, outputFile, model);
        }catch(Exception e) {
        	e.printStackTrace();
        	throw new Exception("error drawing: " + e.getMessage());
        }

    }

	/**
	 * HTML Tag Values
	 *
	 * @since 1.0
	 */
    private class HtmlTagValues {

        public String Top;
        public String Height;
        public String Width;
        public String Left;
        public String FontFamily;
        public String FontSize;
        public String Word;
        public String id;
        public String maxFont;
        public String maxTop;
        public String maxLeft;
        public String WordSpacing;
        public String Color;
    }

    private boolean checkLineAdd(double pageWidth, double pageHeight, String line) {
        HtmlTagValues v = new HtmlTagValues();
    	
        v.Top = line.replaceAll(REGEX_TOP, "$1");
        v.Left = line.replaceAll(REGEX_LEFT, "$1");
        v.Height = line.replaceAll(REGEX_HEIGHT, "$1");
        v.Width = line.replaceAll(REGEX_WIDTH, "$1");
        v.FontFamily = line.replaceAll(REGEX_FONTFAMILY, "$1");
        v.FontSize = line.replaceAll(REGEX_FONTSIZE, "$1");
        v.Word = line.replaceAll(REGEX_WORD, "$1");
        v.WordSpacing = line.replaceAll(REGEX_WORDSPACING, "$1");
        v.Color = line.replaceAll(REGEX_COLOR, "$1");

        return checkLineAdd(pageWidth, pageHeight, v);
    }
    
    private boolean checkLineAdd(double pageWidth, double pageHeight, HtmlTagValues v) {

        if (Double.valueOf(v.Left) < 0 || Double.valueOf(v.Top) < 0 || Double.valueOf(v.Left) > pageWidth || Double.valueOf(v.Top) > pageHeight) {
        	return false;
        }else {
        	return true;	
        }
    }

	/**
	 * Draw box to html 
	 *
	 * @since 1.0
	 */
    private void drawBox(String inputFile, String outputFile, LinkedHashMap<Integer, ArrayList<String>> model) throws IOException {

    	BufferedReader b_in = null;
    	BufferedWriter b_out = null;
    	try {
	        InputStreamReader i_in = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);
	        b_in = new BufferedReader(i_in);
	        FileOutputStream f_out = new FileOutputStream(outputFile);
	        OutputStreamWriter o_out = new OutputStreamWriter(f_out, StandardCharsets.UTF_8);
	        b_out = new BufferedWriter(o_out);

	        double pageWidth = 0;
	        double pageHeight = 0;
	        int nPage = 1;
	        int wordIterator = 1;
	        String line;
	        while ((line = b_in.readLine()) != null) {
	            Matcher m = this.patternP.matcher(line);
	            Matcher m1 = this.patternPage.matcher(line);
	            Matcher m2 = this.patternBlankSpace.matcher(line);
	            Matcher m3 = this.patternImage.matcher(line);
	
	            if (m2.find() || m3.find()) {
	                line = line.replaceAll(".*src.*>", "");
	                line = line.replaceAll("<div class=\"r\" style.*", "");
	            } else if (m1.find()) {
	                b_out.write(line + "\n");
	                if (nPage <= model.size() && model.size() > 0) {
	                    for (int x = 0; x < model.get(nPage).size(); x++) {
	                        b_out.write(model.get(nPage).get(x) + "\n");
	                    }
	                    nPage++;
	                }
	                pageWidth = Double.valueOf(line.replaceAll(REGEX_WIDTH, "$1"));
	                pageHeight = Double.valueOf(line.replaceAll(REGEX_HEIGHT, "$1"));
	            } else if (m.find()) {
	                if (checkLineAdd(pageWidth, pageHeight, line)) {
		                double percent = 0.11;
		                String size = line.replaceAll(REGEX_SIZE, "$2");
		                double a = Double.parseDouble(size);
		                double calSIZE = a - (a * percent);
		                if (wordIterator == 1) {
		                    line = line.replaceAll(REGEX_RESIZE, "$1" + calSIZE + "$3");
		                    wordIterator = 0;
		                } else {
		                    line = line.replaceAll(REGEX_RESIZE, "$1" + calSIZE + "$3"); 
		                    wordIterator = 1;
		                }
		                line = line.replaceAll(REGEX_COLOR, "$1" + "#000000" + "$3");
		                b_out.write(line + "\n");
	                }
	            } else {
	                b_out.write(line + "\n");
	            }
	        }
    	}finally {

    		if (b_in != null) {
    			b_in.close();
    			b_in = null;
    		}
            
    		if (b_out != null) {
    			b_out.close();
    			b_out = null;
    		}
            	
    	}
    }

	/**
	 * Get box to draw 
	 *
	 * @since 1.0
	 */
    private LinkedHashMap<Integer, ArrayList<String>> getBox(HashMap<Integer, List<HtmlTagValues>> hList) {

        int BOX = 1;
        double columnTop = 0;
        double columnLeft = 0;
        double columnHeight = 0;
        double columnWidth = 0;
        double lineTop = 0;
        double lineLeft = 0;
        double lineHeight = 0;
        double lineWidth = 0;
        int round = 0;
        int columnCount = 0;
        double nextTop = 0;
        double previousTop = 0;
        //
        double paraTop = 0;
        double paraLeft = 0;
        double paraHeight = 0;
        double paraWidth = 0;
        int paraRound = 0;
        int paraCount = 0;
        double gapLine = 0;
        boolean clearPara = false;

        //model
        LinkedHashMap<Integer, ArrayList<String>> model = new LinkedHashMap<Integer, ArrayList<String>>();
        ArrayList<String> objm = new ArrayList<String>();
        //line
        ArrayList<Double> objline = new ArrayList<Double>();
        ArrayList<Double> objlineleft = new ArrayList<Double>();
        ArrayList<Double> objpara = new ArrayList<Double>();
        ArrayList<Double> objparaleft = new ArrayList<Double>();
        
        for (int key : hList.keySet() ) {
            //
        	List<HtmlTagValues> tagList = hList.get(key);

        	for (int i=0, len=tagList.size(); i<len; i++) {
        	
                HtmlTagValues v = tagList.get(i);
                HtmlTagValues v_next = (i+1 < tagList.size() ? tagList.get(i + 1) : null);
                HtmlTagValues v_pre = (i > 0 ? tagList.get(i - 1) : null);
                HtmlTagValues v_column = (i - columnCount >= 0 ? tagList.get(i - columnCount) : null);
                HtmlTagValues v_para = (i - paraCount >= 0 ? tagList.get(i - paraCount) : null);
                gapLine = 0;
                //
                if (i == 0){
                	//come first
                    lineTop = Double.valueOf(v.Top);
                    lineLeft = Double.valueOf(v.Left);
                    lineHeight = Double.valueOf(v.Height);
                }
                //
                if (v_pre != null) {
                    previousTop = Math.abs(Double.valueOf(v_pre.Top) - Double.valueOf(v.Top));

                    if (previousTop < 2) previousTop = 0;
                }
                if (v_next != null) {
                    nextTop = Math.abs(Double.valueOf(v.Top) - Double.valueOf(v_next.Top));
                    
                    if (nextTop > 0) {
                    	gapLine = Math.abs(Double.valueOf(v.Top) + Double.valueOf(v.Height) - Double.valueOf(v_next.Top));	
                    }

                    if (nextTop < 2) nextTop = 0;
                }

                //for line
                if (v_pre != null && previousTop > 0) {
                    lineTop = Double.valueOf(v.Top);
                    lineLeft = Double.valueOf(v.Left);
                    lineHeight = Double.valueOf(v.Height);
                    //
                }
                if (v_next != null && nextTop == 0) {
                	if (v_next != null) {
                		double gap =  Math.abs(Math.abs(Double.valueOf(v.Left) - Double.valueOf(v_next.Left)) - Double.valueOf(v.Width));
                		lineWidth += (Double.valueOf(v.Width) + gap);

                		double minLeft = Math.min(Double.valueOf(v.Left), Double.valueOf(v_next.Left));
                		if (lineLeft > minLeft) {
                			lineWidth -= (lineLeft - minLeft); 
                		}
                		lineLeft = Math.min(Math.min(lineLeft, Double.valueOf(v.Left)), Double.valueOf(v_next.Left));
                	}
                    
                } else {
                    lineWidth += Double.valueOf(v.Width);
                    objm.add(getDIV(lineTop, lineLeft, lineHeight, lineWidth, "blue"));

                    //for column
                    if (objlineleft != null && objlineleft.size() > 0 && lineLeft > Collections.min(objlineleft)) {
                        double dLineWidth = (lineLeft - Collections.min(objlineleft) + 1) + lineWidth;
                        objline.add(dLineWidth);
                    }else {
                    	objline.add(lineWidth);
                    }
                    
                    //for paragraph
                    if (objparaleft != null && objparaleft.size() > 0 && lineLeft > Collections.min(objparaleft)) {
                        double dLineWidth = (lineLeft - Collections.min(objparaleft) + 1) + lineWidth;
                        objpara.add(dLineWidth);
                    }else {
                    	objpara.add(lineWidth);	
                    }
                    
                    objlineleft.add(lineLeft);
                    objparaleft.add(lineLeft);

                    lineWidth = 0;
                    //
                }


                //if new paragraph, all variables = first line
                if (paraRound == 0) {
                	paraTop = lineTop;
                	paraLeft = lineLeft;
                	paraHeight = lineHeight;
                	paraWidth = lineWidth;
                	clearPara = false;
                	paraRound = 1;
                } //else compare current parawidth with previous parawidth and increase para height
                else {
                    if (objpara.size() == 0) {
                    	paraWidth = lineWidth;
                    	paraLeft = lineLeft;
                    } else {
                    	paraWidth = Collections.max(objpara);
                    	paraLeft = Collections.min(objparaleft);
                    }
                    if (v_pre != null) {
                    	paraHeight += Math.abs(Double.valueOf(v_pre.Top) - Double.valueOf(v.Top));
                    }
                }
                //
                //
                if ((nextTop > 0 && gapLine > Double.valueOf(v.Height)/2)
                	|| (v_next == null || (nextTop > Double.valueOf(v.Height)/2 && !v.FontFamily.contains("Bold") && v_next.FontFamily.contains("Bold")))
            		) {

                    if (v_para != null) {
                    	paraWidth = Collections.max(objpara);
                    	if (paraWidth == 0.0) paraWidth = Double.valueOf(v_para.Width);
                    	paraHeight = Math.abs(Double.valueOf(v_para.Top) - Double.valueOf(v.Top)) + Double.valueOf(v.Height);
	                    objm.add(getDIV(paraTop, paraLeft, paraHeight, paraWidth, "green"));
                    }

                    clearPara = true;
                } else {
                    paraCount += 1;
                }
                
                //if new column, all variables = first line
                if (round == 0) {
                    columnTop = lineTop;
                    columnLeft = lineLeft;
                    columnHeight = lineHeight;
                    columnWidth = lineWidth;
                    round = 1;
                } //else compare current columnwidth with previous columnwidth and increase column height
                else {
                    if (objline.size() == 0) {
                        columnWidth = lineWidth;
                        columnLeft = lineLeft;
                    } else {
                        columnWidth = Collections.max(objline);
                        columnLeft = Collections.min(objlineleft);
                        //columnWidth = Math.max(columnWidth, (lineLeft - columnLeft) + lineWidth);
                    }
                    if (v_pre != null) {
                    	columnHeight += Math.abs(Double.valueOf(v_pre.Top) - Double.valueOf(v.Top));
                    }
                }
                
                if ((nextTop > 0 && (v_next == null || (gapLine > Math.min(Double.valueOf(v.Height), Double.valueOf(v_next.Height))*1.8)))
                		|| (v_next == null || (nextTop > lineHeight*5))
                		|| (v_next == null || (nextTop > lineHeight  && !v.FontFamily.contains("Bold") && v_next.FontFamily.contains("Bold") && Double.valueOf(v.FontSize) != Double.valueOf(v_next.FontSize) && Double.valueOf(v.Left) != Double.valueOf(v_next.Left) && !v.Color.equals(v_next.Color) && Double.valueOf(v.FontSize) > 20))
                		|| (v_next == null || (nextTop > lineHeight*3  && !v.FontFamily.contains("Bold") && v_next.FontFamily.contains("Bold") && Double.valueOf(v.FontSize) != Double.valueOf(v_next.FontSize) && Double.valueOf(v.Left) != Double.valueOf(v_next.Left)))
                		|| (v_next == null || (nextTop > lineHeight*2 && gapLine > 0 && Double.valueOf(v.FontSize) > 20 && Double.valueOf(v_next.FontSize) > 20 && !v.FontFamily.contains("Bold") && v_next.FontFamily.contains("Bold") && Double.valueOf(v.FontSize) != Double.valueOf(v_next.FontSize)))
                		|| (v_next == null || (nextTop > lineHeight*3 && Double.valueOf(v.FontSize) <= 20 && Double.valueOf(v_next.FontSize) <= 20 && !v.FontFamily.contains("Bold") && v_next.FontFamily.contains("Bold") && Double.valueOf(v.FontSize) != Double.valueOf(v_next.FontSize)))
                	) {
                	
                    if (v_column != null) {
                    	if (objline != null && objline.size() > 0) {
                    		columnWidth = Collections.max(objline);	
                    	}else {
                    		columnWidth = lineWidth;
                    	}
                        columnHeight = Math.abs(Double.valueOf(v_column.Top) - Double.valueOf(v.Top)) + Double.valueOf(v.Height);
                        objm.add(getDIV(columnTop, columnLeft, columnHeight, columnWidth, "red"));
                    }
                    objline.removeAll(objline);
                    objlineleft.removeAll(objlineleft);
                    columnTop = 0;
                    columnLeft = 0;
                    columnWidth = 0;
                    columnHeight = 0;
                    lineTop = 0;
                    lineLeft = 0;
                    lineWidth = 0;
                    lineHeight = 0;
                    round = 0;
                    columnCount = 0;
                } else {
                    columnCount += 1;
                }
                
                if (clearPara) {

                    objpara.removeAll(objpara);
                    objparaleft.removeAll(objparaleft);
                    paraTop = 0;
                    paraLeft = 0;
                    paraWidth = 0;
                    paraHeight = 0;
                    paraRound = 0;
                    paraCount = 0;
                    
                    clearPara = false;
                }

                if (i == len - 1) {
                	
                	if (v_next != null && lineHeight > 1) {
                		lineWidth += Double.valueOf(v_next.Width);
                		paraWidth += lineWidth;
                		columnWidth += lineWidth;
                        objm.add(getDIV(lineTop, lineLeft, lineHeight, lineWidth, "blue"));
                	}
                    
                    if (paraRound > 0) {
	                    if (v_para != null) {
	                    	if (paraWidth == 0.0) paraWidth = Double.valueOf(v_para.Width);
	                    	paraHeight = Math.abs(Double.valueOf(v_para.Top) - Double.valueOf(v.Top)) + Double.valueOf(v.Height);
		                    objm.add(getDIV(paraTop, paraLeft, paraHeight, paraWidth, "green"));
	                    }
                    }
                    
                    if (round > 0) {
                        if (v_column != null) {
                        	columnHeight = Math.abs(Double.valueOf(v_column.Top) - Double.valueOf(v.Top)) + Double.valueOf(v.Height);
                            objm.add(getDIV(columnTop, columnLeft, columnHeight, columnWidth, "red"));
                        }
                    }
                    
                    model.put(BOX, objm);
                    BOX++;
                    objm = new ArrayList<String>();
                    objline.removeAll(objline);
                    objlineleft.removeAll(objlineleft);
                    columnTop = 0;
                    columnLeft = 0;
                    columnWidth = 0;
                    columnHeight = 0;
                    paraTop = 0;
                    paraLeft = 0;
                    paraWidth = 0;
                    paraHeight = 0;
                    paraRound = 0;
                    paraCount = 0;
                    objpara.removeAll(objpara);
                    objparaleft.removeAll(objparaleft);
                    lineTop = 0;
                    lineLeft = 0;
                    lineWidth = 0;
                    lineHeight = 0;
                    round = 0;
                    columnCount = 0;
                }
            }
        }
        
        return model;
    }
    private String getDIV(double TOP, double LEFT, double HEIGHT, double WIDTH, String color) {
    	String DIV = "<div class=\"p\" style=\"border: 1pt solid;top:" + TOP + "pt;left:" + LEFT + "pt;height:" + HEIGHT + "pt;width:" + WIDTH + "pt;background-color:transparent;color:" + color + ";\"></div>";
        return DIV;
    }

	/**
	 * Normalize html
	 *
	 * @since 1.0
	 */
    private void Normalize(String inputFile, String outputFile) throws Exception
	{
		FileInputStream fs = null;	
		BufferedReader br = null;
		try {

			fs = new FileInputStream(inputFile);
			br = new BufferedReader(new InputStreamReader(fs, "UTF-8"));
			StringBuilder sbPageAll = new StringBuilder();
			StringBuilder sbContentPage = new StringBuilder();
			String sLine = "";
			String sStartPage = "<div class=\"page\" id=\"page_";
			String sEndBody = "</body>";
			boolean bFoundFirstPage = false;
			int iPageID = 0;
			AtomicReference<Hashtable<String,Integer>> hashClasses = new AtomicReference<Hashtable<String,Integer>>();
			

			while ((sLine = br.readLine()) != null) {				
				if (sLine.indexOf(sStartPage) == -1 && !bFoundFirstPage) continue; 
				else { bFoundFirstPage = true;}
				
				if (sLine.indexOf(sStartPage) == -1 && sLine.indexOf(sEndBody) == -1)
						sbContentPage.append(sLine + "\n");

				/**
				 * New page found
				 */
				if (sLine.indexOf(sStartPage) != -1)
				{

					if (sbContentPage.toString().length() > 0) {
						/**
						 * End previous page
						 */
						iPageID++;
						String sPageContent = sbContentPage.toString();
						String pageContent = GetPageNormalizedHtml(sPageContent,iPageID,hashClasses);
						sbPageAll.append(pageContent);		
						sbContentPage = new StringBuilder();
					}
					
					sbContentPage.append(sLine + "\n");
				}
				else if (sLine.indexOf(sEndBody) != -1)
				{
					/**
					 * End last page
					 */
					iPageID++;
					String sPageContent = sbContentPage.toString();
					sbPageAll.append(GetPageNormalizedHtml(sPageContent,iPageID,hashClasses));	
					break;
				}
			}
			
			/** Get classes
			- page - the wrapping boundary of a page. 
			- header - the wrapping boundary of the page header.
			- footer - the wrapping boundary of the page footer.
			- column - the wrapping boundary of a column.
			- line - the wrapping boundary of the paragraph line.
			- h1 - the wrapping boundary of a the paragraph h1.
			- h2 - the wrapping boundary of a the paragraph h2.		
			 */
			
			AtomicReference<StringBuilder> sbPageAllNomalize = new AtomicReference<StringBuilder>();
			sbPageAllNomalize.set(sbPageAll);
			String sClasses = GetNormalizeClasses(hashClasses.get(),sbPageAllNomalize);

		    FileUtils.writeStringToFile(new File(outputFile),
		    		GetTemplate(Tempate.body.toString())
		    		.replace("[" + TempateContent.STYLE.toString() + "]",sClasses)
		    		.replace("[" + TempateContent.BODY.toString() + "]",sbPageAllNomalize.get().toString())
		    		,"utf-8");
			
		} catch (Exception e1) {
			e1.printStackTrace();
			throw e1;
		}
		finally 
		{
			try {
				if (br != null) br.close();
				if (fs != null) fs.close();
			} catch (Exception e2) {
				throw e2;
			}
		}
	}

	/**
	 * Get normalize classes
	 *
	 * @since 1.0
	 */
	private String GetNormalizeClasses(Hashtable<String,Integer> hashClasses,AtomicReference<StringBuilder> sbPageAll)
	{
		StringBuilder _sbPageAll = sbPageAll.get();
		StringBuilder sbClasses = new StringBuilder();
		
		Hashtable<Float,String> hashFontSize = new Hashtable<Float,String>();

		String sGlobalStyle = "";
		int iGlobalStyle = 0;
		List<String> listClasse = new ArrayList<String>(hashClasses.keySet());	
		for (String classes : listClasse) {
			if (hashClasses.get(classes) > iGlobalStyle)
			{
				iGlobalStyle = hashClasses.get(classes);
				sGlobalStyle = classes;
			}
		}
		
		for (String classes : listClasse) {
			if (!sGlobalStyle.equals(classes))
			{
				float fFountSize = Float.parseFloat(classes.replaceAll(REGEX_FONTSIZE, "$1"));
				for (int i = 0; i < 10; i++) {
					if (hashFontSize.containsKey(fFountSize))
					{
						fFountSize = (float) (fFountSize + 0.5);
					}
					else
						break;
				}
				hashFontSize.put(fFountSize, classes);
			}
		}
		//(font\-family\:CAAAAA\+DejaVuSans\-Bold;font\-size:12\.46pt;font\-weight:bold;)
		
		
		//replace global style
		_sbPageAll = new StringBuilder(_sbPageAll.toString().replaceAll("\\n+", "\n").replace(sGlobalStyle, ""));
		
		sbClasses.append("body {");
		sbClasses.append(sGlobalStyle);
		sbClasses.append("}");
		sbClasses.append("\np{");
		sbClasses.append("border:1px solid green;");
		sbClasses.append("}");
		sbClasses.append("\n.page{"); 
		sbClasses.append("border:1px dashed silver;");
		sbClasses.append("}");
		sbClasses.append("\n.header{");
		sbClasses.append("border:1px solid pink;");
		sbClasses.append("}");
		sbClasses.append("\n.footer{");
		sbClasses.append("border:1px solid yellow;");
		sbClasses.append("}");
		sbClasses.append("\n.column{");
		sbClasses.append("border:1px solid red;");
		sbClasses.append("}");
		sbClasses.append("\n.line{");
		sbClasses.append("border:0.5px solid blue;");
		sbClasses.append("}");


		List<Float> listFontSize = new ArrayList<Float>(hashFontSize.keySet());	
		Collections.sort(listFontSize);
		int iHCount = listFontSize.size();
		for (float fontsize : listFontSize) {
			
			sbClasses.append("\n.h"+iHCount+" {");
			sbClasses.append(hashFontSize.get(fontsize));
			sbClasses.append("}");
			//replace h style
			_sbPageAll = new StringBuilder(_sbPageAll.toString().replace(" </span>", "</span>").replaceAll("(class=\")(line)(\" style=\"[^<>]+)("+hashFontSize.get(fontsize).replace("-", "\\-").replace("+", "\\+").replace(".", "\\.").replace(" ", "[ ]")+")(\")", "$1$2 h"+iHCount+"$3$5"));
			iHCount--;
		}
		//sbClasses.append("}");
		
		sbPageAll.set(_sbPageAll);
		
		return sbClasses.toString();		
	}

	/**
	 * Get page normalize html
	 *
	 * @since 1.0
	 */
	private String  GetPageNormalizedHtml (String sContent,int iPageID,AtomicReference<Hashtable<String,Integer>> hashClasses)
	{
		String sPageNormalized = "";
		
		//Initial objects
		Hashtable<Key,HTMLObject.BolderObject> hashColumn = new  Hashtable<Key,HTMLObject.BolderObject>();
		Hashtable<Key,HTMLObject.BolderObject> hashParagraph = new  Hashtable<Key,HTMLObject.BolderObject>();
		Hashtable<Key,HTMLObject.BolderObject> hashLine = new  Hashtable<Key,HTMLObject.BolderObject>();
		Hashtable<Float,Hashtable<Integer,HTMLObject.TextObject>> hashText = new  Hashtable<Float,Hashtable<Integer,HTMLObject.TextObject>>();
		Hashtable<String,Integer> _hashClasses = hashClasses.get();
		if (_hashClasses == null) _hashClasses = new Hashtable<String,Integer>();
		
		
		// Get Border
		Pattern pBorder = Pattern.compile("<div[ ]class=\"p\"[ ]style=\"border:[ ]1pt[ ]solid;top:(.*?)pt;left:(.*?)pt;height:(.*?)pt;width:(.*?)pt[^<>]+(red|green|blue);\"></div>", Pattern.MULTILINE);
		Matcher mBorder = pBorder.matcher(sContent);
		while (mBorder.find()) {
			HTMLObject.BolderObject oBolder = new HTMLObject.BolderObject();
			oBolder.top = RoundNumber(mBorder.group(1));
			oBolder.left = RoundNumber(mBorder.group(2));
			oBolder.height = RoundNumber(mBorder.group(3));
			oBolder.width = RoundNumber(mBorder.group(4));
			oBolder.right = oBolder.left + oBolder.width;
			oBolder.bottom = oBolder.top + oBolder.height;
			String sColor = mBorder.group(5);
			oBolder.color = sColor;
			if (sColor.equals("red"))
				hashColumn.put(new Key(oBolder.top,oBolder.left), oBolder);				
			else if (sColor.equals("green"))
				hashParagraph.put(new Key(oBolder.top,oBolder.left), oBolder);
			else if (sColor.equals("blue"))
				hashLine.put(new Key(oBolder.top,oBolder.left), oBolder);
		}	
		
		//Case not handle paragrapBorder 
		if (hashParagraph.size() == 0)
		{
			hashParagraph = (Hashtable<Key, BolderObject>) hashColumn.clone();
		}
		
		// Get Text
		//top:709.0735pt;left:135.07628pt;line-height:8.099976pt;font-family:Times;font-size:8.01pt;word-spacing:3.354201pt;color:#323232;width:39.49199pt;
		Pattern pText = Pattern.compile("<div[ ]class=\"p\"[ ]id=\".*?\"[ ]style=\"top:(?<top>.*?)pt;left:(?<left>.*?)pt;line-height:(?<lineheight>.*?)pt;font-family:(?<fontfamily>.*?);font-size:(?<fontsize>.*?)pt;(font-weight:(?<fontweight>.*?);)?(letter-spacing:(?<letterspacing>.*?);)?(font-style:(?<fontstyle>.*?);)?(word-spacing:(?<wordspacing>.*?)pt;)?(color:(?<color>.*?);)?width:(?<width>.*?)pt;\">(?<text>.*?)</div>", Pattern.MULTILINE);
		Matcher mText = pText.matcher(sContent);
		int iSeq = 0;
		while (mText.find()) {
			iSeq++;
			HTMLObject.TextObject oText = new HTMLObject.TextObject();
			oText.top = RoundNumber(mText.group("top"));
			oText.left = RoundNumber(mText.group("left"));
			oText.lineheight = RoundNumber(mText.group("lineheight"));
			oText.right = oText.left + oText.width;
			oText.bottom = oText.top + oText.lineheight;
			String sfontfamily = mText.group("fontfamily");
			String sfontsize =  mText.group("fontsize");
			String sfontweight = "";
			if (mText.group("fontweight") != null)
				sfontweight = mText.group("fontweight");
			String sfontstyle = "";
			if (mText.group("fontstyle") != null)
				sfontstyle = mText.group("fontstyle");
			String sletterspacing = "";
			if (mText.group("letterspacing") != null)
				sletterspacing = mText.group("letterspacing");
			/*
			String swordspacing = "";
			if (mText.group("wordspacing") != null)
				swordspacing = mText.group("wordspacing");
			String scolor = "";
			if (mText.group("color") != null)
				scolor = mText.group("color");
			*/
			oText.width = Float.parseFloat(mText.group("width"));
			oText.text = mText.group("text");
			
			//get font style
			String sFontStyle = "font-family:" + sfontfamily + ";" + "font-size:" + sfontsize + "pt;";			
			if (mText.group("fontweight") != null)
				sFontStyle += "font-weight:" + sfontweight + ";";
			
			if (mText.group("fontstyle") != null)
				sFontStyle += "font-style:" + sfontstyle + ";";
		
			oText.style = sFontStyle.replaceAll("(letter\\-spacing:(.*?)pt;)", "");
					
			if (hashText.containsKey(oText.top))
			{
				Hashtable<Integer,HTMLObject.TextObject> hashTextMember  = hashText.get(oText.top);
				hashTextMember.put(iSeq, oText);
				hashText.put(oText.top, hashTextMember);
			}
			else
			{
				Hashtable<Integer,HTMLObject.TextObject> hashTextMember = new Hashtable<Integer,HTMLObject.TextObject>();
				hashTextMember.put(iSeq, oText);
				hashText.put(oText.top, hashTextMember);
			}
		}
		
		hashClasses.set(_hashClasses);

		List<Key> listColumnLeft = new ArrayList<Key>(hashColumn.keySet());	
		Collections.sort(listColumnLeft, new KeyComparator());
	    String sColumnAll = "";
	    int iColumnID = 1; 	    
	    
	    //Loop each column
	    for (Key columnKey : listColumnLeft) {

	    	//System.out.println("column"+iColumnID+"==>" + columnKey.x + ":"+columnKey.y);
	    	
		    String sColumnParagraphAll = "";
		    int iParagraphID = 1;
	    	
	    	HTMLObject.BolderObject oBolderColumn = hashColumn.get(columnKey);
	    	
	    	List<Key> listParagraphTop = new ArrayList<Key>(hashParagraph.keySet());
			Collections.sort(listParagraphTop, new KeyComparator());
		    
		  //Loop each paragraph in column
		    for (Key paragraphKey : listParagraphTop) {
		    	
		    	String sColumnParagraphLineAll = "";
		    	HTMLObject.BolderObject oBolderParagraph = hashParagraph.get(paragraphKey);
		    	if (CheckIsBoxInside(oBolderColumn,oBolderParagraph))
		    	{

			    	//System.out.println("paragraph"+iParagraphID+"==>" + paragraphKey.x + ":"+paragraphKey.y);
			    	
			    	List<Key> listLineTop = new ArrayList<Key>(hashLine.keySet());
				    Collections.sort(listLineTop, new KeyComparator());
				    int iLineID = 1;

				    //Loop each line in paragraph
				    for (Key lineKey : listLineTop) {
					    
				    	HTMLObject.BolderObject oBolderLine = hashLine.get(lineKey);
				    
				    	// <div class="p" id="p15" style="top:107.91202pt;left:45.354687pt;line-height:8.100006pt;font-family:Times;font-size:8.01pt;word-spacing:2.083961pt;color:#323232;width:23.994pt;">Lorem</div>				    
				    	
				    	if (CheckIsBoxInside(oBolderParagraph,oBolderLine))
				    	{
				    		
				    		Hashtable<Float,String> hashTextInLine = new  Hashtable<Float,String>();
				    		
					    	//System.out.println("\tline"+iLineID+"==>" + lineKey.x + ":"+lineKey.y);
					    	
					    	List<Float> listText = new ArrayList<Float>(hashText.keySet());
						    Collections.sort(listText);

						    String sLine = "";
						    String sStyle = "";
						    for (Float textTop : listText) {
						    	if (textTop >= oBolderLine.top && textTop <= oBolderLine.bottom)
						    	{
							    	Hashtable<Integer,HTMLObject.TextObject> hashTextMember = hashText.get(textTop);		    	
							    	List<Integer> listTextMember = new ArrayList<Integer>(hashTextMember.keySet());
								    Collections.sort(listTextMember);
								    for (Integer textMemberTop : listTextMember) {
								    	HTMLObject.TextObject oTextMemberObject = hashTextMember.get(textMemberTop);
								    	if (oTextMemberObject.left >= oBolderLine.left && oTextMemberObject.right <= oBolderLine.right)
								    	{
								    		sStyle = oTextMemberObject.style;
								    		if (!hashTextInLine.containsKey(oTextMemberObject.left))
								    			hashTextInLine.put(oTextMemberObject.left, oTextMemberObject.text);								    		
								    	}
								    }
						    	}
						    }	
						    List<Float> listTextInLine = new ArrayList<Float>(hashTextInLine.keySet());
							Collections.sort(listTextInLine);
							
							 for (Float textInLineKey : listTextInLine) {
								sLine += hashTextInLine.get(textInLineKey) + " ";
							}
							// System.out.println(sLine);
						    
							//add counting in classes object
							if (_hashClasses.containsKey(sStyle))
								_hashClasses.put(sStyle, _hashClasses.get(sStyle)+1);
							else
								_hashClasses.put(sStyle, 1);
							
						    String sColumnParagraphLine = GetTemplate(Tempate.columnparagraphline.toString())
						    		.replace("[" + TempateID.COLUMNPARAGRAPHLINEID.toString() + "]"  , String.valueOf(iPageID) + "c" + String.valueOf(iColumnID) + "p" + String.valueOf(iParagraphID) + "l" + String.valueOf(iLineID))
						    		.replace("[" + TempateStyle.TOP.toString() + "]" , String.valueOf(oBolderLine.top))
						    		.replace("[" + TempateStyle.LEFT.toString() + "]" , String.valueOf(oBolderLine.left))
						    		.replace("[" + TempateStyle.HEIGHT.toString() + "]" , String.valueOf(oBolderLine.height))
						    		.replace("[" + TempateStyle.WIDTH.toString() + "]" , String.valueOf(oBolderLine.width))
						    		.replace("[" + TempateStyle.LINESTYLE.toString() + "]" , sStyle)
						    		.replace("[" + TempateContent.COLUMNPARAGRAPHLINE.toString() + "]" , sLine);
						    
						    //Analyze Joins 
						    if (scriptEngine != null && sColumnParagraphLine.length() > 0) {

				    			String sResult = common.getStr(invokeJS("repairObjectSequence", sColumnParagraphLine ));
				    			if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length )
				    			{
				    				sColumnParagraphLine = sResult;
				    			}		    			
				            }	
						    
						    sColumnParagraphLineAll += sColumnParagraphLine + "\n";
						  
					    	iLineID++;
				    	}
				    }
				    				   
				    
				    if (scriptEngine != null && sColumnParagraphLineAll.length() > 0) {
	    	
				    	//call analyzeJoins
		    			String sResult = common.getStr(invokeJS("analyzeJoins", sColumnParagraphLineAll , pdfLanguage));
		    			if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length )
		    			{
		    				sColumnParagraphLineAll = sResult;
		    			}	
		    			
		    			//call isHeader
		    			sResult = common.getStr(invokeJS("isHeader", sColumnParagraphLineAll));
		    			if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length )
		    			{
		    				sColumnParagraphLineAll = sResult;
		    			}	
		    			
		    			//call isFooter
		    			sResult = common.getStr(invokeJS("isFooter", sColumnParagraphLineAll));
		    			if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length )
		    			{
		    				sColumnParagraphLineAll = sResult;
		    			}
		            }	
				    
				    String sColumnParagraph = GetTemplate(Tempate.columnparagraph.toString())
				    		.replace("[" + TempateID.COLUMNPARAGRAPHID.toString() + "]" , String.valueOf(iPageID) + "c" + String.valueOf(iColumnID) + "p" + String.valueOf(iParagraphID))
				    		.replace("[" + TempateStyle.TOP.toString() + "]" , String.valueOf(oBolderParagraph.top))
				    		.replace("[" + TempateStyle.LEFT.toString() + "]" , String.valueOf(oBolderParagraph.left))
				    		.replace("[" + TempateStyle.HEIGHT.toString() + "]" , String.valueOf(oBolderParagraph.height))
				    		.replace("[" + TempateStyle.WIDTH.toString() + "]" , String.valueOf(oBolderParagraph.width))
				    		.replace("[" + TempateContent.COLUMNPARAGRAPH.toString() + "]" , sColumnParagraphLineAll); 
				    sColumnParagraphAll += sColumnParagraph;				    				   
			    	iParagraphID++;
		    	}
		    }
	    	String sColumn = GetTemplate(Tempate.column.toString())
		    		.replace("[" + TempateID.COLUMNID.toString() + "]" , String.valueOf(iPageID) + "c" + String.valueOf(iColumnID))
		    		.replace("[" + TempateStyle.TOP.toString() + "]" , String.valueOf(oBolderColumn.top))
		    		.replace("[" + TempateStyle.LEFT.toString() + "]" , String.valueOf(oBolderColumn.left))
		    		.replace("[" + TempateStyle.HEIGHT.toString() + "]" , String.valueOf(oBolderColumn.height))
		    		.replace("[" + TempateStyle.WIDTH.toString() + "]" , String.valueOf(oBolderColumn.width))
		    		.replace("[" + TempateContent.COLUMN.toString() + "]" , sColumnParagraphAll);
	    		
	    	sColumnAll += sColumn;
		    iColumnID++;
	    }
	    sPageNormalized = GetTemplate(Tempate.page.toString())
	    		.replace("[" + TempateID.PAGEID.toString() + "]" , String.valueOf(iPageID))
	    		.replace("[" + TempateContent.PAGE.toString() + "]" , sColumnAll);
	    
		return sPageNormalized;
	}
	private boolean CheckIsBoxInside(HTMLObject.BolderObject boxCover , HTMLObject.BolderObject boxInside)
	{
		float p = 0.1f;
		if (boxInside.top >= boxCover.top-p && boxInside.bottom <= boxCover.bottom+p
    			&& boxInside.left >= boxCover.left-p && boxInside.right <= boxCover.right+p)
    	{
			return true;
    	}
		return false;
	}
	private float RoundNumber(String number)
	{
		if (number.length() == 0)
			return 0;
		else 
		{
			return  Float.parseFloat(number);
		}
	}

	/**
	 * Invoke JS function
	 *
	 * @since 1.0
	 */
	private Object invokeJS(String function, Object... args) {
		Object result = null;
        if (scriptEngine != null && !failFunctionList.contains(function)) {
        	try {
        		result = scriptEngine.invokeFunction(function, args);
        	}catch(Exception e) {
        		if (!failFunctionList.contains(function)) {
        			failFunctionList.add(function);
        		}
        	}
			
        }
        return result;
	}
	

	/**
	 * Get template document
	 *
	 * @since 1.0
	 */
	private Document getDocument() {
		Document doc = null;
		try {
			InputStream fTemplate = getClass().getClassLoader().getResourceAsStream("template.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(fTemplate);
			doc.getDocumentElement().normalize();
        } catch (SAXException | ParserConfigurationException | IOException e1) {
            e1.printStackTrace();
        }
		return doc;
	}

	/**
	 * Get template
	 *
	 * @since 1.0
	 */
	private String GetTemplate(String sTagName)
	{
		String sContent = "";

		try {
			Document doc = getDocument();
			Node node = doc.getElementsByTagName(sTagName).item(0);
			sContent = node.getTextContent();
		}catch(Exception e) {
			throw e;
		}
		return sContent;
	}

	/**
	 * Key class
	 *
	 * @since 1.0
	 */
	static class Key {

		public final float x;
		public final float y;

	    public Key(float x, float y) {
	        this.x = x;
	        this.y = y;
	    }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof Key)) return false;
	        Key key = (Key) o;
	        return x == key.x && y == key.y;
	    }

	    @Override
	    public int hashCode() {
	    	int result = (int) x;
	        result = (int) (31 * result + y);
	        return result;
	    }

	}

	/**
	 * KeyComparator class
	 *
	 * @since 1.0
	 */
	class KeyComparator implements Comparator<Key> {

	    @Override
	    public int compare(Key o1, Key o2) {
	    	
	    	Float x1 = o1.x;
	    	Float x2 = o2.x;
	    	Float y1 = o1.y;
	    	Float y2 = o2.y;
	    	int i = x1.compareTo(x2);
	    	if(i == 0){
	    		i = y1.compareTo(y2);
	    	}
	        return i ;

	    }

	}

	/**
	 * Output template enum
	 *
	 * @since 1.0
	 */
	enum Tempate {
		body , 
		page,
		header,
		headerparagraph,headerparagraphline,
		column,
		columnparagraph,columnparagraphline,
		footer,
		footerparagraph,footerparagraphline
		}
	
	enum TempateContent { 
		STYLE, 
		BODY, 
		PAGE,
		HEADER,
		HEADERPARAGRAPH,HEADERPARAGRAPHLINE,
		COLUMN,
		COLUMNPARAGRAPH,COLUMNPARAGRAPHLINE,
		FOOTER,
		FOOTERPARAGRAPH,FOOTERPARAGRAPHLINE
		}
	
	enum TempateID { PAGEID,
		HEADERID,HEADERPARAGRAPHID,HEADERPARAGRAPHLINEID,
		COLUMNID,COLUMNPARAGRAPHID,COLUMNPARAGRAPHLINEID,
		FOOTERID,FOOTERPARAGRAPHID,FOOTERPARAGRAPHLINEID}
	enum TempateStyle { TOP,LEFT,WIDTH,HEIGHT,LINESTYLE}
}
