This application allows you to auto-post to a certain website where you need to do http requests to the site originating
from different IP addresses.

It does this by spinning up an AWS EC2 instance and passing through a user-data script (which is executed as a bash script
when the VM is started. After executing the script, the VP is shut down and ultimately terminated. The application then
sleeps for a configurable amount of time (in minutes) before repeating the process.

## Getting started:

*Copy config.properties.sample to config.properties and change the properties to your particular properties
*run mvn package
*run java -jar target/awsVoter-1.0-SNAPSHOT-jar-with-dependencies.jar

### Have fun...