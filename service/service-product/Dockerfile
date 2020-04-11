FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY ./target/service-product.jar service-product.jar
ENTRYPOINT ["java","-jar","/service-product.jar", "&"]