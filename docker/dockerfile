FROM groovy:alpine

USER root

RUN apk add git

RUN git clone https://github.com/gline9/baker-bot.git

COPY ../baker-bot.groovy baker-bot.groovy

ENV SLACK_API_TOKEN=xoxb-521685197296-521818117809-qdTRhDJSArBK3vBbi2qFLqLm

ENTRYPOINT ["groovy", "baker-bot.groovy"]
