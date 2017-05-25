FROM openjdk:8

ENV kangaroo.port=8080
ENV kangaroo.host=0.0.0.0

EXPOSE 8080

CMD ["java", "-jar", "/usr/local/kangaroo/kangaroo-server-authz.jar"]

COPY ./target/kangaroo-server-authz-*-jar-with-dependencies.jar /usr/local/kangaroo/kangaroo-server-authz.jar
