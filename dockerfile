FROM groovy:alpine

USER root

COPY baker-bot.groovy baker-bot.groovy

ENTRYPOINT ["groovy", "baker-bot.groovy"]
