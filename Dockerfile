FROM gradle:8.10-alpine AS build
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME

COPY --chown=gradle:gradle . $APP_HOME/
USER root
RUN chmod +x $APP_HOME/gradlew
RUN cd $APP_HOME
COPY . .

RUN gradle clean build

# actual container
FROM eclipse-temurin:17-alpine
ENV APP_HOME=/usr/app
ENV PORT=4444

WORKDIR $APP_HOME
COPY --from=build $APP_HOME/build/libs/server-*.jar $APP_HOME/server.jar

EXPOSE $PORT
ENTRYPOINT ["java", "-jar", "server.jar"]