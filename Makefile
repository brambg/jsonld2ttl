all: help
TAG = jsonld2ttl
SHADOW_JAR=./jsonld2ttl/build/libs/jsonld2ttl-1.0-SNAPSHOT-all.jar
NEWER_SOURCE_FILES=$(shell find jsonld2ttl/src/main -newer $(SHADOW_JAR) -type f)

~/bin/:
	mkdir -p ~/bin

~/bin/jsonld2ttl: ~/bin/ ./jsonld2ttl/src/main/bash/jsonld2ttl.sh ~/libs/jsonld2ttl.jar
	cp -a ./jsonld2ttl/src/main/bash/jsonld2ttl.sh ~/bin/jsonld2ttl

~/libs:
	mkdir -p ~/libs

~/libs/jsonld2ttl.jar: $(SHADOW_JAR) ~/libs
	cp $(SHADOW_JAR) ~/libs/jsonld2ttl.jar

$(SHADOW_JAR): jsonld2ttl/build.gradle.kts settings.gradle.kts $(NEWER_SOURCE_FILES)
	./gradlew shadowJar
	@echo
	@touch $@

.PHONY: shadow-jar
shadow-jar:
	@make $(SHADOW_JAR)

.PHONY: test
test:
	./gradlew test

.PHONY: clean
clean:
	./gradlew clean

.PHONY: install
install: ~/bin/jsonld2ttl

.PHONY: help
help:
	@echo "make-tools for $(TAG)"
	@echo
	@echo "Please use \`make <target>', where <target> is one of:"
	@echo "  clean         - to clear the build files"
	@echo "  tests         - to test the projejsonld2ttl"
	@echo "  shadow-jar    - to build the shadow jar build/libs/elabjsonld2ttll.jar"
	@echo "  install       - to install the shadow jar in ~/libs and batch script 'jsonld2ttl' in ~/bin/"
