web:
	sbt "~web/run"

build: 
	sbt "core/run classes java/rt.jar java/tools.jar thirdparty/lib/"
	sbt "core/run extract java/jdk.jar thirdparty/src/"

classes:
	sbt "core/run classes java/rt.jar thirdparty/lib/"

extract:
	sbt "core/run extract java/jdk.jar thirdparty/src/"
info:
	sbt "core/run info repository-sources"

methodNameIndex:
	sbt "core/run methodNameIndex"

methodIndex:
	sbt "core/run methodIndex"

methodTypesIndex:
	sbt "core/run methodTypesIndex"

typeExplain:
	sbt "core/run searchMethodTypes -n 30 --explain java.io.InputStream java.lang.String"

viewUsage:
	sbt "core/run viewUsage java.lang.Object.Object java.util.Iterator.next"

searchV2:
	sbt "core/run searchV2 --explain convert InputStream to String"

assembly:
	sbt -mem 2048 "core/assembly"

evaluate:
	sbt "core/run evaluate V0 V1 V2"

.PHONY: methodIndex methodNameIndex

countJDK:
	sbt "core/run countJar java/rt.jar"

questions:
	sbt "core/run questionList questions.csv"
