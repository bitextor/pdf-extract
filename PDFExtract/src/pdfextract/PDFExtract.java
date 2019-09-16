package pdfextract;

import ch.qos.logback.classic.Level;
import pdfextract.HTMLObject.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Invocable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;
import org.fit.pdfdom.PDFDomTreeConfig;
import org.fit.pdfdom.resource.HtmlResourceHandler;
import org.fit.pdfdom.resource.IgnoreResourceHandler;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author MickeyVI
 */
public class PDFExtract {
	/**
	 * Set for build with runnable or not
	 */
	private final boolean runnable = true;

	private String logPath = "";
	private String customScript = "";
	private boolean writeLogFile = true;
	private boolean loadEngineFail = false;
	private Object lockerExtract = new Object();
	private Invocable scriptEngine = null;
	private List<String> failFunctionList = new ArrayList<String>();
	private Pattern patternP = Pattern.compile("<div class=\"p\"");
	private Pattern patternPage = Pattern.compile("<div class=\"page\"");
	private Pattern patternBlankSpace = Pattern.compile("<div class=\"r\"");
	private Pattern patternImage = Pattern.compile("<img.*");
	private Pattern patternPageStartTag = Pattern.compile("<div class=\"page\"[^>]*>");

	private static final String REGEX_TOP = ".*top:([\\-\\+0-9]+.[0-9]+).*";
	private static final String REGEX_LEFT = ".*left:([\\-\\+0-9]+.[0-9]+).*";
	private static final String REGEX_HEIGHT = ".*height:([\\-\\+0-9]+.[0-9]+).*";
	private static final String REGEX_WIDTH = ".*width:([\\-\\+0-9]+.[0-9]+).*";
	private static final String REGEX_FONTSIZE = ".*font-size:([\\-\\+0-9]+.[0-9]+).*";
	private static final String REGEX_FONTFAMILY = ".*font-family:([a-zA-Z\\s\\-]+).*";
	private static final String REGEX_FONTWEIGHT = ".*font-weight:([a-zA-Z0-9\\s\\-]+).*";
	private static final String REGEX_FONTSTYLE = ".*font-style:([a-zA-Z\\s\\-]+).*";
	private static final String REGEX_WORD = ".*>(.*?)<.*";
	private static final String REGEX_COLOR = "(.*color:)(#[a-z]+)(;.*)$";
	private static final String REGEX_WORDSPACING = ".*word-spacing:([\\-\\+0-9]+.[0-9]+).*";

	private Pattern patternTop = Pattern.compile(REGEX_TOP);
	private Pattern patternLeft = Pattern.compile(REGEX_LEFT);
	private Pattern patternHeight = Pattern.compile(REGEX_HEIGHT);
	private Pattern patternWidth = Pattern.compile(REGEX_WIDTH);
	private Pattern patternFontSize = Pattern.compile(REGEX_FONTSIZE);
	private Pattern patternFontFamily = Pattern.compile(REGEX_FONTFAMILY);
	private Pattern patternFontWeight = Pattern.compile(REGEX_FONTWEIGHT);
	private Pattern patternFontStyle = Pattern.compile(REGEX_FONTSTYLE);
	private Pattern patternWord = Pattern.compile(REGEX_WORD);
	private Pattern patternColor = Pattern.compile(REGEX_COLOR);
	private Pattern patternWordSpacing = Pattern.compile(REGEX_WORDSPACING);

	private HashMap<String, String> searchReplaceList = new HashMap<String, String>();
	private Common common = new Common();
	private ExecutorService executor;
	private static float fontSizeScale = 0.5f;

	private void initial(String logFilePath) throws Exception {
		ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(Level.toLevel("off"));

		if (common.IsEmpty(logFilePath)) {
			writeLogFile = false;
		} else {

			if (common.IsEmpty(common.getExtension(logFilePath))) {
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

			if (runnable)
				common.print("Log File: " + logPath);
		}

	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @throws Exception
	 */
	public PDFExtract() throws Exception {
		initial("");
	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @param logFilePath The path to write the log file to.
	 * @throws Exception
	 */
	public PDFExtract(String logFilePath) throws Exception {
		initial(logFilePath);
	}

	/**
	 * PDFExtract is a PDF parser that converts and extracts PDF content into a HTML
	 * format that is optimized for easy alignment across multiple language sources.
	 *
	 * @param inputFile  The path to the source PDF file process for extraction
	 * @param outputFile The path to the output HTML file after extraction
	 * @param rulePath   The path of a custom set of rules to process joins between
	 *                   lines. If no path is specified, then PDFExtract.js will be
	 *                   loaded from the same folder as the PDFExtract.jar
	 *                   execution. If the PDFExtract.js file cannot be found, then
	 *                   processing will continue without analyzing the joins
	 *                   between lines.
	 * @param language   The language of the file using ISO-639-1 codes when
	 *                   processing. If not specified then the default language
	 *                   rules will be used.
	 * @param options    The control parameters
	 * @param debug      Enable Debug/Display mode. This changes the output to a
	 *                   more visual format that renders as HTML in a browser.
	 *
	 */
	public void Extract(String inputFile, String outputFile, String rulePath, String language, String options,
			int debug) throws Exception {

		String outputPath = "";
		try {
			if (writeLogFile) {
				if (runnable)
					common.print(inputFile, "Start extract");
				common.writeLog(logPath, inputFile, "Start extract", false);
			} else {
				common.print(inputFile, "Start extract");
			}

			/**
			 * Check input file exists
			 */
			if (!common.IsExist(inputFile)) {
				throw new Exception("Input file does not exist.");
			}

			/**
			 * Check output file not empty
			 */
			if (common.IsEmpty(outputFile)) {
				throw new Exception("Output file cannot be empty.");
			}

			/**
			 * Get output directory from outputFile and create it if not exists
			 */
			outputPath = common.getParentPath(outputFile);
			if (!common.IsEmpty(outputPath) && !common.IsExist(outputPath)) {
				common.createDir(outputPath);
			}

			if (runnable) {
				/**
				 * Check input file permission
				 */
				common.checkPermissions(inputFile);
			}

			/**
			 * Check input file extension
			 */
			if (!common.getExtension(inputFile).toLowerCase().equals("pdf")) {
				throw new Exception("Input file extension is not pdf.");
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
						if (scriptEngine == null)
							loadEngineFail = true;
					}
				}
			}

			StringBuffer htmlBuffer = new StringBuffer("");
			/**
			 * Call function to convert PDF to HTML
			 */
			htmlBuffer = convertPdfToHtml(inputFile);

			AtomicReference<List<PageObject>> refPages = new AtomicReference<List<PageObject>>(
					new ArrayList<PageObject>());
			/**
			 * Call function to paint html box
			 */
			htmlBuffer = getHtmlBox(htmlBuffer, refPages);

			if (debug == 0) {
				/**
				 * Call function to normalize html
				 */
				htmlBuffer = Normalize(htmlBuffer, refPages, language);
			} else {

				/**
				 * Call function to draw box to output
				 */
				htmlBuffer = drawHtmlBox(htmlBuffer, refPages);
			}

			/**
			 * Write to output file.
			 */
			common.WriteFile(outputFile, htmlBuffer.toString());

			if (writeLogFile) {
				if (runnable)
					common.print(inputFile, "Extract success. -> " + outputFile + "");
				common.writeLog(logPath, inputFile, "Extract success. -> " + outputFile + "", false);
			} else {
				common.print(inputFile, "Extract success. -> " + outputFile + "");
			}

		} catch (Exception e) {

			if (!common.IsEmpty(outputFile)) {
				common.WriteFile(outputFile, common.getOutputError(e));
			}

			String message = e.getMessage();
			if (writeLogFile) {
				common.writeLog(logPath, inputFile, "Error: " + message, true);
			} else {
				if (!runnable)
					common.print(inputFile, "Error: " + message);
			}

			throw e;
		} finally {
		}

	}

	/**
	 * PDFExtract is a PDF parser that converts and extracts PDF content into a HTML
	 * format that is optimized for easy alignment across multiple language sources.
	 *
	 * @param inputStream Stream data as ByteArray for extraction
	 * @param language    The language of the file using ISO-639-1 codes when
	 *                    processing. If not specified then the default language
	 *                    rules will be used.
	 * @param options     The control parameters
	 * @param debug       Enable Debug/Display mode. This changes the output to a
	 *                    more visual format that renders as HTML in a browser.
	 *
	 * @return ByteArrayOutputStream Stream Out
	 */
	public ByteArrayOutputStream Extract(ByteArrayInputStream inputStream, String language, String options, int debug)
			throws Exception {
		try {
			if (writeLogFile) {
				if (runnable)
					common.print("Input Stream", "Start extract");
				common.writeLog(logPath, "Input Stream", "Start extract", false);
			} else {
				common.print("Input Stream", "Start extract");
			}

			/**
			 * Check input stream can be read ?
			 */
			if (inputStream.available() <= 0) {
				throw new Exception("Input Stream does not exist.");
			}

			StringBuffer htmlBuffer;

			/**
			 * Call function to paint html box
			 */
			htmlBuffer = convertPdfToHtml(inputStream);

			AtomicReference<List<PageObject>> refPages = new AtomicReference<List<PageObject>>(
					new ArrayList<PageObject>());
			/**
			 * Call function to paint html box
			 */
			htmlBuffer = getHtmlBox(htmlBuffer, refPages);

			if (debug == 0) {
				/**
				 * Call function to normalize html
				 */
				htmlBuffer = Normalize(htmlBuffer, refPages, language);
			} else {
				/**
				 * Call function to draw box to html
				 */
				htmlBuffer = drawHtmlBox(htmlBuffer, refPages);
			}
			/**
			 * Write output stream
			 */
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			IOUtils.write(htmlBuffer.toString(), outputStream, "UTF-8");

			if (writeLogFile) {
				if (runnable)
					common.print("Input Stream", "Extract success.");
				common.writeLog(logPath, "Input Stream", "Extract success.", false);
			} else {
				common.print("Input Stream", "Extract success.");
			}

			return outputStream;
		} catch (Exception e) {

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			IOUtils.write(common.getOutputError(e), outputStream, "UTF-8");

			String message = e.getMessage();
			if (writeLogFile) {
				common.writeLog(logPath, "Input Stream", "Error: " + message, true);
			} else {
				if (!runnable)
					common.print("Input Stream", "Error: " + message);
			}

			throw e;
		} finally {
		}
	}

	/**
	 * PDFExtract is a PDF parser that converts and extracts PDF content into a HTML
	 * format that is optimized for easy alignment across multiple language sources.
	 *
	 * @param batchFile   The path to the batch file for processing list of files.
	 *                    The input file and output file are specified on the same
	 *                    line delimited by a tab. Each line is delimited by a new
	 *                    line character.
	 * @param rulePath    The path of a custom set of rules to process joins between
	 *                    lines. If no path is specified, then PDFExtract.js will be
	 *                    loaded from the same folder as the PDFExtract.jar
	 *                    execution. If the PDFExtract.js file cannot be found, then
	 *                    processing will continue without analyzing the joins
	 *                    between lines.
	 * @param threadCount The number of threads to run concurrently when processing
	 *                    PDF files. One file can be processed per thread. If not
	 *                    specified, then the default valur of 1 thread is used.
	 * @param language    The language of the file using ISO-639-1 codes when
	 *                    processing. If not specified then the default language
	 *                    rules will be used.
	 * @param options     The control parameters
	 * @param debug       Enable Debug/Display mode. This changes the output to a
	 *                    more visual format that renders as HTML in a browser.
	 *
	 */
	public void Extract(String batchFile, String rulePath, int threadCount, String language, String options, int debug)
			throws Exception {
		try {
			if (writeLogFile) {
				if (runnable)
					common.print("Start extract batch file: " + batchFile);
				common.writeLog(logPath, "Start extract batch file: " + batchFile);
			} else {
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
				if (scriptEngine == null)
					loadEngineFail = true;
			}

			if (threadCount == 0)
				threadCount = 1;
			int maxThreadCount = threadCount;

			/**
			 * Read batch file
			 */
			List<String> lines = common.readLines(batchFile);
			executor = Executors.newFixedThreadPool(maxThreadCount);

			int ind = 0, len = lines.size();
			while (ind < len) {
				String line = lines.get(ind);

				/**
				 * Skip the empty line
				 */
				if (common.IsEmpty(line)) {
					ind++;
					continue;
				}

				AddThreadExtract(ind, line, rulePath, language, options, debug);
				ind++;
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

		} catch (Exception e) {
			String message = e.getMessage();
			if (writeLogFile) {
				common.writeLog(logPath, message, true);
			} else {
				if (!runnable)
					common.print("Error: " + e.getMessage());
			}
			throw e;
		} finally {
			if (writeLogFile) {
				if (runnable)
					common.print("Finish extract batch file: " + batchFile);
				common.writeLog(logPath, "Finish extract batch file: " + batchFile);
			} else {
				common.print("Finish extract batch file: " + batchFile);
			}
		}
	}

	/**
	 * Add new thread pdf extract
	 */
	private void AddThreadExtract(int index, String line, String rulePath, String language, String options, int debug) {
		try {
			executor.execute(new Runnable() {
				@Override
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

					} catch (Exception e) {
						String message = e.getMessage();
						if (writeLogFile) {
							if (runnable)
								common.print(inputFile, "Error: " + message);
							common.writeLog(logPath, inputFile, "Error: " + message, true);
						} else {
							common.print(inputFile, "Error: " + message);
						}
					}
				}
			});

		} catch (Exception ex) {
			String message = ex.toString();
			if (writeLogFile) {
				if (runnable)
					common.print("Batch line: " + line + ", Error: " + message);
				common.writeLog(logPath, "Batch line: " + line + ", Error: " + message, true);
			} else {
				common.print("Batch line: " + line + ", Error: " + message);
			}
		}
	}

	/**
	 * Convert PDF to HTML file
	 */
	private StringBuffer convertPdfToHtml(String inputFile) throws Exception {
		PDDocument pdf = null;
		StringWriter output = null;
		try {
			// loads the PDF file, parse it and gets html file
			pdf = PDDocument.load(new java.io.File(inputFile));
			HtmlResourceHandler handler = new IgnoreResourceHandler();
			PDFDomTreeConfig config = PDFDomTreeConfig.createDefaultConfig();
			config.setImageHandler(handler);
			PDFDomTree parser = new PDFDomTree(config);
			parser.createDOM(pdf);
			output = new StringWriter();
			parser.writeText(pdf, output);

			return output.getBuffer();
		} catch (Exception e) {
			throw e;
		} finally {
			if (pdf != null) {
				pdf.close();
				pdf = null;
			}
			if (output != null) {
				output.close();
				output = null;
			}
		}
	}

	/**
	 * Convert PDF to HTML file
	 */
	private StringBuffer convertPdfToHtml(InputStream inputStream) throws Exception {
		MemoryUsageSetting setupMainMemoryOnly = MemoryUsageSetting.setupMainMemoryOnly();
		PDDocument pdf = null;
		StringWriter output = null;
		try {
			pdf = PDDocument.load(inputStream, setupMainMemoryOnly);
			HtmlResourceHandler handler = new IgnoreResourceHandler();
			PDFDomTreeConfig config = PDFDomTreeConfig.createDefaultConfig();
			config.setImageHandler(handler);
			PDFDomTree parser = new PDFDomTree(config);
			output = new StringWriter();
			parser.writeText(pdf, output);
			return output.getBuffer();
		} catch (Exception e) {
			throw e;
		} finally {
			if (pdf != null) {
				pdf.close();
				pdf = null;
			}
			if (output != null) {
				output.close();
				output = null;
			}
		}
	}

	/**
	 * Get HTML Box
	 */
	private StringBuffer getHtmlBox(StringBuffer htmlBuffer, AtomicReference<List<PageObject>> refPages)
			throws Exception {
		BufferedReader b_in = null;
		StringBuffer htmlBufferOut = new StringBuffer();
		List<PageObject> pages = refPages.get();
		try {

			InputStreamReader i_in = new InputStreamReader(new ByteArrayInputStream(htmlBuffer.toString().getBytes()),
					StandardCharsets.UTF_8);
			b_in = new BufferedReader(i_in);
			String line = "";
			int currentPage = 0;
			PageObject page = new PageObject();

			while ((line = b_in.readLine()) != null) {
				Matcher m = this.patternP.matcher(line);
				Matcher m1 = this.patternPage.matcher(line);
				Matcher m2 = this.patternBlankSpace.matcher(line);
				Matcher m3 = this.patternImage.matcher(line);

				if (m2.find() || m3.find()) {
					line = line.replaceAll(".*src.*>", "");
					line = line.replaceAll("<div class=\"r\" style.*", "");

					if (!common.IsEmpty(line.trim())) {
						htmlBufferOut.append(line + "\n");
					}
				} else if (m.find()) {
					htmlBufferOut.append(line + "\n");

					TextObject text = getTextObject(line);
					if (!checkLineAdd(page.width, page.height, text)) {
						continue;
					}

					page.texts.add(text);
				} else if (m1.find()) {
					if (line.indexOf("/>") > -1) {
						// fix for blank page
						line = line.substring(0, line.lastIndexOf("/>")) + "></div>";
					}
					htmlBufferOut.append(line + "\n");

					if (currentPage > 0) {
						pages.add(page);
					}

					currentPage++;
					page = new PageObject();
					page.pageno = currentPage;
					page.height = common.getFloat(patternHeight.matcher(line).replaceAll("$1"));
					page.width = common.getFloat(patternWidth.matcher(line).replaceAll("$1"));
				} else {
					if (line.indexOf("</style>") > -1) {
						line = line.replace("</style>",
								"\n.p:nth-child(even){background:rgba(255, 255, 0, 0.5);} .p:nth-child(odd){background:rgba(200, 255, 100, 0.5);}\n</style>");
					}
					htmlBufferOut.append(line + "\n");
				}
			}

			//
			pages.add(page);
			//

		} finally {
			if (b_in != null) {
				b_in.close();
				b_in = null;
			}
			refPages.set(pages);
		}

		htmlBuffer = htmlBufferOut;
		getBox(refPages);

		return htmlBuffer;
	}

	private TextObject getTextObject(String line) {
		TextObject obj = new TextObject();

		obj.top = common.getFloat(patternTop.matcher(line).replaceAll("$1"));
		obj.left = common.getFloat(patternLeft.matcher(line).replaceAll("$1"));
		obj.height = common.getFloat(patternHeight.matcher(line).replaceAll("$1"));
		obj.width = common.getFloat(patternWidth.matcher(line).replaceAll("$1"));
		obj.bottom = obj.top + obj.height;
		obj.right = obj.left + obj.width;
		obj.fontfamily = patternFontFamily.matcher(line).replaceAll("$1");
		obj.fontsize = common.getFloat(patternFontSize.matcher(line).replaceAll("$1"));
		obj.text = patternWord.matcher(line).replaceAll("$1");
		obj.wordspacing = common.getFloat(patternWordSpacing.matcher(line).replaceAll("$1"));
		if (line.indexOf("color:") > -1) {
			obj.color = patternColor.matcher(line).replaceAll("$1");
		}
		if (line.indexOf("font-weight") > -1) {
			obj.fontweight = patternFontWeight.matcher(line).replaceAll("$1");
		}
		if (line.indexOf("font-style") > -1) {
			obj.fontstyle = patternFontStyle.matcher(line).replaceAll("$1");
		}

		// get font style
		String sFontStyle = "font-family:" + obj.fontfamily + ";" + "font-size:" + obj.fontsize + "pt;";
		if (obj.fontweight != null)
			sFontStyle += "font-weight:" + obj.fontweight + ";";

		if (obj.fontstyle != null)
			sFontStyle += "font-style:" + obj.fontstyle + ";";

		obj.style = sFontStyle;

		return obj;
	}

	private boolean checkLineAdd(float pageWidth, float pageHeight, TextObject text) {
		if (text.left < 0 || text.top < 0 || text.left > pageWidth || text.top > pageHeight) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Draw box to html
	 */
	private StringBuffer drawHtmlBox(StringBuffer htmlBuffer, AtomicReference<List<PageObject>> refPages)
			throws IOException {
		StringBuffer htmlBufferOut = new StringBuffer();
		List<PageObject> pages = refPages.get();
		try {
			int nPage = 0;
			String sContent = htmlBuffer.toString();

			Matcher m = patternPageStartTag.matcher(sContent);
			while (m.find()) {
				m.appendReplacement(htmlBufferOut, "$0\n");

				PageObject page = pages.get(nPage);
				for (ColumnObject column : page.columns) {
					htmlBufferOut.append(column.html + "\n");
					for (ParagraphObject paragraph : column.paragraphs) {
						htmlBufferOut.append(paragraph.html + "\n");
						for (LineObject line : paragraph.lines) {
							htmlBufferOut.append(line.html + "\n");
						}
					}
				}
				nPage++;
			}
			m.appendTail(htmlBufferOut);
		} finally {
		}

		return htmlBufferOut;
	}

	/**
	 * Get boxes
	 */
	private void getBox(AtomicReference<List<PageObject>> refPages) {
		List<PageObject> pages = refPages.get();
		for (PageObject page : pages) {

			if (page.texts.size() > 0) {

				List<ColumnObject> columns = new ArrayList<ColumnObject>();
				List<ParagraphObject> paragraphs = new ArrayList<ParagraphObject>();
				List<LineObject> lines = new ArrayList<LineObject>();
				List<TextObject> texts = new ArrayList<TextObject>();

				ColumnObject column = null;
				ParagraphObject paragraph = null;
				LineObject line = null;

				for (int i = 0, len = page.texts.size(); i < len; i++) {

					float gap_line = 0, gap_word_next = 0, gap_word_prev = 0;
					float diff_top_prev = 0, diff_top_next = 0, diff_left_next = 0;

					TextObject text = page.texts.get(i);
					TextObject next_text = (i + 1 < len ? page.texts.get(i + 1) : null);
					TextObject prev_text = (i > 0 ? page.texts.get(i - 1) : null);

					//
					if (prev_text != null) {
						diff_top_prev = Math.abs(prev_text.top - text.top);
						if (diff_top_prev < 2)
							diff_top_prev = 0;
						gap_word_prev = Math.abs(prev_text.left + prev_text.width - text.left);
					}

					//
					if (next_text != null) {
						diff_top_next = Math.abs(text.top - next_text.top);
						diff_left_next = Math.abs(text.left - next_text.left);

						if (diff_top_next > 0) {
							gap_line = Math.abs(text.top + text.height - next_text.top);
						}

						if (diff_top_next < 2)
							diff_top_next = 0;
						if (diff_left_next < 2)
							diff_left_next = 0;

						gap_word_next = Math.abs(text.left + text.width - next_text.left);
					}

					/*
					 * LINE
					 */
					if (i == 0 || texts.size() == 0 || (prev_text != null && diff_top_prev == 0 && gap_word_prev > 15)
							|| (prev_text != null && diff_top_prev > 0
									&& Math.abs(prev_text.top + prev_text.height - (text.top + text.height)) > 2
									&& diff_top_prev > line.height)) {
						// start new line
						line = new LineObject(text.top, text.left, text.height, text.width);
					} else {
						line.top = Math.min(line.top, text.top);
						line.left = Math.min(line.left, text.left);
						line.bottom = Math.max(line.bottom, text.bottom);
						line.right = Math.max(line.right, text.right);
						line.width = line.right - line.left + 1;
						line.height = Math.max(line.height, text.height);
					}

					if (!texts.contains(text)) {
						texts.add(text);
					}

					if ((next_text != null && diff_top_next == 0 && gap_word_next < 15)
							|| (next_text != null && gap_word_next < 15
									&& Math.abs(text.top + text.height - (next_text.top + next_text.height)) < Math
											.max(text.height, next_text.height) / 2)) {
						// next text is same line
						double gap = Math.abs(Math.abs(text.left - next_text.left) - text.width);
						line.width += (gap);
					} else {
						// next text is new line
						// end current line
						if (line != null) {
							line.color = BoxColor.LINE.getColor();
							line.html = getDIV(line.top, line.left, line.height, line.width, BoxColor.LINE.getColor());
							line.texts.addAll(texts);
							if (!lines.contains(line)) {
								lines.add(line);
							}
							texts.clear();
						}
						// line = null;
					}

					/*
					 * PARAGRAPH
					 */
					if (paragraph == null) {
						// start new paragraph
						paragraph = new ParagraphObject(line.top, line.left, line.height, line.width);
					} else {
						paragraph.top = Math.min(paragraph.top, line.top);
						paragraph.left = Math.min(paragraph.left, line.left);
						paragraph.bottom = Math.max(paragraph.bottom, line.bottom);
						paragraph.right = Math.max(paragraph.right, line.right);
						paragraph.height = paragraph.bottom - paragraph.top + 1;
						paragraph.width = paragraph.right - paragraph.left + 1;
					}

					if (lines.size() > 0
							&& ((diff_top_next > 0 && gap_line > (text.height / 2) && diff_top_next > line.height)
									|| (next_text == null
											|| (diff_top_next > text.height / 2 && !text.fontfamily.contains("Bold")
													&& next_text.fontfamily.contains("Bold")))
									|| (diff_top_next > 0 && line.left < text.left
											&& (next_text == null
													|| (gap_line > Math.min(text.height, next_text.height) * 2)))
									|| (next_text == null || (diff_top_next > 0 && gap_line > text.height / 2
											&& diff_top_next > line.height && next_text.left > line.left)))) {

						// end paragraph
						if (paragraph != null) {
							paragraph.color = BoxColor.PARAGRAPH.getColor();
							paragraph.html = getDIV(paragraph.top, paragraph.left, paragraph.height, paragraph.width,
									BoxColor.PARAGRAPH.getColor());
							paragraph.lines.addAll(lines);
							paragraphs.add(paragraph);
							lines.clear();
							texts.clear();
							paragraph = null;
						}
						// line = null;
					}

					/*
					 * COLUMN
					 */
					if (column == null) {
						// start new column
						column = new ColumnObject(line.top, line.left, line.height, line.width);
					} else {
						column.top = Math.min(column.top, line.top);
						column.left = Math.min(column.left, line.left);
						column.bottom = Math.max(column.bottom, line.bottom);
						column.right = Math.max(column.right, line.right);
						column.height = column.bottom - column.top + 1;
						column.width = column.right - column.left + 1;
					}

					if (paragraphs.size() > 0 && (

					/*
					 * next top not same + left not same + gap more than min height*1.8
					 */
					(diff_top_next > 0 && line.left < text.left
							&& (next_text == null || (gap_line > Math.min(text.height, next_text.height) * 2)))

							/* diff top more than line height time 5 */
							|| (next_text == null || (diff_top_next > line.height * 5))

							/*
							 * diff top more than line height + height not same + diff height more than 10
							 */
							|| (next_text == null || (diff_top_next > line.height && next_text.left > column.left
									&& text.height != next_text.height
									&& Math.abs(text.height - next_text.height) > 10))

							/*
							 * diff top more than line height + text not bold + next text is bold + height
							 * not same + left not same + color not same + big height
							 */
							|| (next_text == null || (diff_top_next > line.height && !text.fontfamily.contains("Bold")
									&& next_text.fontfamily.contains("Bold") && text.height != next_text.height
									&& text.left != next_text.left && !text.color.equals(next_text.color)
									&& text.height > 20))

							/*
							 * diff top more than line height time 3 + text not bold + next text is bold +
							 * height not same + left not same
							 */
							|| (next_text == null || (diff_top_next > line.height * 3
									&& !text.fontfamily.contains("Bold") && next_text.fontfamily.contains("Bold")
									&& text.height != next_text.height && text.left != next_text.left))

							/*
							 * diff top more than line height time 2 + text not bold + next text is bold +
							 * height not same + has gap line + big height
							 */
							|| (next_text == null
									|| (diff_top_next > line.height * 2 && !text.fontfamily.contains("Bold")
											&& next_text.fontfamily.contains("Bold") && text.height != next_text.height
											&& gap_line > 0 && text.height > 20 && next_text.height > 20))

							/*
							 * diff top more than line height time 3 + text not bold + next text is bold +
							 * height not same + not big height + next not big height
							 */
							|| (next_text == null
									|| (diff_top_next > line.height * 3 && !text.fontfamily.contains("Bold")
											&& next_text.fontfamily.contains("Bold") && text.height != next_text.height
											&& text.height <= 20 && next_text.height <= 20)))) {

						// end column
						if (column != null) {
							column.color = BoxColor.COLUMN.getColor();
							column.html = getDIV(column.top, column.left, column.height, column.width,
									BoxColor.COLUMN.getColor());
							column.paragraphs.addAll(paragraphs);
							columns.add(column);

							paragraphs.clear();
							paragraph = null;
							lines.clear();
							texts.clear();
							column = null;
						}
						// line = null;
					}

					if (i == len - 1) {
						// last text object in page
						if (paragraph != null) {
							paragraph.width += line.width;
							paragraph.color = BoxColor.PARAGRAPH.getColor();
							paragraph.html = getDIV(paragraph.top, paragraph.left, paragraph.height, paragraph.width,
									BoxColor.PARAGRAPH.getColor());
							paragraph.lines.addAll(lines);
							paragraphs.add(paragraph);
						}
						if (column != null) {
							column.width += line.width;
							column.color = BoxColor.COLUMN.getColor();
							column.html = getDIV(column.top, column.left, column.height, column.width,
									BoxColor.COLUMN.getColor());
							column.paragraphs.addAll(paragraphs);
							columns.add(column);
						}
						page.columns.addAll(columns);
						columns.clear();
						paragraphs.clear();
						lines.clear();
						texts.clear();
						column = null;
						paragraph = null;
						line = null;
					}
				}
			}

		}
	}

	private String getDIV(double TOP, double LEFT, double HEIGHT, double WIDTH, String color) {
		String DIV = "<div class=\"p\" style=\"border: 1pt solid;top:" + TOP + "pt;left:" + LEFT + "pt;height:" + HEIGHT
				+ "pt;width:" + WIDTH + "pt;background-color:transparent;color:" + color + ";\"></div>";
		return DIV;
	}

	/**
	 * Normalize html
	 */
	private StringBuffer Normalize(StringBuffer htmlBuffer, AtomicReference<List<PageObject>> refPages, String language)
			throws Exception {
		List<PageObject> pages = refPages.get();
		try {
			StringBuilder sbPageAll = new StringBuilder();
			AtomicReference<Hashtable<String, Integer>> hashClasses = new AtomicReference<Hashtable<String, Integer>>();
			searchReplaceList = common.getSearchReplaceList();

			for (PageObject page : pages) {
				String pageContent = GetPageNormalizedHtml(page, hashClasses, language);
				sbPageAll.append(pageContent);
			}

			/**
			 * Get classes - page - the wrapping boundary of a page. - header - the wrapping
			 * boundary of the page header. - footer - the wrapping boundary of the page
			 * footer. - column - the wrapping boundary of a column. - line - the wrapping
			 * boundary of the paragraph line. - h1 - the wrapping boundary of a the
			 * paragraph h1. - h2 - the wrapping boundary of a the paragraph h2.
			 */

			AtomicReference<StringBuilder> sbPageAllNomalize = new AtomicReference<StringBuilder>();
			sbPageAllNomalize.set(sbPageAll);
			String sClasses = GetNormalizeClasses(hashClasses.get(), sbPageAllNomalize);

			return new StringBuffer(
					GetTemplate(Tempate.body.toString()).replace("[" + TempateContent.STYLE.toString() + "]", sClasses)
							.replace("[" + TempateContent.BODY.toString() + "]", sbPageAllNomalize.get().toString()));

		} catch (Exception e1) {
			throw e1;
		} finally {
		}
	}

	/**
	 * Get normalize classes
	 */
	private String GetNormalizeClasses(Hashtable<String, Integer> hashClasses,
			AtomicReference<StringBuilder> sbPageAll) {
		StringBuilder _sbPageAll = sbPageAll.get();
		StringBuilder sbClasses = new StringBuilder();
		Hashtable<Float, String> hashFontSize = new Hashtable<Float, String>();
		String sGlobalStyle = "";
		int iGlobalStyle = 0;

		List<String> listClasse = new ArrayList<String>(hashClasses.keySet());
		for (String classes : listClasse) {
			if (hashClasses.get(classes) > iGlobalStyle) {
				iGlobalStyle = hashClasses.get(classes);
				sGlobalStyle = classes;
			}
		}

		for (String classes : listClasse) {
			if (!sGlobalStyle.equals(classes)) {
				float fFontSize = common.getFloat(classes.replaceAll(REGEX_FONTSIZE, "$1"));
				for (int i = 0; i < 10; i++) {
					if (hashFontSize.containsKey(fFontSize)) {
						fFontSize = (float) (fFontSize + fontSizeScale);
					} else
						break;
				}
				hashFontSize.put(fFontSize, classes);
			}
		}

		// replace global style
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

			sbClasses.append("\n.h" + iHCount + " {");
			sbClasses.append(hashFontSize.get(fontsize));
			sbClasses.append("}");
			// replace h style
			_sbPageAll = new StringBuilder(_sbPageAll.toString().replace(" </span>", "</span>").replaceAll(
					"(class=\")(line)(\" style=\"[^<>]+)(" + hashFontSize.get(fontsize).replace("-", "\\-")
							.replace("+", "\\+").replace(".", "\\.").replace(" ", "[ ]") + ")(\")",
					"$1$2 h" + iHCount + "$3$5"));
			iHCount--;
		}

		sbPageAll.set(_sbPageAll);

		return sbClasses.toString();
	}

	/**
	 * Get page normalize html
	 */
	private String GetPageNormalizedHtml(PageObject page, AtomicReference<Hashtable<String, Integer>> hashClasses,
			String language) {
		String sPageNormalized = "";
		Hashtable<String, Integer> _hashClasses = hashClasses.get();
		if (_hashClasses == null)
			_hashClasses = new Hashtable<String, Integer>();

		int iPageID = page.pageno;

		List<ColumnObject> listColumnLeft = page.columns;
		Collections.sort(listColumnLeft, new BoxComparator());
		String sColumnAll = "";
		int iColumnID = 1;

		// Loop each column
		for (ColumnObject column : listColumnLeft) {

			String sColumnParagraphAll = "";
			int iParagraphID = 1;

			List<ParagraphObject> listParagraph = column.paragraphs;
			Collections.sort(listParagraph, new BoxComparator());

			// Loop each paragraph in column
			for (ParagraphObject paragraph : listParagraph) {

				String sColumnParagraphLineAll = "";

				List<LineObject> listLine = paragraph.lines;
				Collections.sort(listLine, new BoxComparator());

				// Loop each line in paragraph
				int iLineID = 0;
				float prevRight = 0;
				for (LineObject line : listLine) {

					List<TextObject> listText = line.texts;
					Collections.sort(listText, new TextComparator());

					int round = 0;
					String sStyle = "";
					String sLine = "";
					// Loop each text in line
					for (TextObject text : listText) {
						if (text.text.indexOf('\uFFFD') > -1) {
							if (line.height > (text.fontsize * 2) + 1) {
								line.height = (text.fontsize * 2) + 1;
								line.bottom = line.top + line.height;
							}
						}
						sStyle = text.style;
						// add counting in classes object
						if (_hashClasses.containsKey(sStyle))
							_hashClasses.put(sStyle, _hashClasses.get(sStyle) + 1);
						else
							_hashClasses.put(sStyle, 1);
					}

					// Loop each text in line
					for (TextObject text : listText) {
						if (text.fontsize < line.height && (text.bottom < line.bottom - (line.height / 2) + 1
								|| text.top > line.bottom - (line.height / 2) + 1
								|| (line.height - text.height > 1 && text.height - text.fontsize > 2))) {
							// <sup>
							if (Math.abs(text.top - line.top) < 1) {
								text.text = "<sup>" + text.text + "</sup>";
							} else {
								text.text = "<sub>" + text.text + "</sub>";
							}
						}
						if (round > 0 && text.left - prevRight >= 0.2) {
							sLine += " ";
						}
						sLine += text.text;
						prevRight = text.left + text.width;
						round++;
						sStyle = text.style;
					}

					sLine = common.replaceText(searchReplaceList, sLine);
					sStyle = "";

					String sColumnParagraphLine = GetTemplate(Tempate.columnparagraphline.toString())
							.replace("[" + TempateID.COLUMNPARAGRAPHLINEID.toString() + "]",
									common.getStr(iPageID) + "c" + common.getStr(iColumnID) + "p"
											+ common.getStr(iParagraphID) + "l" + common.getStr(iLineID))
							.replace("[" + TempateStyle.TOP.toString() + "]", common.getStr(line.top))
							.replace("[" + TempateStyle.LEFT.toString() + "]", common.getStr(line.left))
							.replace("[" + TempateStyle.HEIGHT.toString() + "]", common.getStr(line.height))
							.replace("[" + TempateStyle.WIDTH.toString() + "]", common.getStr(line.width))
							.replace("[" + TempateStyle.LINESTYLE.toString() + "]", sStyle)
							.replace("[" + TempateContent.COLUMNPARAGRAPHLINE.toString() + "]", sLine);

					if (scriptEngine != null && sColumnParagraphLine.length() > 0) {
						String sResult = common.getStr(invokeJS("repairObjectSequence", sColumnParagraphLine));
						if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length) {
							sColumnParagraphLine = sResult;
						}
					}

					sColumnParagraphLineAll += sColumnParagraphLine + "\n";
					iLineID++;
				}

				if (scriptEngine != null && sColumnParagraphLineAll.length() > 0) {
					// call analyzeJoins
					String sResult = common.getStr(invokeJS("analyzeJoins", sColumnParagraphLineAll, language));
					if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length) {
						sColumnParagraphLineAll = sResult;
					}

					// call isHeader
					sResult = common.getStr(invokeJS("isHeader", sColumnParagraphLineAll));
					if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length) {
						sColumnParagraphLineAll = sResult;
					}

					// call isFooter
					sResult = common.getStr(invokeJS("isFooter", sColumnParagraphLineAll));
					if (sResult.split("\n").length == sColumnParagraphLineAll.split("\n").length) {
						sColumnParagraphLineAll = sResult;
					}
				}

				String sColumnParagraph = GetTemplate(Tempate.columnparagraph.toString())
						.replace("[" + TempateID.COLUMNPARAGRAPHID.toString() + "]",
								common.getStr(iPageID) + "c" + common.getStr(iColumnID) + "p"
										+ common.getStr(iParagraphID))
						.replace("[" + TempateStyle.TOP.toString() + "]", common.getStr(paragraph.top))
						.replace("[" + TempateStyle.LEFT.toString() + "]", common.getStr(paragraph.left))
						.replace("[" + TempateStyle.HEIGHT.toString() + "]", common.getStr(paragraph.height))
						.replace("[" + TempateStyle.WIDTH.toString() + "]", common.getStr(paragraph.width))
						.replace("[" + TempateContent.COLUMNPARAGRAPH.toString() + "]", sColumnParagraphLineAll);
				sColumnParagraphAll += sColumnParagraph;
				iParagraphID++;

			}
			String sColumn = GetTemplate(Tempate.column.toString())
					.replace("[" + TempateID.COLUMNID.toString() + "]",
							common.getStr(iPageID) + "c" + common.getStr(iColumnID))
					.replace("[" + TempateStyle.TOP.toString() + "]", common.getStr(column.top))
					.replace("[" + TempateStyle.LEFT.toString() + "]", common.getStr(column.left))
					.replace("[" + TempateStyle.HEIGHT.toString() + "]", common.getStr(column.height))
					.replace("[" + TempateStyle.WIDTH.toString() + "]", common.getStr(column.width))
					.replace("[" + TempateContent.COLUMN.toString() + "]", sColumnParagraphAll);

			sColumnAll += sColumn;
			iColumnID++;
		}
		sPageNormalized = GetTemplate(Tempate.page.toString())
				.replace("[" + TempateID.PAGEID.toString() + "]", common.getStr(iPageID))
				.replace("[" + TempateContent.PAGE.toString() + "]", sColumnAll);

		hashClasses.set(_hashClasses);

		return sPageNormalized;
	}

	/**
	 * Invoke JS function
	 */
	private Object invokeJS(String function, Object... args) {
		Object result = null;
		if (scriptEngine != null && !failFunctionList.contains(function)) {
			try {
				result = scriptEngine.invokeFunction(function, args);
			} catch (Exception e) {
				if (!failFunctionList.contains(function)) {
					failFunctionList.add(function);
				}
			}
		}
		return result;
	}

	/**
	 * Get template document
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
	 */
	private String GetTemplate(String sTagName) {
		String sContent = "";

		try {
			Document doc = getDocument();
			Node node = doc.getElementsByTagName(sTagName).item(0);
			sContent = node.getTextContent();
		} catch (Exception e) {
			throw e;
		}
		return sContent;
	}

	/**
	 * Comparator class
	 */
	class BoxComparator implements Comparator<BoxObject> {
		@Override
		public int compare(BoxObject o1, BoxObject o2) {
			Float x1 = o1.top;
			Float x2 = o2.top;
			if (Math.abs(x1 - x2) < 0.5)
				x1 = x2;
			Float y1 = o1.left;
			Float y2 = o2.left;

			int i = x1.compareTo(x2);
			if (i == 0) {
				i = y1.compareTo(y2);
			}
			return i;
		}
	}

	/**
	 * Comparator class
	 */
	class TextComparator implements Comparator<BoxObject> {
		@Override
		public int compare(BoxObject o1, BoxObject o2) {
			if (o1.left < o2.left)
				return -1;
			else if (o1.left > o2.left)
				return 1;
			return 0;
		}
	}

	/**
	 * Output template enum
	 */
	enum Tempate {
		body, page, header, headerparagraph, headerparagraphline, column, columnparagraph, columnparagraphline, footer,
		footerparagraph, footerparagraphline
	}

	enum TempateContent {
		STYLE, BODY, PAGE, HEADER, HEADERPARAGRAPH, HEADERPARAGRAPHLINE, COLUMN, COLUMNPARAGRAPH, COLUMNPARAGRAPHLINE,
		FOOTER, FOOTERPARAGRAPH, FOOTERPARAGRAPHLINE
	}

	enum TempateID {
		PAGEID, HEADERID, HEADERPARAGRAPHID, HEADERPARAGRAPHLINEID, COLUMNID, COLUMNPARAGRAPHID, COLUMNPARAGRAPHLINEID,
		FOOTERID, FOOTERPARAGRAPHID, FOOTERPARAGRAPHLINEID
	}

	enum TempateStyle {
		TOP, LEFT, WIDTH, HEIGHT, LINESTYLE
	}

}