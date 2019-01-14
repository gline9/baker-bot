@Grab('com.github.seratch:jslack:1.1.6')
@Grab('javax.websocket:javax.websocket-api:1.1')
@Grab('org.glassfish.tyrus.bundles:tyrus-standalone-client:1.13')
import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.shortcut.Shortcut
import com.github.seratch.jslack.shortcut.model.*
import com.github.seratch.jslack.api.methods.request.chat.*
import com.github.seratch.jslack.api.methods.request.bots.*
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def json = new JsonSlurper()

def token = System.getenv("SLACK_API_TOKEN")
def slack = Slack.getInstance()

def emojiList = [
    ':cookie:',
    ':doughnut:',
    ':pie:',
    ':cake:',
    ':birthday:'
];

def data = new File('/baker-bot/data/data.json')

def rawData;
if (data.exists())
{
    rawData = json.parseText(data.text)
}
else
{
    rawData = [:]
}

def personTotals = rawData.withDefault { [:].withDefault {0} }

def botUserId = getBotUserID(slack, token)

// Foobar

addShutdownHook {
    def outputFile = new File('/baker-bot/data/data.json');

    if (!outputFile.exists())
    {
        outputFile.getParentFile().mkdirs()
        outputFile.createNewFile()
    }

    outputFile.text = JsonOutput.toJson(personTotals)
}

println "starting rtm client"
slack.rtm(System.getenv("SLACK_API_TOKEN")).withCloseable {
    rtm ->

    rtm.addMessageHandler {
        jsonMessage ->
        try
        {
            def message = json.parseText(jsonMessage)

            println "Handling message: $message"

            def params = [
                message: message,
                slack: slack,
                emojis: emojiList,
                totals: personTotals,
                apiToken: token,
                botId: botUserId
            ]

            switch (message.type)
            {
                case 'message':
                    handleMessageEvent(params)
            }
        }
        catch (ex)
        {
            println ex
        }
    }

    println "rtm client connecting"
    rtm.connect();
    println "rtm client connected"

    while (true)
    {
        sleep(60000)
    }
}

def handleMessageEvent(Map params)
{
    if (params.message.bot_id)
    {
        return
    }

    def message = params.message.text

    if (message.contains('totals') && message.contains("<@$params.botId>"))
    {
        sendTotalInformation(params)
        return
    }

    def people = message.findAll(/<@(\w+)>/, {_, it -> it}).unique()

    people -= params.botId

    def total = 0

    def emojiList = '';

    params.emojis.forEach {
        emoji ->

        def count = message.count(emoji)

        people.forEach {
            params.totals[it][emoji] += count
        }

        emojiList += emoji * count
    }

    if (emojiList.isEmpty() || people.isEmpty())
    {
        return
    }

    replyTo(params, "<@$params.message.user> gave ${people.collect({ "<@$it>" }).join(' ')} $emojiList")
}

def sendTotalInformation(Map params)
{
    def personList = params.totals.collect {
        person, emojis ->

        def emojiList = emojis.collect {
            emoji, count ->
            emoji * count
        }

        "<@$person> ${emojiList.sort().join('')}"
    }

    def message = personList.join('\n')

    replyTo(params, "Totals:\n$message")
}

def replyTo(Map params, message)
{
    println "sending message"
    params.slack.methods().chatPostMessage(ChatPostMessageRequest.builder()
        .token(params.apiToken)
        .asUser(false)
        .channel(params.message.channel)
        .text(message)
        .iconEmoji(':male-cook:')
        .build())
}

def getBotUserID(slack, token)
{
    def response = slack.methods().botsInfo(BotsInfoRequest.builder()
        .token(token)
        .build())

    println response.dump()
    response.bot.userId
}

// println "Acquiring shortcut"
// Shortcut shortcut = slack.shortcut(token);

// println "Posting to channel"
// shortcut.post(ChannelName.of("general"), "hello world");

