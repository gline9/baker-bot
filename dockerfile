FROM groovy:alpine

USER root

COPY baker-bot.groovy baker-bot.groovy

ENV SLACK_API_TOKEN=xoxb-521685197296-521818117809-GuVHk6lWYHh8BSbJIUB7IRZ9

ENTRYPOINT ["groovy", "baker-bot.groovy"]
