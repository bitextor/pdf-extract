# PDFExtract
## Table of Contents
- [Introduction](#introduction)
  - [What is PDFExtract](#what-is-pdfextract)
- [Installation](#installation)
- [Using PDFExtract.jar](#using-pdfextractjar)
  - [Command-line PDF Extraction](#command-line-pdf-extraction)
  - [Library PDF Extraction](#library-pdf-extraction)
- [How It Works](#how-it-works)
- [Document Format](#document-format)
  - [Alignment Optimized HTML](#alignment-optimized-html)
  - [ID Formats](#id-formats)
  - [FontName](#fondname)
  - [Coordinates](#coordinates)
  - [Joining Lines](#joining-lines)
  - [Repairing Object Sequences](#repairing-object-sequences)
  - [Search and Replace Characters](#search-and-replace-characters)
- [Performance](#performance)
- [TODO](#todo)
- [FAQ](#faq)

----
## Introduction
### What is PDFExtract?
PDFExtract is a PDF parser that converts and extracts PDF content into a HTML format that is optimized for easy alignment across multiple language sources. The output is intended for this purpose only and not for rendering as HTML in a web browser. 

While there are many PDF extraction and HTML DOM conversion tools, none are designed to prepare data for alignment between multilingual websites for the purpose of creating parallel corpora. Typically, other tools will extract to a HTML format that is designed to be rendered for human consumption, are very heavy and bloated with information that is not needed, while missing information that would be helpful to an aligner. 

The HTML format produced by PDFExtract is simplified and normalized so that it can be easily matched to other documents that contain the same or similar content translated in different languages. Repairs to the document flow and structure are made so as to be in logical sequence as they appear in the document.  Tools such as Bitextor are able to directly process the outputs. 

PDFExtract has several components and dependancies that are used for the following purpose:
- Poppler: A generic PDF to HMTL conversion tool that performs an initial extraction of PDF data. This format is further refined by the follow on processes in the PDFExtract tool.
- Language ID: Used to determine the language of the content being processed. This is useful for external processes of the data as well as within the various data refinement steps of PDFExtract.
- Sentence Join: A tool that analyzes text based on a specified language and determines if a left and a right portion of text are 2 parts of the same sentence and should be joined as a single sentence.

----
## Installation
Installation instructions are provided in [INSTALL.md](INSTALL.md)

----
## Using PDFExtract.jar

PDFExtract can be used as a command line tool or as a library within a Java project. PDFExtract processes individual files and can also operate in batch mode to process large lists of files. Within Paracrawl, PDFExtraxt streams data via stdin and stdout.


### PDFExtract.json
PDFExtract configuration file, put it into the PDFExtract installation path beside PDFExtract.jar file.

- `script > sentence_join`  specifies the path to the sentence join tool
- `language[name=common] > config`  rules to common use for all
- `language[name=common] > config > join_word`  rules for joining words ["rule for left side", "rule for right side", "join character"]
- `language[name=common] > config > absolute_eof`  rules for identify end of sentence ["rule for left side", "rule for right side"]
- `language[name=common] > config > normalize`  rules for normalize words ["rule", "word normalize"]
- `language[name=common] > config > repair`  rules for repair words at the last step of the process
- `language[name=language] > config`  rules use for specify language
- `language[name=language] > config > sentencejoin_model`  specifies the prefix model path for sentence join tool by language
- `language[name=language] > config > join_word`  rules for joining words by language
- `language[name=language] > config > absolute_eof`  rules for identify end of sentence by language
- `language[name=language] > config > normalize`  rules for normalize words by language
- `language[name=language] > config > repair`  rules for repair words at the last step of the process of the language

```
{
"script" : {
	"sentence_join" : "/var/www/html/experiment/sentence-join/git/sentence-join.py"
}, 
"language" : 
[
	{
		"name" : "common",
		"config" : {
			"join_words" : [
				[".*[\\,\\&\\;\\:]$", "", " "],
				[".*[a-z]+\\-$", "^[a-z]+.*", ""],
				[".*[a-z]{1,}$", "^[a-z]+.*", " "],
				[".*[\\,\\;\\s][A-Z]{1,1}$", "", " "],
				[".*\\s(to|for|at|by)$", "", " "]
			],
			"absolute_eof" : [
				[".*(?<!\\,|\\&|\\;|\\:|\\s[A-Z]{1,1})$", "^[0-9 ]{0,}[A-Z]+.*"],
				[".*(\\?\\\"?|\\!\\\"?)$", ""],
				[".*\\w\\.$", ""],
				["", "^[•]+.*"]
			],
			"normalize" : [
				[ "ﬀ", "ff"],
				["ﬁ\\s?", "fi"],
				["ﬂ\\s?", "fl"],
				["ﬃ", "ffi"],
				["ﬄ", "ffl"],
				["ﬅ", "ft"],
				["ﬆ", "st"],
				["[“”]", "\""],
				["[’´]", "'"],
				["…", "..."],
				["–", "-"],
				[" ", ""],
				["יִ", "!"]
			],
			"repair" : [
				["\\s(\\,|\\)|\\]|\\;)", "$1"],
				["(\\(\\[)\\s", "$1"],
				["([^\\.])\\s(\\.)", "$1$2"]
			]
		}
	},
	{
		"name" : "en",
		"config" : {
			"sentencejoin_model" : "/var/www/html/experiment/sentence-join/toy-model",
			"join_words" : [
			],
			"absolute_eof" : [
			],
			"normalize" : [
			],
			"repair" : [
			]
		}
	}
	
]
}
```



### Command-line PDF Extraction 
The command-line PDFExtract is contained in the PDFExtract.jar package that may be downloaded and directly executed on all the java-enabled platforms.

For extracting a PDF file to the alignment optimized HTML file type:

```sh
java -jar PDFExtract.jar -I <input_file> -O <output_file> -B <batch_file> -L [<log_path>] -T [<number_threads>] --keepbrtags --getperms -C [<config_file>]
```
*Arguments*
- `-I <input_file>` specifies the path to the source PDF file process for extraction. 
- `-O <output_file>` specifies the path to the output HTML file after extraction. 
- `-B <batch_file>` specifies the path to the batch file for processing list of files. The input file and output file are specified on the same line delimited by a tab. Each line is delimited by a new line character.
- `-L <log_path>` specifies the path to write the log file to. As it is common for PDF files to have issues when processing such as being password protected or other forms of restricted permissions, the log file can be written to a specified location for additional processing. If not specified, then the log file will write to stdout.
- `-T <number_threads>` specifies the number of threads to run concurrently when processing PDF files. One file can be processed per thread. If not specified, then the default valur of 1 thread is used.
- `--keepbrtags` by default &lt;br /&gt; is not included in the output. When this argument is specified, then the output will include the &lt;br /&gt; tag after each line.
- `--getperms` by default the permissions is not included in the output. When this argument is specified, then the output will include permissions tag into header section.
- `-C <config_file>` specifies a json configuration file. If not specify, then the file `PDFExtract.json` in the same folder as the `PDFExtract.jar` file will be used.


**Example:**

This example processes a single English PDF file in English.

```sh
java -jar PDFExtract.jar -I pdf-in/sample.pdf -O html-display/sample.html
```

This example processes a batch of files as specified in `sample-display.tab` using 3 threads and writing to a user specified log file. Custom JavaScript rules for sentence joining and object sequence repairs is also specified.

```sh
java -jar PDFExtract.jar -B sample-display.tab -L batch.log -T 3 
```

The contents of `sample-display.tab` are:
```text
pdf-in/sample.pdf	html-display/sample.html
pdf-in/sample2.pdf	html-display/sample2.html
pdf-in/sample3.pdf	html-display/sample3.html
pdf-in/sample4.pdf	html-display/sample4.html
```

The format is:
```
<input_path>\t<output_path>
<input_path>\t<output_path>
<input_path>\t<output_path>
...
```

### Library PDF Extraction

PDFExtract has 2 methods overloaded methods

1. Single File
```Java
public void Extract(String inputFile, String outputFile, int keepBrTags) throws Exception
```

2. Batch File
```java
public void Extract(String batchFile, String rulePath, int threadCount, int keepBrTags) throws Exception
```

3. Stream
```java
public ByteArrayOutputStream Extract(ByteArrayInputStream inputStream, int keepBrTags) throws Exception
```

PDFExtract may be used from within Java with the following import:

```java
import com.java.app.PDFExtract;

PDFExtract pdf = new PDFExtract(logpath);

String inputFile = "/data/test.pdf";
String outputFile = "/data/test.html";
int threadCount = 5;
int keepBrTags = 0; 

// Single File
pdf.Extract(inputFile, outputFile, keepBrTags);

// Batch File
pdf.Extract(batchFile, threadCount, keepBrTags);

// Stream
ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(new File(inputFile).toPath()));
ByteArrayOutputStream outputStream = pdf.Extract(inputStream, keepBrTags);
```

----
## How It Works
Adobe Portal Document Format (PDF) is a publishing format and is not designed for easy editing and manipulation. At the core of PDF is an advanced imaging model derived from the PostScript page description language. This PDF Imaging Model enables the description of text and graphics in a device-independent and resolution-independent manner. To improve performance for interactive viewing, PDF defines a more structured format than that used by most PostScript language programs. 

Unlike PostScript, which is a programming language, PDF is based on a structured binary file format that is optimized for high performance in interactive viewing. PDF also includes objects, such as annotations and hypertext links, that are not part of the page content itself but are useful for interactive viewing and document interchange.

Our first step is to extract the PDF content out into a Document Object Model (DOM) that can be further processed into an optimal format for alignment between two similar documents. PDFExtract uses the Poppler (https://poppler.freedesktop.org/) as a parser in order to get the initial DOM. This DOM is designed for rendering in HTML with large output that may have content out of sequence in terms of when it is rendered, but sill in correctly placed on the page. For example, a footer could be embedded in the sequence at the start rather than at the bottom of the page. There may also be off-page noise from the PDF creation tool that can confuse many aligners. This content is not visible on the page but is embedded in the document. Each word (and sometimes each letter) may be a separate note in the DOM based on the original content. Poppler does a reasonable job of converting these to blocks of text, but the raw DOM output the raw DOM is not very suitable for aligning and mining of content without further processing. 

PDFExtract refines the initial DOM into a normalized and simplified HTML format that is light and fast to process.  The content is refined, repaired then formatted into a cleaner format. Each block of text within the document is then processed through language identification. Once the language of the text is defined, a set of configurable language specific rules can be run to join split sentences and then the content is analyzed further with a sentence join tool to determine if 2 consecutive blocks of text should be joined as one. 

## Document Format
### Alignment Optimized HTML
The content below is a small sample of content that provides examples of each section of the document.
```html
<html>
<head>
    <defaultLang abbr="en" />
    <languages>
        <language abbr="en" percent="99.45055" />
        <language abbr="it" percent="0.18315019" />
        <language abbr="fr" percent="0.18315019" />
        <language abbr="la" percent="0.09157509" />
        <language abbr="co" percent="0.09157509" />
    </languages>
</head>
<body>
     <div id="page1" class="page">
        <p id="page1p1" lang="en" fontname="XPRYPN+FSAlbert-Bold">
            Associated British Foods plc
            Weston Centre
            10 Grosvenor Street London W1K 4QY
            Tel + 44 (0) 20 7399 6500
            Fax + 44 (0) 20 7399 6580
            For an accessible version of the Annual Report and Accounts please visit www.abf.co.uk
        </p>
        <p id="page1p2" lang="en" fontname="XPRYPN+FSAlbert-Bold">
             ...
	</p>
    </div>
    <div id="page2" class="page">
        <p id="page2p1" lang="en" fontname="WXBSBP+FSAlbert-Light">
            • Acquisition of the leading Iberian sugar producer, Azucarera Ebro
            • Sale of Polish sugar business
            • Restructuring of US packaged oils business - new joint venture, Stratas
            • Zambian cane sugar expansion completed - capacity doubled
            • Investment in Chinese beet and cane sugar
            • Enzyme capacity investment in Finland completed
            • Yeast and yeast extracts plant under construction in Harbin
            • New Primark stores in UK and Spain and first openings in the Netherlands, Germany and Portugal
            • US Private Placement secures long-term non-bank finance
        </p>
        <p id="page2p2" lang="en" fontname="YXBEDL+FSAlbert-Light">
            This report has been printed on revive 50:50 Silk paper.
            This paper is made from pre and post consumer waste and virgin wood fibre, independently certified in accordance with the Forest Stewardship Council (FSC).
            It is manufactured at a mill that is certified to ISO 14001 environmental management standards. The pulp is bleached using an elemental chlorine free process. The inks used are all vegetable oil based.
            Printed at St Ives Westerham Press Ltd, ISO 14001, FSC certified and CarbonNeutral®
        </p>
         ....
        <p id="page2p35" lang="en" fontname="YXBEDL+FSAlbert-Light">
            This report contains forward-looking statements. These have been made by the directors in good faith based on the information available to them up to the time of their approval of this report. The directors can give no assurance that these expectations will prove to have been correct. Due to the inherent uncertainties, including both economic and business risk factors underlying such forward-looking information, actual results may differ materially from those expressed or implied by these forward-looking statements. The directors undertake no obligation to update any forward-looking statements whether as a result of new information, future events or otherwise.
        </p>
    </div>
</body>
</html>

```

### ID Formats
The ID of a element is defined by is parent structure with an incremental counter:

- page = page
- paragraph = p

**Example**

Page 1, paragraph 3 would be written as `page1p3`.

### FontName
The name of the font used for each paragraph is defined in the paragraph header. If there is a small change in font wihtin the paragraph, for example to highlight a color or if a part of the text was bold or italic, then these are removed and only the primary font is represented.

### Joining Lines
Lines are joined automatically when they are split in the orignal content. Absolute line/sentence join rules are provided int he conifguration file. Automated sentence joining is provided by the Paracrawl Sentence Join tool. (https://github.com/paracrawl/sentence-join)

### Repairing Object Sequences
Some PDF tools export malformed PDF content in some cases. For example, instead of rendering a word as a single object, a set of letters are rendered as individual objects. There are many exceptions that need to be handled. Common exceptions are handled wihtin the Java code. 

## Search and Replace Characters
Some PDF creation tools will transform characters resulting in words that are not using the correct letters (in terms of actual Unicode values), but look correct on the screen. 

For example (A) first (B) ﬁrst

Both of these look the same. But the "fi" in A is the letter "f" and "i" while the "fi" in B is the character "ﬁ" (U+FB01). 

A list of these characters can be found in the file configuration file in the same folder as the PDFExtract.jar file. Additional search and replace characters can be added as needed. This search and replace is performed when processing words and merging them into lines.

## Performance
The processing has been optimized and multithreaded. Reducing a large file can take some time. A 50MB PDF can be extracted, cleaned and stored in as little 10K, depending on the content. 

Previously for V1 and V2, the Apache PDF Box library was used to extract the intial DOM. V2 was a more optimize version using PDF Box. V3 is a complete rewrite and update to use the Poppler toolkit which is considerably faster. Poppler has some limitations and issues with paragragh font merging when they are different, but these have been worked around externally in the PDFExtract code.

1. Single file

| Name | Size (KB) | V1 | V2 | V3 |
| --- | --- | --- | --- | --- |
| sample.pdf | 2.96 | 00:00:01.630 | 00:00:00.662 | 00:00:00.394 |
| sample2.pdf | 34.72 | 00:00:01.698 | 00:00:00.909 | 00:00:00.454 |
| sample3.pdf | 597.78 | 00:00:04.034 | 00:00:01.801 | 00:00:00.717 |
| sample4.pdf | 3,462.57 | 00:01:38.810 | 00:00:32.910 | 00:00:08.949 |

2. Batch file, 10 files, 10 threads

| Name | Size (KB) | V1 | V2 | V3 |
| --- | --- | --- | --- | --- |
| sample.pdf | 2.96 | 00:00:01.570 | 00:00:00.791 | 00:00:00.445 |
| sample2.pdf | 34.72 | 00:00:02.452 | 00:00:01.377 | 00:00:00.718 |
| sample3.pdf | 597.78 | 00:00:07.693 | 00:00:02.542 | 00:00:01.124 |
| sample4.pdf | 3,462.57 | 00:03:35.353 | 00:00:36.036 | 00:00:12.594 |


## TODO
The below list is a set of features planned for future:
- Right-to-Left languages.
  - This code is untested on right-to-left languages and may need to be modified to support languages such as Arabic.
- Vertical Script
- This code is untested on right-to-left languages and may need to be modified to support languages such as Japanese when written down the page..


----
## FAQ
### Can the HTML output be loaded into the browser to view?
Yes. By default the HTML output is not in a format that will render well in a browser as it is formatted for optimal processing and hot intended to be viewed by humans. Use the `--keepbrtags` option to output the HTML in a more visual format.

### Can this tool extract text from images embedded in PDF files?
No. This tool processes only text. It is not an OCR tool, it is only able to extract text from PDF if the data is already in text format.


