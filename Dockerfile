FROM openjdk:17.0.2-jdk
ARG VERSION
ENV VERSION=$VERSION
COPY ./target/organizer-$VERSION.jar /temp/organizer-$VERSION.jar
#COPY ./credentials.properties /temp/credentials.properties
EXPOSE 8091
WORKDIR /temp
CMD ["java", "-jar", "organizer-$VERSION.jar"]