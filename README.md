# Application Project

[![N|Solid](http://www.cbronline.com/wp-content/uploads/2016/06/Java.png)](https://www.java.com/tr/download/)

[![N|Solid](https://106c4.wpc.azureedge.net/80106C4/Gallery-Prod/cdn/2015-02-24/prod20161101-microsoft-windowsazure-gallery/instaclustr.170be28c-6b1c-4f41-8039-8822de65f8c2.1.0.0/Icon/large.png)](http://cassandra.apache.org/)

[![Build Status](https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTc-O-WnqZQiy1GGeWiZjdV_OJucLGjOgnKH6HLic2hJcYcHFf2)](https://kafka.apache.org/)

[![Build Status](https://www.onlinebooksreview.com/uploads/blog_images/2017/11/27_file.png)](https://spark.apache.org/)

[![Build Status](https://www.docker.com/sites/default/files/social/docker_facebook_share.png)](https://www.docker.com/)

[![Build Status](https://cdn.webrazzi.com/uploads/2012/08/a28e8_node-js.jpg)](https://nodejs.org/en/)

Project is a real-time, continuously generated logs can be shown in the browser as real time and be stored in a database. Developed using Spring MVC and Maven technologies. As well as Apache Kafka, Apache Spark technologies were used. In the final stage, Docker was used for dockerize my solution as a docker image / container on.

  - The application shows the number of rows per city in a dashboard in real time
  - Data is stored in the database

### Technologies

For more information on the technologies used in this application:

![Working Mechanism](https://i.postimg.cc/ZRqnsRjq/GZp23b.png)

First of all, we need to know that we have 3 separate projects.These;  
-Log Creater,
-Spark Streamer,
-Real-time Dashboard

In Log Creater project; we have 5 different servers reside on different cities ( Istanbul,Tokyo,Moskow, Beijing,London ) and logs produces from 5 different servers this section. After that,logs pushes to a kafka message broker.
Kafka server has two topic.
-refined
-unrefined
As the name suggests,unrefined, not processed to remove impurities or unwanted elements. Refined, just the opposite, processed. Logs conveys from the refined topic of the Kafka server to Spark Streamer.

In Spark Streamer project, Spark serves as both consumer and producer. Spark also sends it to the Cassandra database. At this point, it acts as a Producer. After that, Spark, sends the logs back to the refined topic of the Kafka server.Logs sent from Kafka server to Realtime Dashboard.

In Realtime Dashboard project makes it appear of logs on a live dashboard. RealTime Dashboard is a consumer.



* jetbrains.com - Intellij IDEA
* nodejs.org/en - Node.js is a JavaScript runtime built on JavaScript engine.
* kafka.apache.org - A distributed Streaming Platform
* spark.apache.org - Lightning-fast unified analytics engine
* docker.com - Enterprise Container Platform for High-Velocity Innovation
* cassandra.apache.org - Manage massive amounts of data, fast, without losing sleep

### Installation

Make sure the Docker is installed before proceeding! 
- How to install Docker for Ubuntu?

1-) Update Software Repositories
As usual, it’s a good idea to update the local database of software to make sure you’ve got access to the latest revisions.Therefore, open a terminal window and type:

```sh
sudo apt-get update
```
Allow the operation to complete

2-) Uninstall Old Versions of Docker
Next, it’s recommended to uninstall any old Docker software before proceeding.

Use the command:

```sh
sudo apt-get remove docker docker-engine docker.io
```

3-) Install Docker
To install Docker on Ubuntu, in the terminal window enter the command:
```sh
sudo apt install docker.io
```
4-) Start and Automate Docker
The Docker service needs to be setup to run at startup. To do so, type in each command followed by enter:
```sh
sudo systemctl start docker
```
```sh
sudo systemctl enable docker
```
5-) Check Docker Version
To verify the installed Docker version number, enter:
```sh
docker --version
```

- How to install Docker Compose for Ubuntu?
 1-) Download the Docker Compose binary into the /usr/local/bin directory with the following curl command:
```sh
sudo curl -L "https://github.com/docker/compose/releases/download/1.23.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```
2-)Once the download is complete, apply executable permissions to the Compose binary:
```sh
sudo chmod +x /usr/local/bin/docker-compose
```
3-)Verify the installation by running the following command which will display the Compose version:
```sh
docker-compose --version
```
The output will look something like this:
```sh
docker-compose version 1.23.1, build b02f1306
```

- How can I run applications and services:

1-)Clone Project
```sh
git clone https://github.com/nryasln34/spark-analysis-streamer-.git
```
2-)
```sh
cd spark-analysis-streamer-/
```
3-)
```sh
docker-compose up
```
4-)Don't close this console and open new console after
```sh
cd log-creator
```
5-)Run log-creator application
```sh
mvn spring-boot:run
```
6-)Don't close this console and open new console after come back to spark-analysis-streamer-'s directory(cd ..) after
```sh
wget https://archive.apache.org/dist/spark/spark-2.1.0/spark-2.1.0-bin-hadoop2.6.tgz
```
```sh
tar -xvzf spark-2.1.0-bin-hadoop2.6.tgz
```
7-)
```sh
cd spark-streamer
```
8-)create runnable jar
```sh
mvn clean compile assembly:single
```
9-)come back to spark-analysis-streamer-'s directory(cd ..)
```sh
./spark-2.1.0-bin-hadoop2.6/bin/spark-submit --jars spark-streaming_2.11-2.1.0.jar --class com.example.spark.streamer.Application spark-streamer/target/spark-streamer-1.0-SNAPSHOT-jar-with-dependencies.jar localhost:9092 unrefined
```
10-)Don't close this console and open new console after
```sh
cd real-time-dashboard
```
11-)install npm packets
```sh
npm install
```
12-)start nodejs server
```sh
node index.js
```
13-)Browser opens and write localhost:3001
You can watch it here.
14-)You can download gui to access cassandra (Dbeaver EE)
15-)Click the socket mark on the top left.
16-)Choose Cassandra CQL
17-)Confirm Test Connection and then finish
18-)KeySpaces -> teb -> Tables -> logdetails
    Right Click -> Read Data in SQL console
- How can I see Dashboard?
Open your browser and write localhost:3001
You can see the real-time dashboard here.
View from the dashboard:
![Dashboard View](https://i.postimg.cc/8PFJVtWm/dashboard.png)

