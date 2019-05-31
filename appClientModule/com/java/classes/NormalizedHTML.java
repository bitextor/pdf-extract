package com.java.classes;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.java.classes.HTMLObject.BolderObject;


public class NormalizedHTML {
	
	//global parameters
	private String _language = "";
	private Invocable _jsEngine = null;
	private Document _docTemplate = null;
	private Object _lockerExtract = new Object();
	
	public NormalizedHTML(String sLanguage, Invocable jsEngine) {
		_language = sLanguage;
		_jsEngine = jsEngine;
	}
	public void Process(String sInputPath, String sOutputPath) throws Exception
	{
		FileInputStream fs = null;	
		BufferedReader br = null;
		try {

			fs = new FileInputStream(sInputPath);
			br = new BufferedReader(new InputStreamReader(fs, "UTF-8"));
			StringBuilder sbPageAll = new StringBuilder();
			StringBuilder sbContentPage = new StringBuilder();
			String sLine = "";
			String sStartPage = "<div class=\"page\" id=\"page_";
			String sEndBody = "</body>";
			boolean bFoundFirstPage = false;
			int iChunkCount = 0;
			int iPageID = 0;
			int iChunk = 1;
			
			while ((sLine = br.readLine()) != null) {				
				if (sLine.indexOf(sStartPage) == -1 && !bFoundFirstPage) continue; 
				else { bFoundFirstPage = true;}
				
				sbContentPage.append(sLine + "\n");
				
				if (sLine.indexOf(sStartPage) != -1 && iChunkCount < iChunk)
					iChunkCount++;		
				else if (sLine.indexOf(sStartPage) != -1 && iChunkCount >= iChunk)
				{
					iPageID++;
					String sPageContent = sbContentPage.toString().substring(0,sbContentPage.toString().lastIndexOf(sStartPage));
					sbPageAll.append(GetPageNormalizedHtml(sPageContent,iPageID));		
					sbContentPage = new StringBuilder(sbContentPage.toString().substring(sbContentPage.toString().lastIndexOf(sStartPage)));
				    iChunkCount = 0;					
				}
				else if (sLine.indexOf(sEndBody) != -1)
				{
					iPageID++;
					String sPageContent = sbContentPage.toString().substring(0,sbContentPage.toString().lastIndexOf(sEndBody));
					sbPageAll.append(GetPageNormalizedHtml(sPageContent,iPageID));	
					break;
				}
			}
			

		    FileUtils.writeStringToFile(new File(sOutputPath),
		    		GetTemplate(Tempate.body.toString()).replace("[" + TempateContent.BODY.toString() + "]" , 
		    				sbPageAll.toString().replaceAll("\\n+", "\n")),
		    				"utf-8");
			
		} catch (Exception e1) {
			
			throw e1;
		}
		finally 
		{
			try {
				if (br != null) { br.close(); br = null; }
				if (fs != null) { fs.close(); fs = null; }
			} catch (Exception e2) {
				//throw e2;
			}
		}
	}
	private String  GetPageNormalizedHtml (String sContent,int iPageID)
	{
		String sPageNormalized = "";
		
		//Initial objects
		Hashtable<Float,HTMLObject.BolderObject> hashColumn = new  Hashtable<Float,HTMLObject.BolderObject>();
		Hashtable<Float,HTMLObject.BolderObject> hashParagraph = new  Hashtable<Float,HTMLObject.BolderObject>();
		Hashtable<Float,HTMLObject.BolderObject> hashLine = new  Hashtable<Float,HTMLObject.BolderObject>();
		Hashtable<Float,Hashtable<Integer,HTMLObject.TextObject>> hashText = new  Hashtable<Float,Hashtable<Integer,HTMLObject.TextObject>>();
		
		// Get Border
		Pattern pBorder = Pattern.compile("<div[ ]class=\"p\"[ ]style=\"border:[ ]1pt[ ]solid;top:(.*?)pt;left:(.*?)pt;height:(.*?)pt;width:(.*?)pt[^<>]+(red|green|blue);\"></div>", Pattern.MULTILINE);
		Matcher mBorder = pBorder.matcher(sContent);
		while (mBorder.find()) {
			HTMLObject.BolderObject oBolder = new HTMLObject.BolderObject();
			oBolder.top = (float) (Math.round(Float.parseFloat(mBorder.group(1)) * 100.0) / 100.0);
			oBolder.left = (float) (Math.round(Float.parseFloat(mBorder.group(2)) * 100.0) / 100.0);
			oBolder.height = Float.parseFloat(mBorder.group(3));
			oBolder.width = Float.parseFloat(mBorder.group(4));
			oBolder.right = oBolder.left + oBolder.width;
			oBolder.bottom = oBolder.top + oBolder.height;
			String sColor = mBorder.group(5);
			oBolder.color = sColor;
			if (sColor.equals("red"))
				hashColumn.put(oBolder.left, oBolder);
			else if (sColor.equals("green"))
				hashParagraph.put(oBolder.top, oBolder);
			else if (sColor.equals("blue"))
				hashLine.put(oBolder.top, oBolder);
		}	
		
		//Case not handle paragrapBorder 
		if (hashParagraph.size() == 0)
		{
			hashParagraph = (Hashtable<Float, BolderObject>) hashColumn.clone();
		}
		
		// Get Text
		Pattern pText = Pattern.compile("<div[ ]class=\"p\"[ ]id=\".*?\"[ ]style=\"top:(.*?)pt;left:(.*?)pt;line-height:(.*?)pt;[^<>]+width:(.*?)pt;\">(.*?)</div>", Pattern.MULTILINE);
		Matcher mText = pText.matcher(sContent);
		int iSeq = 0;
		while (mText.find()) {
			iSeq++;
			HTMLObject.TextObject oText = new HTMLObject.TextObject();
			oText.top = (float) (Math.round(Float.parseFloat(mText.group(1)) * 100.0) / 100.0);
			oText.left = (float) (Math.round(Float.parseFloat(mText.group(2)) * 100.0) / 100.0);
			oText.lineheight = Float.parseFloat(mText.group(3));
			oText.width = Float.parseFloat(mText.group(4));
			oText.right = oText.left + oText.width;
			oText.bottom = oText.top + oText.lineheight;
			oText.text = mText.group(5);
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
		List<Float> listColumnLeft = new ArrayList<Float>(hashColumn.keySet());
	    Collections.sort(listColumnLeft);
	    
	    String sColumnAll = "";
	    int iColumnID = 1; 	    
	    
	    //Loop each column
	    for (Float columnLeft : listColumnLeft) {

		    String sColumnParagraphAll = "";
		    int iParagraphID = 1;
	    	
	    	HTMLObject.BolderObject oBolderColumn = hashColumn.get(columnLeft);
	    	
	    	List<Float> listParagraphTop = new ArrayList<Float>(hashParagraph.keySet());
		    Collections.sort(listParagraphTop);
		    
		  //Loop each paragraph in column
		    for (Float paragraphTop : listParagraphTop) {
		    	String sColumnParagraphLineAll = "";
		    	HTMLObject.BolderObject oBolderParagraph = hashParagraph.get(paragraphTop);
		    	if (oBolderParagraph.top >= oBolderColumn.top && oBolderParagraph.bottom <= oBolderColumn.bottom
		    			&& oBolderParagraph.left >= oBolderColumn.left && oBolderParagraph.right <= oBolderColumn.right)
		    	{
			    	List<Float> listLineTop = new ArrayList<Float>(hashLine.keySet());
				    Collections.sort(listLineTop);
				    int iLineID = 1;

				  //Loop each line in paragraph
				    for (Float lineTop : listLineTop) {

					    
				    	HTMLObject.BolderObject oBolderLine = hashLine.get(lineTop);
				    	if (oBolderLine.top >= oBolderParagraph.top && oBolderLine.bottom <= oBolderParagraph.bottom
				    			&& oBolderLine.left >= oBolderParagraph.left && oBolderLine.right <= oBolderParagraph.right)
				    	{
				    	
					    	List<Float> listText = new ArrayList<Float>(hashText.keySet());
						    Collections.sort(listText);

						    String sLine = "";
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
								    		sLine += oTextMemberObject.text + " ";
								    		
								    	}
								    }
						    	}
						    }					    
						    
						    String sColumnParagraphLine = GetTemplate(Tempate.columnparagraphline.toString())
						    		.replace("[" + TempateID.COLUMNPARAGRAPHLINEID.toString() + "]"  , String.valueOf(iPageID) + "c" + String.valueOf(iColumnID) + "p" + String.valueOf(iParagraphID) + "l" + String.valueOf(iLineID))
						    		.replace("[" + TempateStyle.TOP.toString() + "]" , String.valueOf(oBolderLine.top))
						    		.replace("[" + TempateStyle.LEFT.toString() + "]" , String.valueOf(oBolderLine.left))
						    		.replace("[" + TempateStyle.HEIGHT.toString() + "]" , String.valueOf(oBolderLine.height))
						    		.replace("[" + TempateStyle.WIDTH.toString() + "]" , String.valueOf(oBolderLine.width))
						    		.replace("[" + TempateContent.COLUMNPARAGRAPHLINE.toString() + "]" , sLine);
						    sColumnParagraphLineAll += sColumnParagraphLine;
						    
					    	iLineID++;
				    	}
				    }

				   
				    /*
				    //Analyze Joins 
				    if (sPrevLine.length() > 0) {
		    			String sResult = AnalyzeJoins("analyzeJoins", arLines , _language);
		            }	
		            */
				    
				    String sColumnParagraph = GetTemplate(Tempate.columnparagraph.toString())
				    		.replace("[" + TempateID.COLUMNPARAGRAPHID.toString() + "]" , String.valueOf(iPageID) + "c" + String.valueOf(iColumnID) + "p" + String.valueOf(iParagraphID))
				    		.replace("[" + TempateStyle.TOP.toString() + "]" , String.valueOf(oBolderParagraph.top))
				    		.replace("[" + TempateStyle.LEFT.toString() + "]" , String.valueOf(oBolderParagraph.left))
				    		.replace("[" + TempateStyle.HEIGHT.toString() + "]" , String.valueOf(oBolderParagraph.height))
				    		.replace("[" + TempateStyle.WIDTH.toString() + "]" , String.valueOf(oBolderParagraph.width))
				    		.replace("[" + TempateContent.COLUMNPARAGRAPH.toString() + "]" , sColumnParagraphLineAll)
				    		.replace("[" + TempateContent.JOINSCORE.toString() + "]" , ""); //for now
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
	private Object AnalyzeJoins(String function, Object... args)
	{
		Object result = null;
		try {

            if (_jsEngine != null) {
    			result = _jsEngine.invokeFunction(function, args);
            }

		} catch (Exception e) {	
			result = null;
		}
		return result;
	}
	private String GetTemplate(String sTagName)
	{
		String sContent = "";

		if (_docTemplate == null) {
			synchronized (_lockerExtract) {
				if (_docTemplate == null) {
					//Read file template
					//File fTemplate = new File(getClass().getClassLoader().getResource("template.xml").getFile());
					InputStream fTemplate = getClass().getClassLoader().getResourceAsStream("template.xml");
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			        DocumentBuilder dBuilder;
			        try {
			            dBuilder = dbFactory.newDocumentBuilder();
			            _docTemplate = dBuilder.parse(fTemplate);
			            _docTemplate.getDocumentElement().normalize();
			        } catch (SAXException | ParserConfigurationException | IOException e1) {
			            e1.printStackTrace();
			        }
				}
			}
		}
		
		if (_docTemplate != null) {
	        Node node = _docTemplate.getElementsByTagName(sTagName).item(0);
	        sContent = node.getTextContent();
		}
		
		return sContent;
	}

	//Output template
	enum Tempate {
		body , 
		page,
		header,
		headerparagraph,headerparagraphline,
		column,
		columnparagraph,columnparagraphline,
		columnh1,columnh1line,
		columnh2,columnh2line,
		footer,
		footerparagraph,footerparagraphline
		}
	
	enum TempateContent { 
		BODY, 
		PAGE,
		HEADER,
		HEADERPARAGRAPH,HEADERPARAGRAPHLINE,
		COLUMN,
		COLUMNPARAGRAPH,COLUMNPARAGRAPHLINE,
		COLUMNH1,COLUMNH1LINE,
		COLUMNH2,COLUMNH2LINE,
		FOOTER,
		FOOTERPARAGRAPH,FOOTERPARAGRAPHLINE,
		JOINSCORE
		}
	
	enum TempateID { PAGEID,
		HEADERID,HEADERPARAGRAPHID,HEADERPARAGRAPHLINEID,
		COLUMNID,COLUMNPARAGRAPHID,COLUMNPARAGRAPHLINEID,
		COLUMNH1PARAGRAPHID,COLUMNH1PARAGRAPHLINEID,
		COLUMNH2PARAGRAPHID,COLUMNH2PARAGRAPHLINEID,
		FOOTERID,FOOTERPARAGRAPHID,FOOTERPARAGRAPHLINEID}
	enum TempateStyle { TOP,LEFT,WIDTH,HEIGHT}
}
