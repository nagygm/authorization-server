ARG project_name=authorization-server
ARG root_project_path=authorization-server
ARG app_project_path=authorization-server/server
ARG profile_default=development

FROM gradle:jdk11 AS GRADLE_BUILD
ARG project_name
ARG root_project_path
ARG app_project_path
ARG profile_default

ENV APP_HOME=/usr/app
WORKDIR $APP_HOME
ENV PROFILE=$profile_default
COPY ./$root_project_path $APP_HOME/$root_project_path
WORKDIR $APP_HOME/$root_project_path
RUN gradle assemble
WORKDIR $APP_HOME/$app_project_path
RUN ls $APP_HOME/$app_project_path/build/libs/
RUN gradle properties -q | grep "^version:" | awk '{print $2}' > release.version

FROM azul/zulu-openjdk-alpine:11

ARG project_name
ARG root_project_path
ARG app_project_path
ARG profile_default

ENV APP_HOME=/usr/app
ENV PROFILE=$profile_default
WORKDIR $APP_HOME

COPY --from=GRADLE_BUILD $APP_HOME/$app_project_path/release.version ./release.version
COPY --from=GRADLE_BUILD $APP_HOME/$app_project_path/build/libs/ ./
RUN export release_version=$(cat "./release.version"); mv $project_name-$release_version.jar ./app.jar
EXPOSE $PORT

RUN chmod u+x /usr/local/bin/run.sh
ENTRYPOINT ["/usr/local/bin/run.sh"]
