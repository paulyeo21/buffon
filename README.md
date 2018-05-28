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
Set-Refresh-Token: hk97pmcojiikm5nt:ade4tav783bmhr16olfslo28cqlet8pb461cgr3okdf3431ll1ptcmq6bg7ic0j9
Server: akka-http/10.1.0
Date: Thu, 26 Apr 2018 13:58:10 GMT
Content-Length: 0
```

POST /api/logout
```
$ curl -i -X POST -H 'Authorization: 86A1D2715C78BC49DE0793A220F0565D9A967E7F-DFEEBEC80A127894237CF350056CF24D7D7546A7CFB68D0C39C3DA61DA8BFB4B' http://localhost:8080/api/logout

HTTP/1.1 200 OK
Set-Authorization:
Set-Refresh-Token:
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

GET /api/current_login
```
$ curl -i -H 'Refresh-Token: hk97pmcojiikm5nt:ade4tav783bmhr16olfslo28cqlet8pb461cgr3okdf3431ll1ptcmq6bg7ic0j9' http://localhost:8080/api/current_login

HTTP/1.1 200 OK
Set-Authorization: 38115FC1413D78D60466AF9545F3F141A15CDC99-63BAA6DCE780A2AE80E512619DF47B187D7546A7CFB68D0C39C3DA61DA8BFB4B
Set-Refresh-Token: 465ajtq875isjvh:dbcs7a57d4pu4fk0ijdj4usrs5f4ikg46v1nuko5smae02f45g6tq42u0shoaats
Server: akka-http/10.1.0
Date: Wed, 02 May 2018 22:37:00 GMT
Content-Length: 0
```

GET /api/shoes?from=0&size=10
```
$ curl -i 'http://localhost:8080/api/shoes'

HTTP/1.1 200 OK
Server: akka-http/10.1.1
Date: Wed, 16 May 2018 23:04:06 GMT
Content-Type: application/json
Content-Length: 77

["{\"name\":\"air force 1\",\"brand\":\"nike\",\"createdAt\":1526510931721}"]%    
```

POST /api/shoes
```
$ curl -i -X POST -H 'Content-Type: application/json' -d '{"name":"kyrie_2", "brand":"nike", "createdAt":1526510931721}' http://localhost:8080/api/shoes 

HTTP/1.1 201 Created
Server: akka-http/10.1.1
Date: Wed, 16 May 2018 23:05:28 GMT
Content-Length: 0
```

GET /api/search?q=niek&from0&size=20
```
$ curl -i 'http://localhost:8080/api/search?q=niek'

HTTP/1.1 200 OK
Server: akka-http/10.1.1
Date: Wed, 16 May 2018 23:04:06 GMT
Content-Type: application/json
Content-Length: 77

["{\"name\":\"air force 1\",\"brand\":\"nike\",\"createdAt\":1526510931721}"]%    
```
