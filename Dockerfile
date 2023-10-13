FROM openjdk:20-jdk
COPY ./target/organizer-*.jar /temp/organizer-*.jar
COPY ./credentials.properties /temp/credentials.properties
EXPOSE 8080
WORKDIR /temp
CMD java -jar $(ls organizer-*.jar)