#!/bin/sh

CURL=`which curl`

generateData(){
  cat <<EOF
{"name":"John Doe","address":"123 West Elm"}
EOF
}

URL='localhost:8080/dynamicClass/Json'

printf "\n== Posting data using malicious class\n"
$CURL --data "$(generateData)" -H "Content-Type: application/json" -X "POST" "$URL?m=malperson"

printf "\n== Posting data using class creation\n"
$CURL --data "$(generateData)" -H "Content-Type: application/json" -X "POST" "$URL?m=person"
printf "\n"

