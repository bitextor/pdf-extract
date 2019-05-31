package com.java.classes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class drawingBOX {
	
	private Pattern p = Pattern.compile("<div class=\"p\"");
    private Pattern p1 = Pattern.compile("<div class=\"page\"");
    private Pattern p2 = Pattern.compile("<div class=\"r\"");
    private Pattern p3 = Pattern.compile("<img.*");
    
    private static final String REGEX_COLOR = "(.*color:)(#[a-z]+)(;.*)$";
    private static final String REGEX_SIZE = "(.*font-size:)([0-9.]+)(pt.*)$";
    private static final String REGEX_RESIZE = "(.*font-size:)([0-9.]+)(pt.*)$";
    //model
    private static LinkedHashMap<Integer, ArrayList<String>> model = new LinkedHashMap<Integer, ArrayList<String>>();
    private static ArrayList<String> objm = new ArrayList<String>();
    //line
    private static ArrayList<Double> objline = new ArrayList<Double>();
    
    public void drawing(String inputFile, String outputFile) throws IOException {
    	
    	InputStreamReader i_in = null;
    	BufferedReader b_in = null;
    	FileOutputStream f_out = null;
    	OutputStreamWriter o_out = null;
    	BufferedWriter b_out = null;
    	
    	try {
	    	i_in = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);
	        b_in = new BufferedReader(i_in);
	        f_out = new FileOutputStream(outputFile);
	        o_out = new OutputStreamWriter(f_out, StandardCharsets.UTF_8);
	        b_out = new BufferedWriter(o_out);
	        
	        int nPage = 1;
	        int wordIterator = 1;
	        String line;
	        while ((line = b_in.readLine()) != null) {
	            Matcher m = this.p.matcher(line);
	            Matcher m1 = this.p1.matcher(line);
	            Matcher m2 = this.p2.matcher(line);
	            Matcher m3 = this.p3.matcher(line);
	            
	            if(m2.find() || m3.find()) {
	            	line = line.replaceAll(".*src.*>","");
	            	line = line.replaceAll("<div class=\"r\" style.*","");
	            }else if (m1.find()) {
	                b_out.write(line + "\n");
	                if (nPage != model.size() && model.size()!=0) {
	                	for (int x = 0; x < model.get(nPage).size(); x++) {
	                        b_out.write(model.get(nPage).get(x) + "\n");
	                    }
	                    nPage += 1;
	               }   
	            } else if (m.find()) {
	                double percent = 0.11;
	                String size = line.replaceAll(REGEX_SIZE, "$2");
	                double a = Double.parseDouble(size);
	                double calSIZE = a - (a * percent);
	                if (wordIterator == 1) {
	                    line = line.replaceAll(REGEX_RESIZE, "$1"+calSIZE+"$3");
	                    //line = line.replaceAll(REGEX_TEXT, "$1background-color:cyan;$2");
	                    wordIterator = 0;
	                }else{
	                    line = line.replaceAll(REGEX_RESIZE, "$1"+calSIZE+"$3");
	                    //line = line.replaceAll(REGEX_TEXT, "$1background-color:silver;$2"); 
	                    wordIterator = 1;
	                }
	                line = line.replaceAll(REGEX_COLOR,"$1" + "#000000" +"$3");
	                b_out.write(line + "\n");
	            } else {
	                b_out.write(line + "\n");
	            }
	        }
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
		        if (b_out != null) {
		        	b_out.close();
		        	b_out = null;
		        }
		        if (o_out != null) {
		        	o_out.close();
		        	o_out = null;
		        }
		        if (f_out != null) {
		        	f_out.close();
		        	f_out = null;
		        }
        	}catch(Exception e) {
        	}
    	}
    }

    public void process(LinkedHashMap<Integer, read2map.HtmlTagValues> newHtmlTagValues, int nPage, int nPageID[]){

    	int BOX = 1;
        double columnTop = 0; double columnLeft = 0; double columnHeight = 0; double columnWidth = 0;
        double lineTop = 0; double lineLeft = 0; double lineHeight = 0; double lineWidth = 0;
        int round = 0; int columnCount = 0;
        double nextTop = 0; double previousTop = 0; double nextLeft = 0; double avgFont = 0;
        
        for(int i = 0; i <= nPage; i++) {
        	//System.out.println(nPageID[i]);
        	for(int j = nPageID[i] ; j < nPageID[i+1]-1; j++) {
        		
        		read2map.HtmlTagValues v = newHtmlTagValues.get(i+1 + j);
        		read2map.HtmlTagValues v_next = newHtmlTagValues.get(i+1 + j + 1);
        		read2map.HtmlTagValues v_pre = newHtmlTagValues.get(i+1 + j - 1);
        		read2map.HtmlTagValues v_column = newHtmlTagValues.get(i+1 + j - columnCount);
        		
        		if(j > nPageID[i]) {
        			previousTop = Math.abs(Double.valueOf(v_pre.Top) - Double.valueOf(v.Top));            		
        		}
        		
        		if(j < nPageID[i+1]) {
        			nextTop = Math.abs(Double.valueOf(v.Top) - Double.valueOf(v_next.Top));
        			nextTop /= Double.valueOf(v.maxTop);
        			avgFont = Math.abs(Double.valueOf(v_next.FontSize) + Double.valueOf(v.FontSize))/2;
        			avgFont /= Double.valueOf(v.maxFont);
        			nextLeft = nextTop / avgFont;
                    nextTop = nextTop / avgFont;
        		}
        		
                //for line
                if(previousTop != 0 || j == nPageID[i]) {
                    lineTop = Double.valueOf(v.Top);
                    lineLeft = Double.valueOf(v.Left);
                    lineHeight = Double.valueOf(v.Height);
                }
                if (nextTop == 0) {
                    lineWidth += (Double.valueOf(v.Width) + Math.abs(Math.abs(Double.valueOf(v.Left) - Double.valueOf(v_next.Left)) - Double.valueOf(v.Width)));
                } else{
                    lineWidth += Double.valueOf(v.Width);
                    calBOX(lineTop, lineLeft, lineHeight, lineWidth, "blue");
                    objline.add(lineWidth);
                    lineWidth = 0;
                    //System.out.println("P: " + (i+1) + "\tID: "+ j + "\tnextTop: " + nextTop);
                }
                
                //if new column, all variables = first line
                if (round == 0) {
                    columnTop = lineTop; columnLeft = lineLeft; columnHeight = lineHeight; columnWidth = lineWidth;
                    round = 1;
                }
                //else compare current parawidth with previous columnwidth and increase column height
                else {
                    if (objline.size() == 0) columnWidth = lineWidth;
                    else columnWidth = Collections.max(objline);
                    columnHeight += Math.abs(Double.valueOf(v_pre.Top) - Double.valueOf(v.Top));
                }
                //if new column, all variables = first line
                if (nextTop > 0.4 || nextLeft > 0.2) { 
                	calBOX(columnTop, columnLeft, columnHeight, columnWidth, "red");
                	objline.removeAll(objline);
                	columnTop = 0; columnLeft = 0; columnWidth = 0; columnHeight = 0;
                	lineTop = 0; lineLeft = 0; lineWidth = 0; lineHeight = 0;
                	round = 0; columnCount = 0;
             	}
                else{
                	columnCount += 1;
             	}
                
                if(j == nPageID[i+1] - 2){
                    lineWidth += Double.valueOf(v_next.Width);
                    calBOX(lineTop, lineLeft, lineHeight, lineWidth, "blue");
                    columnHeight = Math.abs(Double.valueOf(v_column.Top) - Double.valueOf(v.Top)) + Double.valueOf(v.Height);
                    calBOX(columnTop, columnLeft, columnHeight, columnWidth, "red");
                    model.put(BOX, objm);
                    BOX++;
                    objm = new ArrayList<String>();
                    objline.removeAll(objline);
                    columnTop = 0; columnLeft = 0; columnWidth = 0; columnHeight = 0;
                    lineTop = 0; lineLeft = 0; lineWidth = 0; lineHeight = 0;
                    round = 0; columnCount = 0;
        		}
        	}
        }
    }
    
    private void calBOX(double TOP, double LEFT, double HEIGHT, double WIDTH, String color) {
        String DIV = "<div class=\"p\" style=\"border: 1pt solid;top:" + TOP + "pt;left:" + LEFT + "pt;height:" + HEIGHT + "pt;width:" + WIDTH + "pt;background-color:transparent;color:" + color + ";\"></div>";
        objm.add(DIV);
    }
}
