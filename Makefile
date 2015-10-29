build:
	sbt "core/run classes java/rt.jar thirdparty/lib/"

extract:
	sbt "core/run extract java/jdk.jar thirdparty/src/"
info:
	sbt "core/run info repository-sources"
