# PDFExtract Installation

- Download setup script to `work` directory: [setup.sh](setup.sh)

*Support for Ubuntu >= 16.04 or CentOS >= 7 or Debian >= 9*
	

- Go into `work` directory then execute setup script.

```
apt-get update
apt-get install build-essential git ant maven protobuf-compiler libprotobuf-dev cmake poppler-utils openjdk-8-jdk

git clone https://github.com/bitextor/pdf-extract.git --recursive

cd pdf-extract
cd cld3-Java
ant jar
mvn install:install-file -Dfile=cld3-java.jar -DgroupId=cld3-java -DartifactId=cld3-java -Dversion=1.0 -Dpackaging=jar
cd ..
mvn package
```

- After setup finish, there will have `PDFExtract-2.0.jar` and `PDFExtract.json` in `target` folder.

```
	ls -l target/
	- PDFExtract-2.0.jar
	- PDFExtract.json
```

- You can now run the PDFExtract-2.0.jar via command line using the instructions in [README.md](README.md)

- PDFExtract project is now in `/work/setup-tmp/pdf-extract/PDFExtract` directory.

- And java library for PDFExtract in `/work/setup-tmp/pdf-extract/PDFExtract/target/`.


## Protobuf installation issues
If using your distribution packages for `libprotobuf` is not enough to compile `cld3`, please, install it manually (you can follow `bitextor` instructions: https://github.com/bitextor/bitextor#language-detector)



# Sentence-join installation

- Prequisite - KenLM must be installed
- Set path to KenLM in [pdf-extract config file](PDFExtract.json) in secgion `kenlm_path`

```
	"sentence_join" : "/home/user/sentence-join/sentence-join.py"
	"kenlm_path" : "/home/user/kenlm/bin"
```

- Download the models for the language pairs that you want to process from `http://data.statmt.org/paracrawl/sentence-join/`
- Set path with prefix for models (expected extensions forward.binlm and backward.binlm) in [pdf-extract config file](PDFExtract.json) in section `sentencejoin_model` for each language

```
	"sentencejoin_model" : "/home/user/models/en/opus",
```


# Dependencies
All dependencies are included in the project folder.


| Library | URL| Description |
| --- | --- | --- |
| cld3-java.jar | https://github.com/xondre09/cld3-Java | Java wrapper for cld3 |
| json-lib-2.4-jdk15.jar |	https://mvnrepository.com/artifact/net.sf.json-lib/json-lib/2.4 | Read json |
| commons-beanutils-1.8.3.jar | https://mvnrepository.com/artifact/commons-beanutils/commons-beanutils/1.8.3 | Dependency of json |
| commons-collections.jar | https://mvnrepository.com/artifact/commons-collections/commons-collections/3.2 | Dependency of json |
| commons-lang.jar | https://mvnrepository.com/artifact/commons-lang/commons-lang/2.1 | Dependency of json |
| commons-logging-1.2.jar |	https://mvnrepository.com/artifact/commons-logging/commons-logging/1.2 | Dependency of json |
| ezmorph.jar |	https://mvnrepository.com/artifact/net.sf.ezmorph/ezmorph/1.0.6 | Dependency of json |
| commons-io-2.6.jar | https://mvnrepository.com/artifact/commons-io/commons-io/2.6 | Read / write file |
| guava-15.0.jar |	https://mvnrepository.com/artifact/com.google.guava/guava/15.0 | Manage collections |
| pdfbox-2.0.17.jar |	https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox/2.0.17 | Manage pdf file |


