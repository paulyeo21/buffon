# Setup
Install Intellij CE: https://www.jetbrains.com/idea/download/#section=mac 

Once Intellij is up go to plugins and download Scala and SBT.

# Commands
sbt run
sbt test
sbt scalastyle
sbt docker:publishLocal

docker run \
--rn \
-p 8080:8080 \
shoe-dawg-backend:1.0
