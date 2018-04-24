# Setup
Install Intellij CE: https://www.jetbrains.com/idea/download/#section=mac 

Once Intellij is up go to plugins and download Scala and SBT.

Install Cassandra:
1. brew install python
2. pip install cql
3. brew install cassandra

# Commands

sbt run

sbt test

sbt scalastyle

sbt docker:publishLocal

docker run \
--rn \
-p 8080:8080 \
shoe-dawg-backend:1.0

brew services start cassandra

cqlsh

[nodetool](http://cassandra.apache.org/doc/latest/tools/nodetool/nodetool.html)

# Endpoints

GET /api/login
```
$ curl -i -u email:password http://localhost:8080/api/login

HTTP/1.1 200 OK
Set-Authorization: 86A1D2715C78BC49DE0793A220F0565D9A967E7F-DFEEBEC80A127894237CF350056CF24D7D7546A7CFB68D0C39C3DA61DA8BFB4B
Server: akka-http/10.1.0
Date: Thu, 26 Apr 2018 13:58:10 GMT
Content-Length: 0
```

POST /api/logout
```
$ curl -i -X POST -H 'Authorization: 86A1D2715C78BC49DE0793A220F0565D9A967E7F-DFEEBEC80A127894237CF350056CF24D7D7546A7CFB68D0C39C3DA61DA8BFB4B' http://localhost:8080/api/logout

HTTP/1.1 200 OK
Set-Authorization:
Server: akka-http/10.1.0
Date: Thu, 26 Apr 2018 13:59:53 GMT
Content-Length: 0
```

POST /api/users
```
$ curl -i -X POST -H 'Content-Type: application/json' -d '{"email":"email", "password":"password"}' http://localhost:8080/api/users

HTTP/1.1 201 Created
Server: akka-http/10.1.0
Date: Thu, 26 Apr 2018 14:01:49 GMT
Content-Length: 0
```
