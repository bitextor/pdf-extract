package pdfextract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author MickeyVI
 */
public class HTMLObject {

	public static class BoxObject {
		public String color = "";
		public float top;
		public float bottom;
		public float left;
		public float right;
		public float height;
		public float width;
		public String html;
	}

	public static class TextObject extends BoxObject {
		public String text = "";
		public float fontsize = 0;
		public String fontfamily = "";
		public String fontweight = "";
		public String fontstyle = "";
		public boolean islink = false;
		public float wordspacing = 0;
		public String class_ = "";
		public boolean deleted = false;
		public String lang = "";
	}

	public static class PageObject {
		public int pageno = 0;
		public float width = 0;
		public float height = 0;
		public List<TextObject> texts = new ArrayList<TextObject>();
		public String language = "";
	}

	public static class DocumentObject {
		public List<PageObject> pages = new ArrayList<>();
		public float width = 0;
		public float height = 0;
		public String language = "";
		public List<LangObject> langList = new ArrayList<>();
		public List<WarnObject> warningList = new ArrayList<>();
		public AccessPermission permission = new AccessPermission();
		public HashMap<String, StyleObject> style = new HashMap<String, StyleObject>();
	}

	public static class LangObject {
		public String name = "";
		public float percent = 0;
		public int count = 0;
	}

	public static class StyleObject {
		public String id = "";
		public float size = 0;
		public String family = "";
		public String color = "";
	}

	public static class WarnObject {
		public WarnObject(String method_, String detail_) {
			method = method_;
			detail = detail_;
			
		}
		
		public WarnObject(String method_, String detail_, String solution_) {
			method = method_;
			solution = solution_;
			detail = detail_;
			
		}

		public String method = "";
		public String solution = "";
		public String detail = "";
	}

	public static class AccessPermission {
		public boolean isEncrytped = false;
		public boolean canAssembly = false;
		public boolean canCopy = false;
		public boolean canPrint = false;
		public boolean canPrintDegraded = false;
		public boolean canModified = false;
		public boolean canModifyAnnotations = false;
		public boolean canFillInForm = false;
		public boolean canScreenReader = false;
		public String verbose = "";
	}
}