package pdfextract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author MickeyVI
 */
public class Config {

	private Common common = new Common();
	private String _path = "";
	private ConfigInfo _config = new ConfigInfo();

	public Config(String path) throws Exception {
		_path = path;
		load();
	}

	private List<NormalizeInfo> getNormalizeList(JSONArray jArray) {
		List<NormalizeInfo> list = new ArrayList<>();
		if (jArray != null) {
			for (int j = 0, jlen = jArray.size(); j < jlen; j++) {

				JSONArray jNormalize = (JSONArray) jArray.get(j);

				String left = jNormalize.getString(0);
				String right = jNormalize.getString(1);

				if (!common.IsEmpty(left) && right.length() > 0) {
					NormalizeInfo norm = new NormalizeInfo();
					norm.search = left;
					norm.replace = right;
					list.add(norm);
				}
			}
		}
		return list;
	}

	private List<JoinWordInfo> getJoinWordsList(JSONArray jArray) {
		List<JoinWordInfo> list = new ArrayList<>();
		if (jArray != null) {
			for (int j = 0, jlen = jArray.size(); j < jlen; j++) {

				JSONArray jNormalize = (JSONArray) jArray.get(j);

				String front = jNormalize.getString(0);
				String back = jNormalize.getString(1);
				String replace = jNormalize.getString(2);

				JoinWordInfo joinw = new JoinWordInfo();
				joinw.front = front;
				joinw.back = back;
				joinw.joinText = replace;
				list.add(joinw);
			}
		}
		return list;
	}

	private List<EOFInfo> getEOFList(JSONArray jArray) {
		List<EOFInfo> list = new ArrayList<>();
		if (jArray != null) {
			for (int j = 0, jlen = jArray.size(); j < jlen; j++) {

				JSONArray jEOF = (JSONArray) jArray.get(j);

				String front = jEOF.getString(0);
				String back = jEOF.getString(1);

				EOFInfo eofi = new EOFInfo();
				eofi.front = front;
				eofi.back = back;
				list.add(eofi);
			}
		}
		return list;
	}

	private void load() throws Exception {
		if (!common.IsExist(_path)) {
			return;
		}

		String sConfig = common.readFile(_path);

		if (common.IsEmpty(sConfig)) {
			return;
		}

		JSONObject json = common.getJSONFormat(sConfig);
		JSONArray languages = common.getJSONArray(json, "language");

		_config = new ConfigInfo();

		String sentenceJoinScript = common.getJSONValue(json, "script", "sentence_join");
		_config.sentenceJoinScript = sentenceJoinScript;

		for (int i = 0, len = languages.size(); i < len; i++) {
			JSONObject config = languages.getJSONObject(i);
			String lang = common.getJSONValue(config, "name");
			JSONObject langConfigs = common.getJSONObject(config, "config");

			JSONArray joinWords = common.getJSONArray(langConfigs, "join_words");
			JSONArray absoluteEOF = common.getJSONArray(langConfigs, "absolute_eof");
			JSONArray normalize = common.getJSONArray(langConfigs, "normalize");
			JSONArray repair = common.getJSONArray(langConfigs, "repair");
			String sentenceJoinModel = common.getJSONValue(langConfigs, "sentencejoin_model");

			LangInfo langInfo = new LangInfo();
			langInfo.language = lang;
			langInfo.joinWords.addAll(getJoinWordsList(joinWords));
			langInfo.absoluteEOF.addAll(getEOFList(absoluteEOF));
			langInfo.normalize.addAll(getNormalizeList(normalize));
			langInfo.repair.addAll(getNormalizeList(repair));
			langInfo.sentenceJoinModel = sentenceJoinModel;

			_config.langConfig.put(lang, langInfo);
		}
	}

	public LangInfo get(String lang) {
		if (_config != null && _config.langConfig != null) {
			LangInfo langInfo = _config.langConfig.get(lang);
			return langInfo;
		}
		return null;
	}

	public String getSentenceJoinScript() {
		if (_config != null && _config.sentenceJoinScript != null) {
			return _config.sentenceJoinScript;
		}
		return null;
	}

	public static class ConfigInfo {
		public HashMap<String, LangInfo> langConfig = new HashMap<>();
		public String sentenceJoinScript = "";

	}

	public static class LangInfo {
		public String language = "";
		public List<JoinWordInfo> joinWords = new ArrayList<>();
		public List<EOFInfo> absoluteEOF = new ArrayList<>();
		public List<NormalizeInfo> normalize = new ArrayList<>();
		public List<NormalizeInfo> repair = new ArrayList<>();
		public String sentenceJoinModel = "";
	}

	public static class NormalizeInfo {
		public String search = "";
		public String replace = "";
	}

	public static class JoinWordInfo {
		public String front = "";
		public String back = "";
		public String joinText = "";
	}

	public static class EOFInfo {
		public String front = "";
		public String back = "";
	}
}