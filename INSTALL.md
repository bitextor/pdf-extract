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
   3. Next to “Select root directory,” browse to the cloned repository on your computer where you downloaded all the files from Github. Then select Open and browse to `/gitproject/pdf-extract/PDFExtract`.

# Requirements

1. poppler -- https://poppler.freedesktop.org/

2. cld3 -- https://github.com/google/cld3


# Dependencies
All dependencies are included in the project folder.


| Library | URL| Description |
| --- | --- | --- |
| cld3-java.jar | https://github.com/xondre09/cld3-Java | Java wrapper for cld3 |
| json-lib-2.4-jdk15.jar |	https://repo1.maven.org/maven2/net/sf/json-lib/json-lib/2.4/ | Read json |
| commons-beanutils-1.8.3.jar | https://commons.apache.org/proper/commons-beanutils/download_beanutils.cgi | Dependency of json |
| commons-collections.jar | https://commons.apache.org/proper/commons-collections/download_collections.cgi | Dependency of json |
| commons-lang.jar | https://mvnrepository.com/artifact/commons-lang/commons-lang | Dependency of json |
| commons-logging-1.2.jar |	https://commons.apache.org/logging/download_logging.cgi | Dependency of json |
| ezmorph.jar |	https://mvnrepository.com/artifact/net.sf.ezmorph/ezmorph/1.0.6 | Dependency of json |
| commons-io-2.6.jar | https://commons.apache.org/proper/commons-io/download_io.cgi | Read / write file |
| guava-15.0.jar |	https://mvnrepository.com/artifact/com.google.guava/guava/15.0 | Manage collections |
| pdfbox-2.0.17.jar |	https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox/2.0.17 | Manage pdf file |
