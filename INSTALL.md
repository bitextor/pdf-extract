# PDFExtract Installation

- Download setup script to `work` directory: [setup.sh](setup.sh)

*Support for Ubuntu >= 16.04 or CentOS >= 7 or Debian >= 9*
	

- Go into `work` directory then execute setup script.

```
	cd /work
	bash setup.sh
```

*This will take around an hour to process.*

If you don't need to install dependencies and only build PDFExtract, run:

```
	cd /word
	bash setup.sh compile
```

- After setup finish, there will have `PDFExtract-2.0.jar` and `PDFExtract.json` in work folder.

```
	ls -l
	- PDFExtract-2.0.jar
	- PDFExtract.json
```

- You can now run the PDFExtract-2.0.jar via command line using the instructions in [README.md](README.md)

- PDFExtract project is now in `/work/setup-tmp/pdf-extract/PDFExtract` directory.

- And java library for PDFExtract in `/work/setup-tmp/pdf-extract/PDFExtract/target/`.


## Setup.sh

Here is the steps to install in setup script:


1. Install prerequisite programs

2. Install protobuf

3. Build cld3 java wrapper

4. Install cld3-java.jar in maven repository

5. Build PDFExtract


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

