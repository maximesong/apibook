#!/bin/bash
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
mkdir -p ${DIR}/../db
mongod --port 27017 --dbpath ${DIR}/../db/
