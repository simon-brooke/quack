#!/bin/bash

docs=../dog-and-duck/resources/activitystreams-test-documents/

for file in ${docs}/*.json
do
    name=`basename ${file} | sed s/json$/html/`

    echo ${name}
    java -jar target/uberjar/quack-0.1.0-SNAPSHOT-standalone.jar \
        -i ${file} \
        -o "docs/samples/${name}" \
        -f html
done