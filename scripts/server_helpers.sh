#!/bin/bash

timestamp=$(echo $(($(date +'%s * 1000 + %-N / 1000000'))))

curl -X POST 'http://localhost:8080/api/shoes' \
-H 'Content-Type: application/json' \
-d \
'{
  "name": "yeezy",
  "brand": "adidas",
  "createdAt": '$timestamp'
}'

