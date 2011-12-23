#!/bin/bash

TEMP_FILE="tmp.txt"
TEMP_ERROR="tmp.err"
# change this line for your server URL
URL_ROOT="http://localhost:8080"

die () {
    echo >&2 "$@"
    exit 1
}

# validate command line
[ "$#" -eq 1 ] || die "Invalid argument! Instance ID required. e.g. $0 i-00006bc0"

#echo "Demo, Desbribe DB Instance"
#echo "=========================="
#echo
curl -v -X GET -H "Accept: application/json" ${URL_ROOT}/daas/instance/$1 > $TEMP_FILE 2> $TEMP_ERROR

if [ -s $TEMP_FILE ]; then
	cat $TEMP_FILE | python -mjson.tool
else
	echo "No response. Check $TEMP_ERROR for error!"
fi
echo
rm $TEMP_FILE