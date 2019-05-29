# PDFExtract
## Table of Contents
- [Introduction](#introduction)
  - [What is PDFExtract](#what-is-pdfextract)
  - [Approach](#approach)
- [Installation](#installation)
- [Processes and Tools](#processes-and-tools)
  - [Full Process](#full-process)
  - [Output Files](#output-files)
    - [Extracted Domain Matched Data](#extracted-domain-matched-data)
    - [Model](#model)
    - [Scores](#scores)
  - [Individual Tools](#individual-tools)
    - [TokenizeDomainSampleData.py](#tokenizedomainsampledatapy)
    - [TokenizePoolData.py](#tokenizepooldatapy)
    - [TrainDomainModel.py](#traindomainmodelpy)
    - [ScorePoolData.py](#scorepooldatapy)
    - [ExtractMatchedDomainData.py](#extractmatcheddomaindatapy)
  - [Configuration File – config.json](#configuration-file--configjson)
  - [Pool Data Folder Structure](#pool-data-folder-structure)
- [Dependencies](#dependencies)
  - [KenLM](#kenlm)
  - [Tokenizer](#tokenizer)
- [FAQ](#FAQ)


----
## Introduction
### What is PDFExtract?
PDF Extract is a PDF parser that converts and extracts PDF content into an optimized HTML format suitable for easy alignment across multiple language sources. While there are many PDF extraction and DOM conversion tools, typically other tools will extract to an HTML format or similar that is designed to be rendered for human consumption, is very heavy and bloated. The extracted format from PDF Extract is simplified and normalized so that it can be easily matched to other documents that contain the same or similar content translated in different languages. Tools such as Bitextor are able to directly process the outputs.

----
## Installation
Installation instructions are provided in [INSTALL.md](INSTALL.md)

## Using PDFExtract.jar

### Command-line PDF Extraction 
The command-line PDFExtract is contained in the PDFExtract.jar package that may be downloaded and directly executed on all the java-enabled platforms.

For extracting a PDF file to the alignment optimized HTML file type:

```1sh
java -jar PDFExtract.jar -I <input_file> -O <output_file> -B <batch_file> -L [<log_path>] -o [<options>]
```
*Arguments*
`-I <input_file>` is the path to the source PDF file process for extraction. 
'-O <output_file>` is the path to the output HTML file after extraction. 
'-B <batch_file>` is the path to the batch file for processing list of files. The input file and output file are specified on the same line delimited by a tab. Each line is delimited by a new line character.
'-L <log_path>' is the path to write the log file to. As it is common for PDF files to have issues when processing such as being password protected or other forms of restricted permissions, the log file can be written to a specifed location for additional processing. If not specified, then the log file will write to stdout.
'-o <options>` specifies control parameters. (LIST TO COME, STILL BEING REFINED)

**Example:**

The example below will process *Domain Sample Data* file found in  `/data/mysample/` and write the *Domain Matched Data* to `/data/domain/automotive/en_de/`. Matching data will only be extracted if it scores above the threshold of 0.5.

```sh
FullProcess.py -dn automotive -s en -t de -dsd /data/mysample/ -dmd /data/domain/ -est 0.5
```

### Library PDF Extraction

PDFExtract may be usef from within Java with the following import.

```java
import com.java.app.PDFExtract;

PDFExtract pdf = new PDFExtract(logpath);
// Single File
pdf.Extract(inputFile, outputFile, "options");

// Batch File
pdf.Extract(batchFile, "options");
```

----
## How It Works
PDF is a publishing format and is not designed for easy editing and manipulation. At the core of PDF is an advanced imaging model derived from the PostScript page description language. This PDF Imaging Model enables the description of text and graphics in a device-independent and resolution-independent manner. To improve performance for interactive viewing, PDF defines a more structured format than that used by most PostScript language programs. Unlike Postscript, which is a programming language, PDF is based on a structured binary file format that is optimized for high performance in interactive viewing. PDF also includes objects, such as annotations and hypertext links, that are not part of the page content itself but are useful for interactive viewing and document interchange.

Our first step is to extract the PDF content out into a DOM that can be used to further processed. PDFExtract uses the Pdf2Dom Java library as a parser for the CSSBox rendering engine in order to get a DOM designed for rendering in HTML, but not suitable for mining. PDF Extract further processes and mines the DOM into a normalized and simplified HTML format.

The core concept is very simple. Regions within the PDF are defined as a set of boxes. The lowest level of a box is a letter, then progressively expands to words, lines, paragraphs, columns and pages. Words are merged into lines. Lines are merged into paragraphs. The below examples show the original PDF file in the right, with the page marked in a blue box, columns marked in a red box, paragraphs marked in a green box and lines marked in a blue box. Once the clean boxed regions are defined, the content is merged to create clean HTML. 

![alt text](Example1.png "Example 1")

Gaps between lines are used to determine whether the next line is in the same paragraph or another. 

**NOTE:** Some issues remain in very large fonts and defining paragraphs. 

![alt text](Example2.png "Example 2")

Once the regions boxes are defined, the objects that fall within the boxes can be extracted into a normalized HTML format. 

##Normalized HTML Format.
```html
<html>
	<!--Generated Normalized Fonts-->
	<!--Standard text font for the document has no class assigned to the paragraph-->
	<!--Classes page, header, footer, column do not need a style. They are for classification only,
	    but a style can be applied for visual rendering -->
	<style>
		.h1 {
			font-family:arial;
			font-weight:bold;
			font-size:20pt;
			text-decoration:normal;
			font-style:italic;
		}
		.h2 {
			font-family:arial;
			font-weight:bold;
			font-size:18pt;
			text-decoration:underline;
			font-style:normal;
		}
	</style>
	<!--
	<style>
		body {
			font-family:arial;
			font-weight:normal;
			font-size:10pt;			
			text-decoration:normal;
			font-style:normal;
		}
		p{
			position: relative;
			border:1px solid red;
		}
		.page{ 
			border:1px dashed silver;
		}
		.header{
			position: relative;
			border:1px solid cyan;
		}
		.footer{
			position: relative;
			border:1px solid purple;
		}
		.column{
			position: relative;
			border:1px solid orange;
		}
	</style>
	-->
	<body>
		<div id="page1" class="page">
			<div id="page1h1" class="header" style="top:0px;left:0px;width:100px;height:100px;" >
				<p id="page1h1p1" class="text" style="top:0px;left:0px;width:100px;height:100px;">header text</p>
			</div>
			<div id="page1c1" class="column" style="top:0px;left:0px;width:100px;height:100px;">
				<p id="page1c1p1" class="h1" style="top:0px;left:0px;width:100px;height:100px;">heading text 1</p>
				<p id="page1c1p2" style="top:0px;left:0px;width:100px;height:100px;">paragraph 2 text</p>
				<p id="page1c1p3" style="top:0px;left:0px;width:100px;height:100px;">paragraph 3 text</p>
				<p id="page1c1p4" style="top:0px;left:0px;width:100px;height:100px;">paragraph 4 text</p>
			</div>
			<div id="page1c2" class="column" style="top:0px;left:0px;width:100px;height:100px;">
				<p id="page1c2p1" style="top:0px;left:0px;width:100px;height:100px;">paragraph 1 text</p>
				<p id="page1c2p2" class="h2" style="top:0px;left:0px;width:100px;height:100px;">heading text 2</p>
				<p id="page1c2p3" style="top:0px;left:0px;width:100px;height:100px;">paragraph 3 text</p>
				<p id="page1c2p4" style="top:0px;left:0px;width:100px;height:100px;">paragraph 4 text</p>
			</div>
			<div id="page1f1" class="footer" style="top:0px;left:0px;width:100px;height:100px;">
				<p id="page1f1p1" style="top:0px;left:0px;width:100px;height:100px;">footer text</p>
			</div>
		</div>
		<div id="page2" class="page">
			<div id="page2h1" class="header" style="top:0px;left:0px;width:100px;height:100px;" >
				<p id="page2h1p1" style="top:0px;left:0px;width:100px;height:100px;">header text</p>
			</div>
			<div id="page2c1" class="column" style="top:0px;left:0px;width:100px;height:100px;">
				<p id="page2c1p1" style="top:0px;left:0px;width:100px;height:100px;">paragraph 1 text</p>
				<p id="page2c1p2" style="top:0px;left:0px;width:100px;height:100px;">paragraph 2 text</p>
				<p id="page2c1p3" style="top:0px;left:0px;width:100px;height:100px;">paragraph 3 text</p>
				<p id="page2c1p4" style="top:0px;left:0px;width:100px;height:100px;">paragraph 4 text</p>
			</div>
			<div id="page2c2" class="column" style="top:0px;left:0px;width:100px;height:100px;">
				<p id="page2c2p1" style="top:0px;left:0px;width:100px;height:100px;">paragraph 1 text</p>
				<p id="page2c2p2" style="top:0px;left:0px;width:100px;height:100px;">paragraph 2 text</p>
				<p id="page2c2p3" style="top:0px;left:0px;width:100px;height:100px;">paragraph 3 text</p>
				<p id="page2c2p4" style="top:0px;left:0px;width:100px;height:100px;">paragraph 4 text</p>
			</div>
			<div id="page2f1" class="footer" style="top:0px;left:0px;width:100px;height:100px;">
				<p id="page2f1p1" style="top:0px;left:0px;width:100px;height:100px;">footer text 1</p>
				<p id="page2f1p2" style="top:0px;left:0px;width:100px;height:100px;">footer text 2</p>
			</div>
		</div>
	</body>
</html>
```

##

----
## FAQ
TODO
