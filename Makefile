build:
	sbt "core/run classes java/rt.jar thirdparty/lib/"

extract:
	sbt "core/run extract java/jdk.jar thirdparty/src/"
info:
	sbt "core/run info repository-sources"
index:
	sbt "core/run index"

web:
	sbt "~web/run"

typeExplain:
	sbt "core/run searchMethodTypes -n 30 --explain java.io.InputStream"
