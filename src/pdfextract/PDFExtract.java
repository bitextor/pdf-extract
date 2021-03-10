package pdfextract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.itextpdf.text.pdf.PdfEncryptor;
import com.itextpdf.text.pdf.PdfReader;

import pdfextract.Config.EOFInfo;
import pdfextract.Config.JoinWordInfo;
import pdfextract.Config.LangInfo;
import pdfextract.Config.NormalizeInfo;
import pdfextract.DetectLanguage.LanguageResult;
import pdfextract.HTMLObject.DocumentObject;
import pdfextract.HTMLObject.LangObject;
import pdfextract.HTMLObject.PageObject;
import pdfextract.HTMLObject.StyleObject;
import pdfextract.HTMLObject.TextObject;
import pdfextract.HTMLObject.WarnObject;
import pdfextract.SentenceJoin.WorkerStatus;

/**
 * @author MickeyVI
 */
public class PDFExtract {
	/**
	 * Set for build with runnable or not
	 */
	private final boolean runnable = true;

	private String logPath = "";
	private boolean writeLogFile = true;

	private Pattern patternPageOpen = Pattern.compile("<page ");
	private Pattern patternStyleOpen = Pattern.compile("<fontspec ");
	private Pattern patternText = Pattern.compile("<text ");
	private Pattern patternPageNo = Pattern.compile(".*number=\"([0-9\\.]+)\".*");
	private Pattern patternWidth = Pattern.compile(".*width=\"([0-9\\.]+)\".*");
	private Pattern patternHeight = Pattern.compile(".*height=\"([0-9\\.]+)\".*");
	private Pattern patternLeft = Pattern.compile(".*left=\"([0-9\\.]+)\".*");
	private Pattern patternTop = Pattern.compile(".*top=\"([0-9\\.]+)\".*");
	private Pattern patternFont = Pattern.compile(".*font=\"([0-9\\.]+)\".*");
	private Pattern patternId = Pattern.compile(".*id=\"([0-9]+)\".*");
	private Pattern patternSize = Pattern.compile(".*size=\"([0-9]+)\".*");
	private Pattern patternFamily = Pattern.compile(".*family=\"([^\"]+)\".*");
	private Pattern patternColor = Pattern.compile(".*color=\"(#[a-z0-9]+)\".*");
	// #41: Change Link Pattern.
	private Pattern patternLink = Pattern.compile(".*<a ?[^>]*>(.+?)<\\/a>.*");
	private Pattern patternWord = Pattern.compile("<text [^>]*>(.*?)<\\/text>");

	private Common common = new Common();
	private ExecutorService executor;

	private PDFToHtml pdf = null;
	private String paraMarker = "LSMARKERLS:PARA";
	private int maxWordsJoin = 5;

	private Object _objectWorker = new Object();
	private HashMap<String, SentenceJoin> _hashSentenceJoin = new HashMap<>();
	private Config config = null;

	private void initial(String logFilePath, int verbose, String configFile, long timeout) throws Exception {
		initial(logFilePath, verbose, configFile, timeout, null, null);
	}
	private void initial(String logFilePath, int verbose, String configFile, long timeout, String kenlmPath, String sentenceJoinPath) throws Exception {
		common.setVerbose(verbose);
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

		try {
			if (common.IsEmpty(configFile) || !common.IsExist(configFile)) {
				configFile = common.getConfigPath();
			}
			config = new Config(configFile, kenlmPath, sentenceJoinPath);
		} catch (Exception e) {
			throw new Exception("initial failed. " + e.getMessage());
		}

		pdf = new PDFToHtml(timeout);

	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @throws Exception
	 */
	public PDFExtract() throws Exception {
		initial("", 0, "", 0);
	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @param configFile The path for configuration file
	 * @throws Exception
	 */
	public PDFExtract(String configFile) throws Exception {
		initial("", 0, configFile, 0);
	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @param timeout The maximum time wait in seconds for poppler extract
	 * @throws Exception
	 */
	public PDFExtract(long timeout) throws Exception {
		initial("", 0, "", timeout);
	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @param configFile The path for configuration file
	 * @param timeout    The maximum time wait in seconds for poppler extract
	 * @throws Exception
	 */
	public PDFExtract(String configFile, long timeout) throws Exception {
		initial("", 0, configFile, timeout);
	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @param logFilePath The path to write the log file to.
	 * @param configFile  The path for configuration file
	 * @param timeout     The maximum time wait in seconds for poppler extract
	 * @throws Exception
	 */
	public PDFExtract(String logFilePath, String configFile, long timeout) throws Exception {
		initial(logFilePath, 0, configFile, timeout);
	}

	/**
	 * Initializes a newly created PDFExtract object.
	 * 
	 * @param logFilePath The path to write the log file to.
	 * @param verbose     Print the information to stdout (1=print, 0=silence)
	 * @param configFile  The path for configuration file
	 * @param timeout     The maximum time wait in seconds for poppler extract
	 * @throws Exception
	 */
	public PDFExtract(String logFilePath, int verbose, String configFile, long timeout) throws Exception {
		initial(logFilePath, verbose, configFile, timeout);
	}
	//#43 Add new arguments for -K<kenlm_path>, -S<sentence_join_path>
	public PDFExtract(String logFilePath, int verbose, String configFile, long timeout, String kenlmPath, String sentenceJoinPath) throws Exception {
		initial(logFilePath, verbose, configFile, timeout, kenlmPath, sentenceJoinPath);
	}

	/**
	 * PDFExtract is a PDF parser that converts and extracts PDF content into a HTML
	 * format that is optimized for easy alignment across multiple language sources.
	 *
	 * @param inputFile     The path to the source PDF file process for extraction
	 * @param outputFile    The path to the output HTML file after extraction
	 * @param keepBrTags    By default <br />
	 *                      is not included in the output. When this argument is
	 *                      specified, then the output will include the <br />
	 *                      tag after each line.
	 * @param getPermission By default the permissions is not included in the
	 *                      output. When this argument is specified, then the output
	 *                      will include permissions tag into header section.
	 */
	public void Extract(String inputFile, String outputFile, int keepBrTags, int getPermission) throws Exception {

		String outputPath = "";
		UUID uuid = UUID.randomUUID();
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

			StringBuffer htmlBuffer = new StringBuffer("");
			AtomicReference<DocumentObject> refDoc = new AtomicReference<DocumentObject>(new DocumentObject());

			if (getPermission == 1) {
				/**
				 * get pdf access permission
				 */
				getAccessPermissions(inputFile, refDoc);
			}

			/**
			 * Call function to convert PDF to HTML
			 */
			htmlBuffer = convertPdfToHtml(inputFile);
//			common.writeLog(logPath, htmlBuffer.toString(), false);

			getHtmlObject(htmlBuffer, refDoc);

			/**
			 * Call function to repair & adjustment
			 */
			repairAndAdjustment(refDoc);

			/**
			 * Call function to do language id
			 */
			languageId(refDoc);

			/**
			 * Call function to join sentence
			 */
			sentenceJoin(uuid.toString(), refDoc);

			/**
			 * Call function to final repair
			 */
			finalRepair(refDoc);

			/**
			 * Call function to generate html output
			 */
			htmlBuffer = generateOutput(refDoc, keepBrTags, getPermission);

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
			// Fix issue #57 Sentence join fails when using a batch file: Checking the model has been being used or not before terminate the model.
			shutdownProcess(uuid.toString());
		}
	}

	/**
	 * PDFExtract is a PDF parser that converts and extracts PDF content into a HTML
	 * format that is optimized for easy alignment across multiple language sources.
	 *
	 * @param inputStream Stream data as ByteArray for extraction
	 * @param keepBrTags  By default <br />
	 *                    is not included in the output. When this argument is
	 *                    specified, then the output will include the <br />
	 *                    tag after each line.
	 *
	 * @return ByteArrayOutputStream Stream Out
	 */
	public ByteArrayOutputStream Extract(ByteArrayInputStream inputStream, int keepBrTags, int getPermission)
			throws Exception {
		UUID uuid = UUID.randomUUID(); 
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
			AtomicReference<DocumentObject> refDoc = new AtomicReference<DocumentObject>(new DocumentObject());

			if (getPermission == 1) {
				/**
				 * get pdf access permission
				 */
				getAccessPermissions(inputStream, refDoc);
			}

			/**
			 * Call function to paint html box
			 */
			htmlBuffer = convertPdfToHtml(inputStream);
			getHtmlObject(htmlBuffer, refDoc);

			/**
			 * Call function to repair & adjustment
			 */
			repairAndAdjustment(refDoc);

			/**
			 * Call function to do language id
			 */
			languageId(refDoc);

			/**
			 * Call function to join sentence
			 */
			sentenceJoin(uuid.toString(), refDoc);

			/**
			 * Call function to generate html output
			 */
			htmlBuffer = generateOutput(refDoc, keepBrTags, getPermission);

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
				common.print("Input Stream", "Error: " + message);
			}

			throw e;
		} finally {
			// Fix issue #57 Sentence join fails when using a batch file: Checking the model has been being used or not before terminate the model.
			shutdownProcess(uuid.toString());
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
	 * @param threadCount The number of threads to run concurrently when processing
	 *                    PDF files. One file can be processed per thread. If not
	 *                    specified, then the default valur of 1 thread is used.
	 * @param keepBrTags  By default <br />
	 *                    is not included in the output. When this argument is
	 *                    specified, then the output will include the <br />
	 *                    tag after each line.
	 *
	 */
	public void Extract(String batchFile, int threadCount, int keepBrTags, int getPermission) throws Exception {
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

				AddThreadExtract(ind, line, keepBrTags, getPermission);
				ind++;
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

		} catch (Exception e) {
			String message = e.getMessage();
			if (writeLogFile) {
				common.writeLog(logPath, message, true);
			} else {
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
			shutdownProcess();
		}
	}

	/**
	 * Add new thread pdf extract
	 */
	private void AddThreadExtract(int index, String line, int keepBrTags, int getPermission) {
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
						Extract(inputFile, outputFile, keepBrTags, getPermission);

					} catch (Exception e) {
						String message = e.getMessage();
						if (writeLogFile) {
							if (runnable)
								common.print(inputFile, "Error: " + message);
							common.writeLog(logPath, inputFile, "Error: " + message, true);
						} else {
							if (!runnable)
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
	 * Get permissions
	 */
	private void getAccessPermissions(String inputFile, AtomicReference<DocumentObject> refDoc) throws Exception {
		DocumentObject doc = refDoc.get();
		PdfReader reader = null;
		try {
			reader = new PdfReader(inputFile);
			setAccessPermissions(reader, refDoc);
			if (!doc.permission.canCopy) {
				pdf.decrypt(reader, inputFile);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		}
	}

	private void getAccessPermissions(InputStream inputStream, AtomicReference<DocumentObject> refDoc)
			throws Exception {
		PdfReader reader = null;
		try {
			reader = new PdfReader(inputStream);
			setAccessPermissions(reader, refDoc);
		} catch (Exception e) {
			throw e;
		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		}
	}

	private void setAccessPermissions(PdfReader reader, AtomicReference<DocumentObject> refDoc) {
		DocumentObject doc = refDoc.get();
		//
		PdfReader.unethicalreading = true;
		int permissions = (int) reader.getPermissions();
		//
		doc.permission.isEncrytped = reader.isEncrypted();
		doc.permission.canAssembly = PdfEncryptor.isAssemblyAllowed(permissions);
		doc.permission.canCopy = PdfEncryptor.isCopyAllowed(permissions);
		doc.permission.canPrint = PdfEncryptor.isPrintingAllowed(permissions);
		doc.permission.canPrintDegraded = PdfEncryptor.isDegradedPrintingAllowed(permissions);
		doc.permission.canModified = PdfEncryptor.isModifyContentsAllowed(permissions);
		doc.permission.canModifyAnnotations = PdfEncryptor.isModifyAnnotationsAllowed(permissions);
		doc.permission.canFillInForm = PdfEncryptor.isFillInAllowed(permissions);
		doc.permission.canScreenReader = PdfEncryptor.isScreenReadersAllowed(permissions);
		doc.permission.verbose = PdfEncryptor.getPermissionsVerbose(permissions);
	}

	/**
	 * Convert PDF to HTML file
	 */
	private StringBuffer convertPdfToHtml(String inputFile) throws Exception {
		return pdf.extract(inputFile);
	}

	/**
	 * Convert PDF to HTML file
	 */
	private StringBuffer convertPdfToHtml(ByteArrayInputStream inputStream) throws Exception {
		return pdf.extract(inputStream);
	}

	/**
	 * Get HTML Object Parse data to HTML Object
	 */
	private void getHtmlObject(StringBuffer htmlBuffer, AtomicReference<DocumentObject> refDoc) throws Exception {
		BufferedReader b_in = null;
		DocumentObject doc = refDoc.get();

		try {

			InputStreamReader i_in = new InputStreamReader(new ByteArrayInputStream(htmlBuffer.toString().getBytes()),
					StandardCharsets.UTF_8);
			b_in = new BufferedReader(i_in);
			String line = "";
			int currentPage = 0;
			PageObject page = new PageObject();
			HashMap<Float, Integer> mapPageWidth = new HashMap<Float, Integer>();
			HashMap<Float, Integer> mapPageHeight = new HashMap<Float, Integer>();
			HashMap<String, StyleObject> mapStyle = new HashMap<String, StyleObject>();

			while ((line = b_in.readLine()) != null) {

				Matcher mStyleOpen = this.patternStyleOpen.matcher(line);
				Matcher patternText = this.patternText.matcher(line);
				Matcher mPageOpen = this.patternPageOpen.matcher(line);

				if (patternText.find()) {

					/**
					 * New text found, call function to get a text object and add into the page
					 */
					TextObject text = getTextObject(line, mapStyle);
					if (!checkLineAdd(page.width, page.height, text)) {
						continue;
					}
					page.texts.add(text);

				} else if (mPageOpen.find()) {

					if (currentPage > 0) {
						doc.pages.add(page);
						Thread.sleep(50);
					}
					currentPage++;
					page = new PageObject();

					/**
					 * New page found, extract page number, width and height of the page
					 */
					page.pageno = common.getInt(patternPageNo.matcher(line).replaceAll("$1"));
					page.height = common.getFloat(patternHeight.matcher(line).replaceAll("$1"));
					page.width = common.getFloat(patternWidth.matcher(line).replaceAll("$1"));

					mapPageWidth.put(page.width, common.getInt(mapPageWidth.get(page.width)) + 1);
					mapPageHeight.put(page.height, common.getInt(mapPageHeight.get(page.height)) + 1);

				} else if (mStyleOpen.find()) {

					/**
					 * Collect new style into the list
					 */
					StyleObject style = new StyleObject();
					style.id = common.getStr(patternId.matcher(line).replaceAll("$1"));
					style.size = common.getInt(patternSize.matcher(line).replaceAll("$1"));
					style.family = common.getStr(patternFamily.matcher(line).replaceAll("$1"));
					style.color = common.getStr(patternColor.matcher(line).replaceAll("$1"));
					mapStyle.put(style.id, style);

				}
			}

			doc.pages.add(page);
			doc.style = mapStyle;

			/**
			 * Get most width and height of the document
			 */
			doc.width = getMaxCount(mapPageWidth);
			doc.height = getMaxCount(mapPageHeight);

		} finally {
			if (b_in != null) {
				b_in.close();
				b_in = null;
			}
			refDoc.set(doc);
		}

	}

	/**
	 * Repair and adjust text with the common rules
	 */
	private void repairAndAdjustment(AtomicReference<DocumentObject> refDoc) throws Exception {

		DocumentObject doc = refDoc.get();
		try {

			LangInfo commonInfo = null;
			if (config != null) {
				commonInfo = config.get("common");
			}
			
			HashMap<String, Integer> mapStyleCount = new HashMap<String, Integer>();

			/**
			 * Get height + normalize text
			 */
			for (PageObject page : doc.pages) {

				HashMap<Float, Integer> mapHeight = new HashMap<Float, Integer>();
				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);

					if (common.IsEmpty(text.text)) {
						text.deleted = true;
						continue;
					}
					
					if (!common.IsEmpty(text.class_)) {
						int count = 0;
						if (mapStyleCount.containsKey(text.class_)) {
							count = mapStyleCount.get(text.class_);
						}
						mapStyleCount.put(text.class_, count + 1);
					}
					
					if (text.height > 0) {
						int count = 0;
						if (mapHeight.containsKey(text.height)) {
							count = mapHeight.get(text.height);
						}
						mapHeight.put(text.height, count + 1);
					}
					if (commonInfo != null) {
						text.text = common.replaceText(commonInfo.normalize, text.text).trim();
					}
					
					
				}

			}
			
			// Get Most Style of Document.
			String keyMostStyle = getMaxLangCount(mapStyleCount);
			StyleObject mostStyle = doc.style.get(keyMostStyle);

			/**
			 * Check top to merge line
			 */
			for (PageObject page : doc.pages) {

				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);

					if (text.deleted)
						continue;
					if (common.IsEmpty(text.text)) {
						text.deleted = true;
						continue;
					}

					StringBuffer sb = new StringBuffer(text.text.trim());
					boolean hasUpdated = false;
					if (i + 1 < len) {
						for (int j = i + 1; j < len; j++) {
							TextObject nextText = page.texts.get(j);

							if (text.deleted)
								continue;
							if (common.IsEmpty(nextText.text)) {
								nextText.deleted = true;
								continue;
							}

							if (!isMergeTop(text, nextText)) {
								i = j - 1;
								break;
							} else {

								// merge
								sb.append(" " + nextText.text.trim());
								if (nextText.islink ) {
									text.color = mostStyle.color;
									text.fontfamily = mostStyle.family;
									text.class_ = mostStyle.id;
								}
								if (nextText.top < text.top)
									text.top = nextText.top;
								if (nextText.bottom > text.bottom)
									text.bottom = nextText.bottom;
								// Change font if nextext is larger than text.
								if (nextText.text.length() > text.text.length()) {
									text.fontfamily = nextText.fontfamily;
									text.color = nextText.color;
									text.class_ = nextText.class_;
								}
								text.right = nextText.right;
								text.width = text.right - text.left;
								text.height = text.bottom - text.top;
								nextText.deleted = true;
								hasUpdated = true;

							}
						}

						if (hasUpdated) {
							text.text = sb.toString();
						}
					}

				}
			}

			/**
			 * Adding paragraph separator
			 */
			for (PageObject page : doc.pages) {
				List<TextObject> newTexts = new ArrayList<TextObject>();
				float fLineSpace = new Float("-99.0");

				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);

					if (text.deleted)
						continue;
					if (common.IsEmpty(text.text))
						continue;
					newTexts.add(text);
					
					if (i + 1 < len) {
						for (int j = i + 1; j < len; j++) {
							TextObject nextText = page.texts.get(j);

							if (nextText.deleted) {
								continue;
							}
							
							if (common.IsEmpty(nextText.text)) {
								continue;
							}
							
							if (text.text.trim().endsWith(",") && isClassChanged(text, nextText)) {
								newTexts.add(getNewText(paraMarker));
								fLineSpace = -1.0f;
							} 
							else 
								if (isTooFar(text, nextText, fLineSpace) 
									|| isFontChanged(text, nextText)
							){
								newTexts.add(getNewText(paraMarker));
								fLineSpace = -1.0f;
							}// Separate paragraph if font style is difference.
							else if (!text.fontfamily.equals(nextText.fontfamily) 
									&& text.text.length() > 6 
									&& nextText.text.length() > 6) {
								newTexts.add(getNewText(paraMarker));
								fLineSpace = -1.0f;
							}
							else {
								if (fLineSpace < 0)
									fLineSpace = nextText.top - text.top;
							}
							break;
						}
					} else {
						newTexts.add(getNewText(paraMarker));
					}

				}

				page.texts.clear();
				page.texts.addAll(newTexts);

			}

			/**
			 * Merge sentence
			 */
			List<JoinWordInfo> joinList = new ArrayList<>();
			List<EOFInfo> eofList = new ArrayList<>();

			if (config != null && config.get("common") != null) {
				joinList = config.get("common").joinWords;
				eofList = config.get("common").absoluteEOF;
			}

			for (PageObject page : doc.pages) {

				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);

					if (text.deleted)
						continue;
					if (text.text.equals(paraMarker))
						continue;

					boolean isJoin = false;
					if (i + 1 < len) {
						for (int j = i + 1; j < len; j++) {
							TextObject nextText = page.texts.get(j);

							if (nextText.deleted)
								continue;
							if (nextText.text.equals(paraMarker))
								break;
							if (!text.lang.equals(nextText.lang))
								break;

							boolean isEOF = false;
							// absolute_eof
							for (EOFInfo rule : eofList) {
								if (!common.IsEmpty(rule.front) && !common.IsEmpty(rule.back)) {
									if (text.text.matches(rule.front) && nextText.text.matches(rule.back))
										isEOF = true;
								} else if (!common.IsEmpty(rule.front)) {
									if (text.text.matches(rule.front))
										isEOF = true;
								} else if (!common.IsEmpty(rule.back)) {
									if (nextText.text.matches(rule.back))
										isEOF = true;
								}

								if (isEOF) {
									isJoin = false;
									break;
								}
							}

							if (!isEOF) {
								// join_words
								for (JoinWordInfo rule : joinList) {
									if (!common.IsEmpty(rule.front) && !common.IsEmpty(rule.back)) {
										if (text.text.matches(rule.front) && nextText.text.matches(rule.back))
											isJoin = true;
									} else if (!common.IsEmpty(rule.front)) {
										if (text.text.matches(rule.front))
											isJoin = true;
									} else if (!common.IsEmpty(rule.back)) {
										if (nextText.text.matches(rule.back))
											isJoin = true;
									}

									if (isJoin) {
										text.text += rule.joinText + nextText.text;
										nextText.deleted = true;
										break;
									}
								}
							}

							if (!isJoin) {
								i = j - 1;
								break;
							}
						}
					}

				}
			}

		} finally {
			refDoc.set(doc);
		}
	}

	/**
	 * Detect language + normalize text with language rules
	 */
	private void languageId(AtomicReference<DocumentObject> refDoc) {
		DocumentObject doc = refDoc.get();
		try {
			DetectLanguage detectLang = null;
			try {
				detectLang = new DetectLanguage();
			} catch (java.lang.UnsatisfiedLinkError e) {
				doc.warningList.add(new WarnObject("languageId", common.getStackTrace(e)));
			} catch (java.lang.Exception e) {
				doc.warningList.add(new WarnObject("languageId", common.getStackTrace(e)));
			}

			if (detectLang == null)
				return;

			List<NormalizeInfo> normalizeList = new ArrayList<>();
			LangInfo langInfo = new LangInfo();

			// language id
			HashMap<String, Integer> mapLang = new HashMap<>();

			List<LanguageResult> results = null;
			for (PageObject page : doc.pages) {

				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);

					if (text.deleted)
						continue;
					if (text.text.equals(paraMarker))
						continue;

					if (!canDetectLang(text)) {
						continue;
					}

					/**
					 * Call detect language
					 */
					results = detectLang.find(text.text);
					if (results != null && results.size() > 0) {
						LanguageResult lang = results.get(0);
						if (lang.reliable) {
							text.lang = lang.language;

							if (config != null) {
								langInfo = config.get(text.lang);
								if (langInfo != null) {
									normalizeList = langInfo.normalize;

									/**
									 * Normalize text by language
									 */
									text.text = common.replaceText(normalizeList, text.text);
								}
							}

							int count = 0;
							if (mapLang.containsKey(lang.language)) {
								count = mapLang.get(lang.language);
							}
							mapLang.put(lang.language, count + 1);
						}
					}
				}
			}

			/**
			 * Get most language and document language list
			 */
			doc.language = getMaxLangCount(mapLang);
			doc.langList = getLangList(mapLang);

		} catch (Exception e) {

		} finally {
			refDoc.set(doc);
		}
	}

	/**
	 * Sentence Join
	 */
	private void sentenceJoin(String uuid, AtomicReference<DocumentObject> refDoc) {
		DocumentObject doc = refDoc.get();
		if (common.IsEmpty(doc.language))
			return;

		try {

			for (PageObject page : doc.pages) {

				String currentLang = "";
				List<TextObject> texts = new ArrayList<>();
				List<TextObject> newTexts = new ArrayList<>();
				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);

					if (text.deleted)
						continue;

					if (common.IsEmpty(text.lang))
						text.lang = doc.language;
					
					if (i == 0 || common.IsEmpty(currentLang)) {

						// start first chunk
						if (!common.IsEmpty(text.lang)) {
							currentLang = text.lang;
						} else {
							currentLang = doc.language;
						}
						texts.add(text);

					} else if (text.text.equals(paraMarker) || (!common.IsEmpty(currentLang)
							&& !common.IsEmpty(text.lang) && !text.lang.equals(currentLang))) {

						// do sentence join
						if (text.text.equals(paraMarker))
							newTexts.add(text);

						if (!text.lang.equals(currentLang)) {
							newTexts.add(getNewText(paraMarker));
						}

						newTexts.addAll(sentenceJoin(uuid, texts, currentLang));

						// start new chunk
						texts.clear();

						if (!text.text.equals(paraMarker)) {
							texts.add(text);
							if (!common.IsEmpty(text.lang)) {
								currentLang = text.lang;
							} else {
								currentLang = doc.language;
							}
						} else {
							currentLang = "";
						}
					} else {
						texts.add(text);
					}
				}

				if (texts.size() > 0) {
					newTexts.add(getNewText(paraMarker));
					newTexts.addAll(sentenceJoin(uuid, texts, currentLang));
					texts.clear();
				}

				
				// Clean Paramaker
				List<TextObject> tempTextJoin = new ArrayList<>();
				int processingIndex = 0;
				for (int i = 0; i < newTexts.size() - 2 ; i ++ ) {
					
					for (int j = i+1; j < newTexts.size(); j++) {
						if ((paraMarker.equals(newTexts.get(j).text) || newTexts.get(j).deleted) && !newTexts.get(j-1).deleted) {
							processingIndex = j-1;
							
							if (paraMarker.equals(newTexts.get(j).text)) {
								for (int k = j+1; k < newTexts.size(); k++) {
									if (!newTexts.get(k).deleted) {
										TextObject textleft = newTexts.get(processingIndex);
										TextObject textright = newTexts.get(k);
										if (!textleft.lang.equals(textright.lang)){
											processingIndex = k;
											break;
										}
										tempTextJoin.add(textleft);
										tempTextJoin.add(textright);
										
										tempTextJoin = sentenceJoin(uuid, tempTextJoin, textleft.lang);

										if (tempTextJoin.size() > 1) {
											processingIndex = k;
										}else{
											newTexts.get(processingIndex).text = tempTextJoin.get(0).text;
											newTexts.get(k).deleted = true;
											newTexts.get(j).deleted = true;
										}
										tempTextJoin = new ArrayList<HTMLObject.TextObject>();
										break;
									}
								}
								
								if (processingIndex > j) {
									i = processingIndex - 1;
									break;
								}
							}
						}
					}
					
				}

				page.texts.clear();
				page.texts.addAll(newTexts);
			}

		}catch (Exception e){
			
			e.printStackTrace();
		}finally {
			refDoc.set(doc);
		}
	}

	/**
	 * Final repair with the configuration rules
	 */
	private void finalRepair(AtomicReference<DocumentObject> refDoc) {
		DocumentObject doc = refDoc.get();

		try {

			List<NormalizeInfo> repairList = new ArrayList<>();
			if (config.get("common") != null) {
				repairList = config.get("common").repair;
			}

			LangInfo langInfo = new LangInfo();

			for (PageObject page : doc.pages) {

				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);

					if (text.deleted)
						continue;
					if (text.text.equals(paraMarker))
						continue;

					text.text = common.replaceText(repairList, text.text);

					if (common.IsEmpty(text.lang))
						continue;

					langInfo = config.get(text.lang);
					if (langInfo != null) {
						List<NormalizeInfo> langRepairList = langInfo.repair;
						text.text = common.replaceText(langRepairList, text.text);
					}

				}
			}

		} finally {
			refDoc.set(doc);
		}
	}

	/**
	 * Generate final output with html format
	 */
	private StringBuffer generateOutput(AtomicReference<DocumentObject> refDoc, int keepBrTags, int getPermission) {


		DocumentObject doc = refDoc.get();
		StringBuffer sbOut = new StringBuffer();
		boolean isKenlmError = false;

		// html
		sbOut.append("<html>\n");

		// header
		sbOut.append("<head>\n");
		sbOut.append("<defaultLang abbr=\"" + doc.language + "\" />\n");
		sbOut.append("<languages>\n");

		List<String> noModel = new ArrayList<>();
		List<String> errorModel = new ArrayList<>();

		for (LangObject lang : doc.langList) {
			// Add row count by language.
			sbOut.append("<language abbr=\"" + lang.name + "\" percent=\"" + lang.percent + "\"" + " rows=\"" + lang.count + "\" "   + "/>\n");
			SentenceJoin sj = _hashSentenceJoin.get(lang.name);
			
			// #51 :To prevent Spurious warnings about sentenceJoin models.
			if (_hashSentenceJoin.containsKey(lang.name) && sj == null) {
				noModel.add(lang.name);
			} else if (null != sj && sj.status() == WorkerStatus.ERROR) {
				isKenlmError = sj.isKenlmError;
				errorModel.add(lang.name);
			}
		}
		
		
		// #54 Accurate the error warning message.		
		String scriptPath = config.getSentenceJoinScript();
		if (StringUtils.isEmpty(scriptPath) || !common.IsExist(scriptPath)) {
			doc.warningList
			.add(new WarnObject("sentenceJoin", "Fail loading Python script sentence-join.py","Please specify/verify the \"sentence_join\" value in the configuration files."));
		}else {

			if (errorModel.size() > 0) {
				
				if (isKenlmError) {
					doc.warningList.add(
							new WarnObject("sentenceJoin", "Fail loading model for language: " + String.join(", ", errorModel) + ""
									, "Please verify the KenLM path on the argument of command-line."));
				}else {
					doc.warningList.add(
							new WarnObject("sentenceJoin", "Fail loading model for language: " + String.join(", ", errorModel) + ""
									, "Please verify the \"sentencejoin_model\" value of language {" + String.join(", ", errorModel) +"} in configuration file."));
				}
				
			}
			
			if (noModel.size() > 0) {
				doc.warningList
						.add(new WarnObject("sentenceJoin", "No model for language: " + String.join(", ", noModel) + ""
								, "Please specify the \"sentencejoin_model\" value of language {" + String.join(", ", noModel) +"} in configuration file."));
			}
		}

		
		sbOut.append("</languages>\n");
		if (doc.warningList.size() > 0) {
			sbOut.append("<warnings>" + "\n");
			for (WarnObject warnObj : doc.warningList) {
				sbOut.append("<warning>" + "\n");
				sbOut.append("<method>" + warnObj.method + "</method>" + "\n");
				sbOut.append("<details>\n");
				sbOut.append("\t<message>");
				
				sbOut.append("<![CDATA[" + "");
				sbOut.append(warnObj.detail);
				sbOut.append("]]>" + "");
				sbOut.append("</message>" + "\n");
				sbOut.append("\t<suggestion>");
				sbOut.append("<![CDATA[" + "");
				sbOut.append(warnObj.solution);
				sbOut.append("]]>" + "");
				sbOut.append("</suggestion>" + "\n");
				sbOut.append("</details>" + "\n");
				sbOut.append("</warning>" + "\n");
			}
			sbOut.append("</warnings>" + "\n");
		}
		// End Accurate the error warning message.
		
		if (getPermission == 1) {
			sbOut.append("<permission isencrypted=\"" + doc.permission.isEncrytped + "\">" + "\n");
			sbOut.append("<canassemply>" + doc.permission.canAssembly + "</canassemply>" + "\n");
			sbOut.append("<cancopy>" + doc.permission.canCopy + "</cancopy>" + "\n");
			sbOut.append("<canmodified>" + doc.permission.canModified + "</canmodified>" + "\n");
			sbOut.append(
					"<canmodifyannotations>" + doc.permission.canModifyAnnotations + "</canmodifyannotations>" + "\n");
			sbOut.append("<canprint>" + doc.permission.canPrint + "</canprint>" + "\n");
			sbOut.append("<canprintdegraded>" + doc.permission.canPrintDegraded + "</canprintdegraded>" + "\n");
			sbOut.append("<canfillinform>" + doc.permission.canFillInForm + "</canfillinform>" + "\n");
			sbOut.append("<canscreenreader>" + doc.permission.canScreenReader + "</canscreenreader>" + "\n");
			sbOut.append("</permission>" + "\n");
		}
		sbOut.append("</head>\n");

		// body
		sbOut.append("<body>\n");
		for (PageObject page : doc.pages) {

			// page
			sbOut.append("<div id=\"page" + page.pageno + "\" class=\"page\">\n");

			int ipara = 1;
			boolean bpara = false;

			if (page.texts.size() > 0) {

				for (int i = 0, len = page.texts.size(); i < len; i++) {
					TextObject text = page.texts.get(i);
					if (text.deleted)
						continue;
					if (common.IsEmpty(text.text))
						continue;

					if (text.text.equals(paraMarker) && !text.deleted) {
						if (bpara) {
							sbOut.append("</p>\n");
							bpara = false;
						}

						if (i + 1 < len) {

							if (page.texts.get(i + 1).text.equals(paraMarker) && !page.texts.get(i + 1).deleted)
								continue;
							if (common.IsEmpty(page.texts.get(i + 1).text))
								continue;

							String lang = page.texts.get(i + 1).lang;
							String font = page.texts.get(i + 1).fontfamily;

							// paragraph
							sbOut.append("<p id=\"page" + page.pageno + "p" + ipara++ + "\" lang=\"" + lang
									+ "\" fontname=\"" + font + "\">\n");
							bpara = true;
						}
					} else {
						if (!bpara) {
							// paragraph
							sbOut.append("<p id=\"page" + page.pageno + "p" + ipara++ + "\" lang=\"" + text.lang
									+ "\" fontname=\"" + text.fontfamily + "\">\n");
							bpara = true;
						}

						// line
						sbOut.append(text.text.trim());
						sbOut.append((keepBrTags == 1 ? "<br />" : "") + "\n");
					}
				}
				if (ipara > 1) {
					sbOut.append("</p>\n");
				}
			}

			sbOut.append("</div>\n");

		}
		sbOut.append("</body>\n");
		sbOut.append("</html>\n");

		return sbOut;
	
	}

	/**
	 * Extract text properties into object
	 */
	private TextObject getTextObject(String line, HashMap<String, StyleObject> mapStyle) {
		TextObject obj = new TextObject();

		obj.top = common.getFloat(patternTop.matcher(line).replaceAll("$1"));
		obj.left = common.getFloat(patternLeft.matcher(line).replaceAll("$1"));
		obj.width = common.getFloat(patternWidth.matcher(line).replaceAll("$1"));
		obj.height = common.getFloat(patternHeight.matcher(line).replaceAll("$1"));
		obj.bottom = obj.top + obj.height;
		obj.right = obj.left + obj.width;
		obj.text = common.getStr(patternWord.matcher(line).replaceAll("$1"));
		obj.class_ = common.getStr(patternFont.matcher(line).replaceAll("$1"));

		if (mapStyle != null && mapStyle.containsKey(obj.class_)) {
			StyleObject css = mapStyle.get(obj.class_);
			obj.fontsize = css.size;
			obj.fontfamily = css.family;
			obj.color = css.color;
		}

		String text = obj.text;

		if (patternLink.matcher(text).matches()) {
			obj.islink = true;
		}

		text = text.replaceAll("<br\\/>", "_LSBRLS_");
		text = text.replaceAll("<[^>]*>", "");
		text = text.replaceAll("&#160;", " ").replaceAll("\\s{2,100}", " ");
		text = text.replaceAll("_LSBRLS_", "<br/>");
		obj.text = text;

		return obj;
	}

	/**
	 * Consider to add text object into the page
	 */
	private boolean checkLineAdd(float pageWidth, float pageHeight, TextObject text) {
		if (text.left < 0 || text.top < 0 || text.left > pageWidth || text.top > pageHeight) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Check next font change
	 */
	private boolean isFontChanged(TextObject text, TextObject nextText) {
		if ((text.fontsize != nextText.fontsize && !isEquals(text.height, nextText.height))
				|| (!text.color.equals(nextText.color) && !isEquals(text.top, nextText.top))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check next class change
	 */
	private boolean isClassChanged(TextObject text, TextObject nextText) {
		if (!text.class_.equals(nextText.class_)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get maximum count of the list
	 */
	private Float getMaxCount(HashMap<Float, Integer> map) {

		if (map == null || map.size() == 0) {
			return 0f;
		} else {
			// #37, Fix to remove sorting for getting max
			Map.Entry<Float, Integer> maxEntry = null;
			for (Map.Entry<Float, Integer> entry : map.entrySet())
			{
			    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
			    {
			        maxEntry = entry;
			    }
			}
			
			return maxEntry.getKey();
		}
	}

	/**
	 * Get maximum language count
	 */
	private String getMaxLangCount(HashMap<String, Integer> map) {

		if (map == null || map.size() == 0) {
			return "";
		} else {
			// #37, Fix to remove sorting for getting max
			Map.Entry<String, Integer>  maxEntry = null;
			for (Map.Entry<String, Integer>  entry : map.entrySet())
			{
			    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
			    {
			        maxEntry = entry;
			    }
			}
			
			return maxEntry.getKey();
		}
	}

	/**
	 * Create new text object with value
	 */
	private TextObject getNewText(String val) {
		TextObject t = new TextObject();
		t.text = val;
		return t;
	}

	/**
	 * Compare two numbers
	 */
	private boolean isEquals(Float f1, Float f2) {
		if (Math.abs(f1 - f2) <= 8) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check next text is too far
	 * Modify the method for line space and left position to specify paragraph.
	 */
	private boolean isTooFar(TextObject text, TextObject nextText, float fLinespace) {
		boolean bResult;
		float ftempLineSpace = 0.0f;
		if (fLinespace > 0) {
			ftempLineSpace = Math.abs(text.top - nextText.top);
			if ((ftempLineSpace >= 0.7*fLinespace && ftempLineSpace <= 1.30*fLinespace) && 
					(text.left - nextText.left  < 0  ||  text.left - nextText.left > 0 )) {
				bResult = true;
			}else if (ftempLineSpace < 0.7*fLinespace || ftempLineSpace > 1.30*fLinespace){
				bResult = true;
			}else {
				bResult = false;
			}
		}else {
			if ((Math.abs(text.top - nextText.top) > (text.height + nextText.height)*1.5) || !(Math.abs(text.left - nextText.left)  <= 100 && Math.abs(text.left - nextText.left) >=0 ) 
//					|| (Math.abs((text.width + text.left)  - (nextText.left + nextText.width)) >= 50 )
					|| (text.text.length() <=10)
					) {
				bResult = true;
			} else {
				bResult = false;
			}
		}
		return bResult;
	}

	/**
	 * Consider top position to merge an object
	 */
	private boolean isMergeTop(TextObject text, TextObject nextText) {
		if (isEquals(text.top, nextText.top) && !isFontChanged(text, nextText) && nextText.left - text.right < 200) {
			return true;
		}
		return false;
	}

	/**
	 * Consider text can detect language or not If it too short sentence, no detect
	 */
	private boolean canDetectLang(TextObject text) {
		String str = text.text;
		str = str.replaceAll(
				"[\\?\\!\\,0-9\\-\\_\\#\\*\\&\\(\\)\\+\\=\\@\\%\\<\\>\\{\\}\\[\\]\\^\\\\\\/\\;\\.{2,100}]*", "");
		str = str.replaceAll("\\s\\.{2,100}", "");
		str = str.replaceAll("\\:", " ");
		str = str.replaceAll("[σΦϕ∈†τψη∇π∑∂αβ→∏ε]*", "");
		str = str.replaceAll("\\s{2,100}", " ");
		str = str.trim();

		int wordlength = str.split(" ", -1).length;
		int charlength = str.replaceAll(" ", "").length();
		if (wordlength > 10 || charlength > 30) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Collect languages detected in the document and calculate percentage
	 * appearance
	 */
	private List<LangObject> getLangList(HashMap<String, Integer> map) {

		List<LangObject> langList = new ArrayList<>();

		// total
		float total = 0;
		for (Map.Entry<String, Integer> hash : map.entrySet()) {
			total += common.getDouble(hash.getValue());
		}

		// calculate percentage
		for (Map.Entry<String, Integer> hash : map.entrySet()) {
			float percent = (common.getFloat(hash.getValue()) * 100) / total;

			LangObject lang = new LangObject();
			lang.name = hash.getKey();
			lang.percent = percent;
			lang.count = hash.getValue();
			langList.add(lang);
		}

		// sort
		Collections.sort(langList, new Comparator<LangObject>() {
			@Override
			public int compare(LangObject l1, LangObject l2) {
				return Float.compare(l2.percent, l1.percent);
			}
		});

		return langList;
	}

	/**
	 * Get first n words of the line
	 */
	private String getFirstWords(String str) {

		String[] words = str.split(" ");
		int wordlength = words.length;

		String newStr = "";
		for (int i = 0; i <= maxWordsJoin && i < wordlength; i++) {
			newStr += " " + words[i];
		}

		return newStr.trim();
	}

	/**
	 * Get last n words of the line
	 */
	private String getLastWords(String str) {

		String[] words = str.trim().split(" ");
		int wordlength = words.length;

		int count = 0;
		String newStr = "";
		for (int i = wordlength - 1; i >= 0 && count <= maxWordsJoin; i--, count++) {
			newStr = words[i] + " " + newStr;
		}

		return newStr.trim();
	}

	/**
	 * Get sentence join model for language
	 */
	private String getSentenceJoinModel(String lang) {
		if (config != null && config.get(lang) != null) {
			return config.get(lang).sentenceJoinModel;
		}
		return "";
	}

	/**
	 * Execute sentence join by language
	 */
	private List<TextObject> sentenceJoin(String uuid, List<TextObject> texts, String lang) {
		
		List<TextObject> newTexts = new ArrayList<>();
		try {

			if (texts.size() < 2) {
				return texts;
			}

			SentenceJoin sj;

			synchronized (_objectWorker) {

				if (_hashSentenceJoin.containsKey(lang)) {
					sj = _hashSentenceJoin.get(lang);
					// Fix issue #57 Sentence join fails when using a batch file: Set last execute time.
					sj.get_registerid().add(uuid);
				} else {

					String scriptPath = config.getSentenceJoinScript();
					String sKenlmPath = config.getKenlmPath();
					String modelPath = getSentenceJoinModel(lang);
					if (common.IsExist(scriptPath) && !common.IsEmpty(modelPath)) {
						// #43 Change Request: Poppler rewrite
						sj = new SentenceJoin(lang, scriptPath, modelPath, sKenlmPath);
						_hashSentenceJoin.put(lang, sj);
					} else {
						sj = null;
						_hashSentenceJoin.put(lang, sj);
					}
				}
				
				// Fix Issue #35 to prevent call start() multiple time in one language id.
				if (sj != null && sj.status() != WorkerStatus.RUNNING && sj.status() != WorkerStatus.LOADING && sj.status() != WorkerStatus.ERROR) {
					sj.start();
					// Fix issue #57 Sentence join fails when using a batch file: Set last execute time.
					sj.get_registerid().add(uuid);
				}
			}

			if (sj != null && sj.status() == WorkerStatus.RUNNING) {
				// Fix issue #57 Sentence join fails when using a batch file: Set last execute time.
				sj.get_registerid().add(uuid);
				for (int i = texts.size() - 1, start = 0; i >= start; i--) {
					TextObject text = texts.get(i);

					if (i - 1 >= 0) {
						TextObject prevText = texts.get(i - 1);

						String text1 = getLastWords(prevText.text).trim();
						String text2 = getFirstWords(text.text).trim();

						boolean isJoin = false;
						if (!common.IsEmpty(text1) && !common.IsEmpty(text2) 
								&& !text1.trim().endsWith(".")
								&& !text2.trim().startsWith("•") && !text1.trim().equals(" ")
								&& !text2.trim().equals(" ")) {
							isJoin = sj.execute(text1, text2);
						}

						if (isJoin) {
							prevText.text = prevText.text.trim() + " " + text.text.trim();
							text.deleted = true;
						} else {
							newTexts.add(text);
						}
					} else {
						newTexts.add(text);
					}

				}

				return Lists.reverse(newTexts);

			}

		} catch (Exception e) {
			common.print(e.getMessage());
		}

		return texts;
	}

	/**
	 * Shutdown sentence join process
	 */
	public void shutdownProcess() throws Exception {
		try {
			if (_hashSentenceJoin != null && _hashSentenceJoin.size() > 0) {
				for (Map.Entry<String, SentenceJoin> hash : _hashSentenceJoin.entrySet()) {
					if (hash.getValue() != null) {
						hash.getValue().stop();
						hash.getValue().setStatus(WorkerStatus.STOPPED);
					}
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}
	
	// Fix issue #57 Sentence join fails when using a batch file: Checking the model has been being used or not before terminate the model.
	public void shutdownProcess(String uuid) throws Exception {
		try {
			if (_hashSentenceJoin != null && _hashSentenceJoin.size() > 0) {
				for (Map.Entry<String, SentenceJoin> hash : _hashSentenceJoin.entrySet()) {
					
					
					if (hash.getValue() != null ) {
						Set<String> registerid = hash.getValue().get_registerid();
//						common.print(hash.getKey() + " " + uuid + " *Before Size:::::::::" + registerid.size());
//						common.print(hash.getKey() + " " + registerid);
						if (registerid.contains(uuid)) {
							registerid.remove(uuid);
//							common.print(hash.getKey() + " " +"*After Size:::::::::" + registerid.size());
							if (registerid.size() == 0) {
								hash.getValue().setStatus(WorkerStatus.STOPPED);
								hash.getValue().stop();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}
	
}