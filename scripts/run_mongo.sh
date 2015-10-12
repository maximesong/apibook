#!/bin/bash
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
mongod --port 27017 --dbpath ${DIR}/../db/
