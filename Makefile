build:
	sbt "core/run classes java/rt.jar thirdparty/lib/"

extract:
	sbt "core/run extract java/jdk.jar thirdparty/src/"
info:
	sbt "core/run info repository-sources"
methodIndex:
	sbt "core/run methodIndex"

methodTypesIndex:
	sbt "core/run methodTypesIndex"

web:
	sbt "~web/run"

typeExplain:
	sbt "core/run searchMethodTypes -n 30 --explain java.io.InputStream java.lang.String"

viewUsage:
	sbt "core/run viewUsage java.lang.Object.Object java.util.Iterator.next"

searchV2:
	sbt "core/run searchV2 --explain convert InputStream to String"

assembly:
	sbt -mem 2048 "core/assembly"
