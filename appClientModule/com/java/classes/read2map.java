package com.java.classes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author nat
 */

public class read2map 
{
    
    private Pattern p = Pattern.compile("<div class=\"p\"");
    private Pattern p1 = Pattern.compile("<div class=\"page\"");
    
    private static final String REGEX_T = ".*top:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_L = ".*left:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_H = ".*height:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_W = ".*width:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_FS = ".*font-size:([\\-\\+0-9]+.[0-9]+).*";
    private static final String REGEX_FF = ".*font-family:([a-zA-Z\\s\\-]+).*";
    private static final String REGEX_WORD = ".*>(.*?)<.*";
    
    public void process(String inputFile, String outputFile) throws IOException
    {
        LinkedHashMap<Integer, HtmlTagValues> hHtmlTagValues = new LinkedHashMap<Integer, HtmlTagValues>();
    	
        InputStreamReader i_in = null;
        BufferedReader b_in = null;
        
        try {
	        i_in = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);
	        b_in = new BufferedReader(i_in);
	        
	        int nPage = 0;
	        int oldElementID = 0; int newElementID = 0;
	        int nPageID[] = new int[99999];
	        double pageWidth = 0; double pageHeight = 0;
	        String line = "";
	        
	        while ((line = b_in.readLine()) != null) 
	        {	
	        	double maxFont = 0; double maxTop = 0; double maxLeft = 0;
	            Matcher m = this.p.matcher(line);
	            Matcher m1 = this.p1.matcher(line);
	            
	            if (m.find()) {
	             
	            	HtmlTagValues v = new HtmlTagValues();
	    
	                v.Top = line.replaceAll(REGEX_T, "$1");  
	                v.Left = line.replaceAll(REGEX_L, "$1");  
	                v.Height = line.replaceAll(REGEX_H, "$1");  
	                v.Width = line.replaceAll(REGEX_W, "$1");  
	                v.FontFamily = line.replaceAll(REGEX_FF, "$1");
	                v.FontSize = line.replaceAll(REGEX_FS, "$1");
	                v.Word = line.replaceAll(REGEX_WORD, "$1");  
	                if(maxTop < Double.valueOf(v.Top)) v.maxTop = v.Top;
	                if(maxFont < Double.valueOf(v.FontSize)) v.maxFont = v.FontSize;
	                if(maxLeft < Double.valueOf(v.Left)) v.maxLeft = v.Left;
	                
	                if(Double.valueOf(v.Left) < 0 || Double.valueOf(v.Top) < 0 || Double.valueOf(v.Left) > pageWidth || Double.valueOf(v.Top) > pageHeight){
	                	line = line.replaceAll("<div class=\"p\"", "");
	                	continue;
	                }
	                
	                hHtmlTagValues.put(nPage + newElementID, v);                                
	//                String fileContent = "P: " + nPage + "\tID: "+ newElementID + "\tT: " + v.Top + "\tL: " + v.Left + "\tH: " + v.Height + "\tW: " + v.Width + "\tFF: " + v.FontFamily + "\tFS: " + v.FontSize + "\tWO: " + v.Word;
	//                writer.println(fileContent);
	                newElementID++;
	            }
	            else if (m1.find())
	            {
	            	nPageID[nPage] = newElementID;
	            	nPage++;
	            	pageWidth = Double.valueOf(line.replaceAll(REGEX_W, "$1"));
	            	pageHeight = Double.valueOf(line.replaceAll(REGEX_H, "$1"));
	//            	String fileContent = "############# Page:\t" + nPage + "\t#############";
	//            	writer.println(fileContent);            	
	            }
	        }
	        nPageID[nPage] = newElementID - oldElementID;    	
	//        writer.close();

	        drawingBOX dB = new drawingBOX();
	        dB.process(hHtmlTagValues, nPage, nPageID);
	        dB.drawing(inputFile, outputFile);

        }finally {
        	try {
	        	if (b_in != null) {
	            	b_in.close();
	            	b_in = null;
	        	}
	        	if (i_in != null) {
	        		i_in.close();
	        		i_in = null;
	        	}
        	}catch(Exception e) {
        	}
        }
    }
    
    public class HtmlTagValues
    {
        public String Top;
        public String Height;
        public String Width;
        public String Left;
        public String FontFamily;
        public String FontSize;
        public String Word;
        public int nPage;
        public String id;
        public String maxFont;
        public String maxTop;
        public String maxLeft;
    }
}