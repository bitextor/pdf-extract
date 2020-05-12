package pdfextract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author MickeyVI
 */
public class SentenceJoin {

	private Common common = new Common();

	private String _sScriptPath = "";
	private InputStream _inStream = null;
	private OutputStream _outStream = null;
	private InputStream _errorStream = null;
	private PrintWriter _pWriter = null;
	private InputStreamReader _reader = null;
	private InputStreamReader _readerError = null;
	private BufferedReader _buffError = null;
	private Scanner _scan = null;
	private Process _proc = null;

	private WorkerStatus _workerStatus;
	private String _language = "";
	private String _modelPath = "";
	private ExecutorService _executor;
	private Object _objectWorker = new Object();

	/**
	 * Constructor
	 */
	public SentenceJoin(String lang, String scriptPath, String modelPath) {
		_language = lang;
		_sScriptPath = scriptPath;
		_modelPath = modelPath;
		_executor = Executors.newSingleThreadExecutor();
	}

	/**
	 * Worker Status
	 */
	public enum WorkerStatus {
		STOPPED, LOADING, RUNNING, ERROR
	}

	/**
	 * Get Worker Status
	 */
	public WorkerStatus status() {
		return _workerStatus;
	}

	/**
	 * Start sentence join process
	 */
	public void start() throws Exception {
		if (_workerStatus == WorkerStatus.RUNNING || _workerStatus == WorkerStatus.LOADING)
			return;
		if (!common.IsExist(_sScriptPath))
			return;
		if (common.IsEmpty(_modelPath))
			return;

		_workerStatus = WorkerStatus.LOADING;
		
		// fix issue #35 to prevent invalid _workerStatus
		try {
			// Execute command line to start process
			String[] commands = new String[4];
			commands[0] = _sScriptPath;
			commands[1] = "--apply";
			commands[2] = "--model";
			commands[3] = _modelPath;
	
			ProcessBuilder proc = new ProcessBuilder(commands);
			_proc = proc.start();
	
			_inStream = _proc.getInputStream();
			_outStream = _proc.getOutputStream();
			_errorStream = _proc.getErrorStream();
	
			// Set reader and scaner
			_reader = new InputStreamReader(_inStream, "UTF-8");
			_readerError = new InputStreamReader(_errorStream, "UTF-8");
	
			// Check error
			_buffError = new BufferedReader(_readerError);
			_scan = new Scanner(_reader);
			_pWriter = new PrintWriter(_outStream);

			// Run test
			_pWriter.println("test\ttest");
			_pWriter.flush();

			_scan.nextLine();
			_workerStatus = WorkerStatus.RUNNING;

		} catch (Exception e) {
			// If load model not finish. It must go to this error.
			_workerStatus = WorkerStatus.ERROR;
			throw new Exception("Start sentence join [" + _language + "] failed");
		}

		_executor.shutdown();
	}

	/**
	 * Stop sentence join process
	 */
	public void stop() throws Exception {
		try {
			try {
				if (_outStream != null)
					_outStream.close();
			} catch (IOException e) {
			}

			if (_inStream != null)
				_inStream.close();
			if (_pWriter != null)
				_pWriter.close();
			if (_buffError != null)
				_buffError.close();
			if (_reader != null)
				_reader.close();
			if (_readerError != null)
				_readerError.close();
			if (_errorStream != null)
				_errorStream.close();
			if (_scan != null)
				_scan.close();

			if (_proc != null)
				_proc.destroy();
			_proc = null;

		} catch (Exception e) {
			common.print("stop sentence join [" + _language + "] failed. " + e.getMessage());
		} finally {
		}

	}

	/**
	 * Execute sentence join and get result
	 */
	public boolean execute(String text1, String text2) throws Exception {

		synchronized (_objectWorker) {
			try {

				_pWriter.println(text1 + "\t" + text2);
				_pWriter.flush();

				String sOutput = _scan.nextLine();
				return common.getBool(sOutput);

			} catch (Exception e) {
				common.print("execute sentence join [" + _language + "] failed. " + text1 + "\t" + text2 + " ,"
						+ e.getMessage());
			}finally {
				//Fix #36, to read stderr for prevent deadlock.
				print_error();
			}
		}

		return false;
	}
	
	//Fix #36, to read stderr for prevent deadlock.
	private void print_error() {
		try {
			if (_buffError.ready()) {
				String line = "";
				while ((line = _buffError.readLine()) != null) {
					common.print(line);
					if (!_buffError.ready()) {
						break;
					}
				}
			}
		} catch (IOException e) {
			common.print("execute sentence join failed. "+ e.getMessage());
		}
	}
}
