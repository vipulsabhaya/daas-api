#!/bin/bash

TEMP_FILE="tmp.txt"
TEMP_ERROR="tmp.err"
# change this line for your server URL
URL_ROOT="http://localhost:8080"


# echo "Demo, Create Fleet"
# echo "==================="
# echo
curl -v -X POST -H "Accept: application/json" -H "Content-Type: application/json" -d '{"daas-create-request" : {"name" : "test", "flavor" : "standard.medium", "dbType" : {"version" : "1.0.2", "name" : "riak"}}}' ${URL_ROOT}/daas/instance > $TEMP_FILE 2> $TEMP_ERROR

if [ -s $TEMP_FILE ]; then
	cat $TEMP_FILE | python -mjson.tool
else
	echo "No response. Check $TEMP_ERROR for error!"
fi
echo
rm $TEMP_FILE

