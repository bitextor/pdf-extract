package pdfextract;

import java.util.ArrayList;
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
		public String style = "";
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
		public float mostHeight = 0;
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
	}

	public static class LangObject {
		public String name = "";
		public float percent = 0;
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
		public String method = "";
		public String detail = "";
	}
}