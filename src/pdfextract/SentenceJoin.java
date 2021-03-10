package pdfextract;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;

/**
 * @author MickeyVI
 */
public class SentenceJoin {

	private Common common = new Common();

	private String _sScriptPath = "";
	private String _skenlmPath = "";
	private Process _proc = null;

	private WorkerStatus _workerStatus;
	private String _language = "";
	private String _modelPath = "";
	private ExecutorService _executor;
	private long _lastExecuteTime;
	private Object _objectWorker = new Object();
	private StreamGobblerWithOutput _errorStreamGobbler;
	private StreamGobblerWithOutput _inputStreamGobbler;
	public boolean isKenlmError = false;


	/**
	 * Constructor
	 */
	public SentenceJoin(String lang, String scriptPath, String modelPath) {
		_language = lang;
		_sScriptPath = scriptPath;
		_modelPath = modelPath;
		_executor = Executors.newSingleThreadExecutor();
	}
	
	public SentenceJoin(String lang, String scriptPath, String modelPath, String sKenlmPath) {
		_language = lang;
		_sScriptPath = scriptPath;
		_modelPath = modelPath;
		_skenlmPath = sKenlmPath;
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
	
	// Set worker Status
	public void setStatus(WorkerStatus status) {
		 this._workerStatus = status;
	}

	public long get_lastExecuteTime() {
		return _lastExecuteTime;
	}

	public void set_lastExecuteTime(long _lastExecuteTime) {
		this._lastExecuteTime = _lastExecuteTime;
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
			StringBuilder sbError = new StringBuilder();
			// Execute command line to start process
			String[] commands = new String[4];
			if (!StringUtils.isEmpty(_skenlmPath)){ 
				commands = new String[6];
			}
			
			commands[0] = _sScriptPath;
			commands[1] = "--apply";
			commands[2] = "--model";
			commands[3] = _modelPath;
			
			// #43 Poppler rewrite 
			if (!StringUtils.isEmpty(_skenlmPath)){
				commands[4] = "--kenlm_path";
				commands[5] = _skenlmPath;
			}
			
			ProcessBuilder proc = new ProcessBuilder(commands);
			//#50 Set to false to prevent error: This binary file contains Trie with Quantization and array-compressed pointers.
			proc.redirectErrorStream(false); // setting  true
			_proc = proc.start();
	
			// Fix issue #36 Deadlock if SentenceJoin writes to stderr
			this._inputStreamGobbler = new StreamGobblerWithOutput("InputStreamST", _proc.getInputStream(), _proc.getOutputStream(), "test\teating");
			this._errorStreamGobbler = new StreamGobblerWithOutput("ErrorStreamST", _proc.getErrorStream());
			Thread tInput = new Thread(this._inputStreamGobbler);
			Thread tError = new Thread(this._errorStreamGobbler);
			
			tError.start();
			tInput.start();
			tInput.join();

			sbError = _errorStreamGobbler.getOutputBuffer();
			
			if (sbError.length() > 0) {
			    // #55 to skip the "This binary file contains trie with quantization and array-compressed pointers." from KenLM.
				// and check error with system exit code.
				String sTemp = sbError.toString();
				sTemp = sTemp.replaceAll("(This binary file contains trie with quantization and array\\-compressed pointers\\.)(\\n*)", "");
				if (!_proc.isAlive() && !StringUtils.isEmpty(sTemp) && _proc.exitValue() != 0)
					throw new Exception(sTemp.toString());
				else {
					_workerStatus = WorkerStatus.RUNNING;
				}
			}else {
				// success
				_workerStatus = WorkerStatus.RUNNING;
			}
			_inputStreamGobbler.getSentenceJoin("Someone\tplays football");


		} catch (Exception e) {
			// If load model not finish. It must go to this error.
//			e.printStackTrace();
			// #54 Accurate the error warning message.
			if (null != e.getMessage() && e.getMessage().contains("KenLM")) {
				isKenlmError = true;
			}
			_workerStatus = WorkerStatus.ERROR;
			throw new Exception("Start sentence join [" + _language + "] failed: " + e.getMessage());
		}

		_executor.shutdown();
	}

	/**
	 * Stop sentence join process
	 */
	public void stop() throws Exception {
		try {
			
			if (null != this._inputStreamGobbler)
				this._inputStreamGobbler.CloseBuffer();
			
			if (null != this._errorStreamGobbler)
				this._errorStreamGobbler.CloseBuffer();

			if (_proc != null)
				_proc.destroy();
			_proc = null;

		} catch (Exception e) {
			// e.printStackTrace();
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
				String sOutput = "";
				// Fix issue #57 Sentence join fails when using a batch file: Set last execute time.
				set_lastExecuteTime(new Date().getTime());
				sOutput = _inputStreamGobbler.getSentenceJoin(text1 + "\t" + text2);
				return common.getBool(sOutput);

			} catch (Exception e) {
				// e.printStackTrace();
				common.print("execute sentence join [" + _language + "] failed. " + text1 + "\t" + text2 + " ,"
						+ e.getMessage());
			}
		}

		return false;
	}
	
}
