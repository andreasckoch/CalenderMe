#!/bin/bash
sshkey='calenderdb-key'

# setup of mongoDB
ssh $sshkey "sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 9DA31620334BD75D9DCB49F368818C72E52529D4"
ssh $sshkey "echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/4.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.0.list"
ssh $sshkey "sudo apt-get update"
ssh $sshkey "sudo apt-get install -y mongodb-org"

# start server daemon
ssh $sshkey "sudo service mongod start"

# create new database named calenderDB
ssh $sshkey "mongo --host 127.0.0.1:27017"
ssh $sshkey "use calenderDB"
