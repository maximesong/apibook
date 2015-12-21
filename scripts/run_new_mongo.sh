#!/bin/bash
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
mkdir -p ${DIR}/../new_db
mongod --port 27017 --dbpath ${DIR}/../new_db/
