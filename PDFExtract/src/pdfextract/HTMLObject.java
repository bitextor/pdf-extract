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

	public enum BoxColor {
		LINE("blue"), PARAGRAPH("green"), COLUMN("red");

		private String color;

		BoxColor(String color) {
			this.color = color;
		}

		public String getColor() {
			return color;
		}
	}

	public static class TextObject extends BoxObject {
		public String text = "";
		public String style = "";
		public float fontsize;
		public String fontfamily;
		public String fontweight;
		public String fontstyle;
		public float wordspacing;

	}

	public static class LineObject extends BoxObject {
		public List<TextObject> texts = new ArrayList<TextObject>();

		public LineObject() {
		}

		public LineObject(float top, float left, float height, float width) {
			this.top = top;
			this.left = left;
			this.height = height;
			this.width = width;

			this.bottom = this.top + this.height;
			this.right = this.left + this.width;
		}
	}

	public static class ParagraphObject extends BoxObject {
		public List<LineObject> lines = new ArrayList<LineObject>();

		public ParagraphObject() {
		}

		public ParagraphObject(float top, float left, float height, float width) {
			this.top = top;
			this.left = left;
			this.height = height;
			this.width = width;

			this.bottom = this.top + this.height;
			this.right = this.left + this.width;
		}
	}

	public static class ColumnObject extends BoxObject {
		public List<ParagraphObject> paragraphs = new ArrayList<ParagraphObject>();

		public ColumnObject() {
		}

		public ColumnObject(float top, float left, float height, float width) {
			this.top = top;
			this.left = left;
			this.height = height;
			this.width = width;

			this.bottom = this.top + this.height;
			this.right = this.left + this.width;
		}
	}

	public static class PageObject {
		public int pageno;
		public float width;
		public float height;
		public List<TextObject> texts = new ArrayList<TextObject>();
		public List<ColumnObject> columns = new ArrayList<ColumnObject>();
	}
}
