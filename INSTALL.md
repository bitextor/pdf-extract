# PDFExtract Installation

1. Download / Close the project from Github
   1. Go into directory that you want to clone the repository to and execute the `git clone` command.
```sh
cd /gitproject
git clone https://github.com/bitextor/pdf-extract.git
```
   2. A project folder will be created `/gitproject/pdf-extract` with the project files
   3. You can now run the binary in the `/gitproject/pdf-extract/runnable-jar` using the instructions in [README.md](#command-line-pdf-extraction)

2. Developers
   1. Import the projects into Eclipse (for developer)
   2. In Eclipse, go to File > Import > General and select Existing Projects into Eclipse.
   3. Next to “Select root directory,” browse to the cloned repository on your computer where you downloaded all the files from Github. Then select Open and browse to `/gitproject/pdfextract/PDFExtract`.

# Dependancies
All dependancies are included in the project folder.


| Library | URL| Description |
| --- | --- | --- |
| commons-io-2.4.jar | https://commons.apache.org/proper/commons-io/download_io.cgi | Read / write file |
| pdfbox-2.0.0.jar | https://pdfbox.apache.org/download.cgi | Convert PDF to HTML |
| pdf2dom-1.7.jar	| https://mvnrepository.com/artifact/net.sf.cssbox/pdf2dom/1.7 | Convert PDF to HTML |
|  fontbox-2.0.0.jar |	https://pdfbox.apache.org/download.cgi | Dependency of pdfbox |
| FontVerter-1.2.19.jar |	https://mvnrepository.com/artifact/net.mabboud.fontverter/FontVerter/1.2.19 | Dependency of pdfbox |
| commons-lang3-3.4.jar |	https://commons.apache.org/lang/download_lang.cgi | Dependency of pdfbox |
| commons-logging-1.2.jar |	https://commons.apache.org/logging/download_logging.cgi | Dependency of pdfbox |
| guava-15.0.jar |	https://mvnrepository.com/artifact/com.google.guava/guava/15.0 | Dependency of pdfbox |
| javassist-3.18.2-GA.jar |	https://mvnrepository.com/artifact/org.javassist/javassist/3.18.2-GA | Dependency of pdfbox |
| ch.qos.logback.classic-0.9.28.jar |	https://logback.qos.ch/download.html | Dependency of pdfbox |
| ch.qos.logback.core-0.9.28.jar | https://logback.qos.ch/download.html | Dependency of pdfbox |
| slf4j-api-1.6.1.jar |	https://www.slf4j.org/download.html | Dependency of pdfbox |
| reflections-0.9.9.jar |	https://mvnrepository.com/artifact/org.reflections/reflections/0.9.9-RC1 | Dependency of pdfbox |
| bcprov-jdk16-1.45.jar |	https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk16/1.45 | Dependency of pdfbox |
