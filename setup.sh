#!/bin/bash

START=$(date +%s)

if [ -f /etc/os-release ]; then
    # freedesktop.org and systemd
    . /etc/os-release
    OS=$NAME
    VER=$VERSION_ID
elif type lsb_release >/dev/null 2>&1; then
    # linuxbase.org
    OS=$(lsb_release -si)
    VER=$(lsb_release -sr)
elif [ -f /etc/lsb-release ]; then
    # For some versions of Debian/Ubuntu without lsb_release command
    . /etc/lsb-release
    OS=$DISTRIB_ID
    VER=$DISTRIB_RELEASE
elif [ -f /etc/debian_version ]; then
    # Older Debian/Ubuntu/etc.
    OS=Debian
    VER=$(cat /etc/debian_version)
elif [ -f /etc/SuSe-release ]; then
    # Older SuSE/etc.
    OS=""
    VER=""
elif [ -f /etc/redhat-release ]; then
    # Older Red Hat, CentOS, etc.
    OS=""
    VER=""
else
    # Fall back to uname, e.g. "Linux <version>", also works for BSD, etc.
    OS=""
    VER=""
fi

if [ "$OS" == "" ] ; then

	echo "The operating system is not supported by this script."

elif [[ "$OS" == *"Debian"* && $VER+0 < 9 ]] ; then
	
	echo "The operating system is not supported by this script."
	echo "Debian 9 or higher is required."

elif [[ "$OS" == *"Ubuntu"* && $VER+0 < 16 ]] ; then

	echo "The operating system is not supported by this script."
	echo "Ubuntu 16 or higher is required."

elif [[ "$OS" == *"CentOS"* && $VER+0 < 7 ]] ; then

	echo "The operating system is not supported by this script."
	echo "Ubuntu 16 or higher is required."
else

	echo "###"
	echo "### $OS - $VER ###"
	echo "###"

	setupTmpDir="setup-tmp"
	[ ! -d "$setupTmpDir" ] && mkdir "$setupTmpDir"
	cd $setupTmpDir

	echo "###"
	echo "### Install prerequisite programs ###"
	echo "###"
	if [[ "$OS" == *"Ubuntu"* || "$OS" == *"Debian"* ]] ; then
		apt-get update
		apt-get install autoconf automake libtool curl make g++ unzip git ant maven poppler-utils wget -y

		if [[ "$OS" == *"Ubuntu"* && $VER+0 < 18 ]] ; then
			apt-get install apt-transport-https ca-certificates gnupg software-properties-common -y
			wget -qO - https://apt.kitware.com/keys/kitware-archive-latest.asc | apt-key add -
			apt-add-repository 'deb https://apt.kitware.com/ubuntu/ xenial main'
			apt-get update
		fi
		apt-get install cmake -y

		if [[ "$OS" == *"Debian"* && $VER+0 != 9 ]] ; then
			apt install apt-transport-https ca-certificates wget dirmngr gnupg software-properties-common -y
			wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | apt-key add -
			add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/
			apt update
			apt install adoptopenjdk-8-hotspot -y
			export JAVA_HOME=/usr/lib/jvm/adoptopenjdk-8-hotspot-amd64
		else
			if [[ "$OS" == *"Ubuntu"* && $VER+0 < 16 ]] ; then
				yes "" | add-apt-repository ppa:openjdk-r/ppa
				apt-get update
			fi
			apt-get install openjdk-8-jdk -y
			export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
			if [[ "$OS" == *"Ubuntu"* && $VER+0 < 16 ]] ; then
				update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
			fi
		fi
	elif [[ "$OS" == *"CentOS"* || "$OS" == *"Amazon Linux"* || "$OS" == *"Fedora"* ]] ; then
		yum install epel-release -y
		yum install autoconf automake libtool unzip gcc-c++ git ant cmake cmake3 maven poppler-utils -y
		yum group install "Development Tools" -y

		alternatives --install /usr/local/bin/cmake cmake /usr/bin/cmake 10 \
		--slave /usr/local/bin/ctest ctest /usr/bin/ctest \
		--slave /usr/local/bin/cpack cpack /usr/bin/cpack \
		--slave /usr/local/bin/ccmake ccmake /usr/bin/ccmake \
		--family cmake

		alternatives --install /usr/local/bin/cmake cmake /usr/bin/cmake3 20 \
		--slave /usr/local/bin/ctest ctest /usr/bin/ctest3 \
		--slave /usr/local/bin/cpack cpack /usr/bin/cpack3 \
		--slave /usr/local/bin/ccmake ccmake /usr/bin/ccmake3 \
		--family cmake
	fi

	echo "###"
	echo "### Install protobuf ###"
	echo "###"
	if [ -d "protobuf" ]; then rm -Rf "protobuf"; fi
	git clone https://github.com/google/protobuf.git
	cd protobuf
	./autogen.sh
	./configure
	make
	make install
	ldconfig
	cd ..

	echo "###"
	echo "### Build cld3 java wrapper ###"
	echo "###"
	if [ -d "cld3-Java" ]; then rm -Rf "cld3-Java"; fi
	git clone https://github.com/xondre09/cld3-Java.git
	git clone https://github.com/google/cld3.git
	cp -rp cld3/src/ cld3-Java/src/main/cpp/cld3/
	cd cld3-Java/
	ant jar

	echo "###"
	echo "### Install cld3-java.jar in maven repository ###"
	echo "###"
	mvn install:install-file -Dfile=cld3-java.jar -DgroupId=cld3-java -DartifactId=cld3-java -Dversion=1.0 -Dpackaging=jar
	cd ..

	echo "###"
	echo "### Build PDFExtract ###"
	echo "###"
	if [ -d "pdf-extract" ]; then rm -Rf "pdf-extract"; fi
	git clone -b poppler-rewrite https://github.com/bitextor/pdf-extract.git
	cd pdf-extract/PDFExtract
	mvn clean install -DskipTests
	cd ../../../
	cp $setupTmpDir/pdf-extract/PDFExtract/PDFExtract.json .
	cp $setupTmpDir/pdf-extract/PDFExtract/target/PDFExtract*.jar .
	ls -l
fi

END=$(date +%s)
DIFF=$(( $END - $START ))
echo "###"
echo "### Finish, process took $DIFF seconds ###"
echo "###"

