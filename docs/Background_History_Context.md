# Clojurians-log Slack Log

## Introduction

The most active online community for Clojure is Clojurians Slack. But Slack is
closed off. You need to log in to see what people are saying. Search engines
don't index it. You can link to individual posts, but those links will still
only work for people with accounts.

We're also at the mercy of Slack the company. For a long time we were on a free
plan which only allowed people to see and search the last 10k(?) messages. At
the time this meant a horizon of about 2 weeks.

A simple solution to this is a HTML archive, for a long time this has been
provided by https://clojurians-log.clojureverse.org. It's a simple app that logs
events coming from Slack ("user posts message", "user deletes message", "emoji
reaction", "thread reply", etc.), and has routes to show all messages for a
given channel and a given day.

## Clojurians-log UI

- route that renders all messages for a given date/channel
- thread messages are rendered with the thread head, so on the date of the first message
- each page has an overview of all channels that have messages on the same date, with a count of the number of messages
- there's also a route for each message, this renders the same date/channel history, but scrolls to and highlights the message
- route for a user's info 

## History

The original site was static HTML generated from a bunch of PHP and Node.js
scripts (IIRC). It would use the slack RTM (Real-time messaging) API to
establish a websocket and wait for JSON events. These were appended to JSONL
files, one per day.

I don't remember who started the project, I'd have to see what I can find out
about that, but looking at the data it must have been in 2015. I (Arne) took
over the project not too long after (2016 or 2017?) when the original creator no
longer wanted to maintain it.

It was a pain to work with because regenerating all the HTML took ages, so e.g.
improving the design was a PITA. Rendering was extremely basic back then, and I
made multiple improvements to make it a bit nicer. I also generally wanted to
make it more attractive for people to contribute, and so I rewrote it as a
Clojure app, and for extra cool points it uses Datomic. Turns out Datomic is not
great for this use case, which I will elaborate on later.

For actually listening for events we used a separate python script,
(https://github.com/clojureverse/rtmbot), which would just append to JSONL
files. This meant that e.g. if we did a new deploy and had some downtime, the
archiving would just carry on. We tried to make this super simple and stable,
with vendored dependencies, but breaking changes in Python did manage to break
it eventually, which is why the archiving broke ~1.5 years ago. I tried just now
if I could revive it, but since then the legacy token type we used has been
deprecated. 

The JSONL files, one per day, were added to a git repo for archiving and
distribution. This is what I consider the actual primary archive, from which we
can always rebuild. Git isn't ideal for this, and the repo (cloned and checked
out) is now 5.1G in size, but it's been fairly convenient.

The actual [repo with logs](https://github.com/plexus/clojurians-log) is
private, but there's a [small public
subset](https://github.com/clojureverse/clojurians-log-demo-data), mainly for
people to test with locally.

We've kept this private because e.g. if someone deletes a message, both the
original message and the delete event are in there, as well as general concerns
around enabling abuse if we make this available in bulk in a machine readable
format.

A cron job would regularly add and push changes to these archive files to the
repo. It would also trigger an import of any new data into datomic, so it would
show up on the site.

## Challenges

Taken at face value it seems like a simple problem, you can even just render a
static HTML page for every channel/day combination. In practice however
everything on Slack is mutable.

- people can edit old messages, from the beginning of history to now
- people can delete old messages
- people can add or remove emoji reactions to old messages
- people can respond to old threads, or start threads from old messages (so a message today may belong on a page from a year ago)
- people can change their username, and this is reflected in mentions
- channel names can change, and this is reflected in mentions
- some users and channels categorically don't want to be logged/indexed

When we were still on the free plan, we could assume that after about two weeks
things had dropped off the history accessible in slack, and so would no longer
change, but that's no longer the case.

Some examples of things that have happened or might happen:

- someone posts a company announcement prematurely, they delete the post, but it's still up on the archive
- a person changes their name (e.g. after transitioning) but their deadname still shows up on the site
- GDPR related requests like "right to be forgotten"

We've been contacted by cases like these, and the easiest way to prevent having
to do manual admin stuff like this is to make sure the archive quickly and
accurately reflects what's on the site.

## Scrapers, Memory, Caching

Since switching from a static build to a Clojure/Datomic app, managing load and
memory pressure has always been a challenge. The log runs on a 8GB/4vCPU
machine, which isn't massive, but at the same time seems like it should be
reasonable for what it does.

Actual humans visiting the site is not the problem, the challenge is what
happens when multiple scrapers/crawlers stop by. We might have
Google+Yandex+Yahoo crawl the site at the same time over a period of multiple
days. The server would get overloaded, requests would start timing out.

This of course has gotten a 100x worse now that we're in the AI space race.

Part of why this is challenging is that we effectively have a page/url for each
individual message, making caching not very effective. Crawlers by definition
don't hit the same URL multiple times, but hit a large number of them in
succession.

This also impacts Datomic, which for performace relies on keeping recently used
blocks in memory in the peer, which works well in typical applications where a
small subset of all data is actively in use at a given time, but here it's
effectively random across the entire history.

That all said, with some careful tweaking of the JVM memory flags for both the
transactor and the application, by putting CloudFlare in front and letting it
cache, and by setting up a [aggressive bot
blocklist](https://github.com/mitchellkrogza/nginx-ultimate-bad-bot-blocker)
we've managed to keep this mostly under the control, sacrificing some liveness
because of the caching.


## Slack (legacy) bots

Being over a decade old, the slack log has been built using "legacy" Slack
integrations, in particular we had a long running legacy token with broad
access. This was very convenient, because it allowed to receive events from the
entire workspace. With newer bots/tokens permissions are more fine-grained,
which means the bot user needs to be in the channel.

As of March 2025 legacy tokens are officially removed, and so we need to move to
the modern stuff. This means the old @logbot bot is officially defunt, and
@logbot2 is the future.

## Getting Messages out of Slack

When we are talking about messages we are really talking about events, e.g. we
are also interested in edits, deletes, emoji reactions, etc. Slack represents
these are JSON objects with a `"type"` and `"ts"`, a fine-grained UNIX timestamp
which also acts as a primary identifier (they guarantee uniqueness per channel).

There are three different approaches of getting these events, real-time, through
the channel history API, or through a workspace-wide export.

Real-time is most interesting to us, but the others are useful for backfilling.

There are three different APIs/mechanisms for getting real time events from
Slack. What we have used in the past is the RTM API (RTM=real-time messaging),
where you connect to Slack via a websocket and receive events. The RTM API
requires a legacy token, but legacy tokens no longer work, so it's effectively
no longer available.

See [RTMBot](https://github.com/clojureverse/rtmbot)

The modern equivalent is the Socket API. The details differ but the principle is
the same, connect to a websocket, get events.

See [co.gaiwan.slack.socket.api](https://github.com/GaiwanTeam/clj-slack/blob/main/src/co/gaiwan/slack/socket/api.clj)

The third option is to configure a webhook. Slack will do a HTTP POST for each
event. You have three seconds to confirm delivery, or slack will retry. So
there's some redudancy/retry logic built-in, which is appealing. It's a bit more
annoying to work with for local dev at least, since you need a publicly
accessible HTTPS endpoint. I use [FRP](https://github.com/fatedier/frp) for
that.

This is the mechanism [slack-event-sink](https://github.com/GaiwanTeam/slack-event-sink) uses. 

## Slack Markdown

Slack uses a variant/subset of markdown. The clojurians-log app contains its own
parser for it, and there's an improved version of it in `GaiwanTeam/clj-slack`.

One challenge is that user and channel references are in the markdown as
raw identifiers, e.g.

```
Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR>: here is the `my-ns.core` code
```

Another challenge are emojis like `:simple_smile:` or `:+1::skin-tone-2`. We
need to scrape the list off of Slack whenever they add new ones, there's no
comprehensive up-to-date public list of these.

## Attachments

Attachments (mainly images) so far have not been handled by the archiver. IIRC
we use the image link as hosted by slack, if we show them at all. It's something
I'd like to address going.

## Hosting

I originally paid for the hosting as a sole proprietor doing my Lambdaisland
stuff. After 2019 Gaiwan footed the bill. We got sponsorship from Exoscale in
the form of credits a few times, last time as part of sponsoring for Heart of
Clojure 2024, but those have all long run out, and we're looking to migrate this
and other things off of Exoscale, since they are quite expensive. That's also
why we've tried our best to make due with a relatively small VM.

Current thinking is to move this to a Scaleway account paid for by my Belgian
company Magpie, until we've figured out a better long-term home, ideally
something controlled directly by the community or a non-profit.

DNS is with DNSimple, clojurians-log.clojureverse.org goes to CloudFlare,
clojurians-log-internal.clojureverse.org goes to the machine itself.

## WIP

Mitesh and I have both worked on a number of pieces that are supposed to become
the "new" clojurians-log. There's actually a lot there, but life and other
issues have gotten in the way of us actually shipping it out.

The [GaiwanTeam/clj-slack](https://github.com/GaiwanTeam/clj-slack) repo
contains a slew of utilities, it's a shared toolkit we've developed across
multiple projects (not just clojurians-log, we've done a bunch of interesting
things with slack for clients as well). It contains wrappers for the various
APIs, markdown handling code, code for working with slack event data on disk or
in memory, code for "backfilling" archival data by pulling it from the
conversations API, etc. This used to a private repo but we've made it public, I
might move it to `lambdaisland/slack-sdk` or something, to make clear it's not
related to the [other clj-slack](https://github.com/julienXX/clj-slack).

Mitesh worked on a PostgreSQL based rewrite,
[clojurians-log-v2](https://github.com/GaiwanTeam/clojurians-log-v2), but didn't
really account for how to get new data in. The big appeal is that it would give
people a full-text search.

I would like to retain the separation between a minimal process that collects
raw logs, the raw logs at rest for long-term archival, and an app/frontend for
rendering and serving them up. This last one could be backed by a database
(datomic, postgres), or simply use a file-based storage approach.

I've done work on a new
[Slack-event-sink](https://github.com/GaiwanTeam/slack-event-sink) for capturing
events. This also archives images/attachments. I've also experimented with
filesystem based approaches as part of our clj-slack repo.

## Filesystem based approach

The idea here is to keep an intermediate format, similar to the raw archive
data, but instead of having files for each date that the event happened, have a
file for each channel+date where the event/message needs to be rendered. So e.g.
an edit, emoji reaction, thread reply etc that happens on a different day is
still appended to the file for a given channel+day. Querying for this is pretty
much the only thing we use the database for. It gives us a simple format that is
easy to update (just append messages at the end), and that can be rendered at
lightning speed, while no longer having the overhead of bulk-ingesting into a
database.

The only other information you need are channels.json and users.json, which you
can pull and store periodically, and the channel/date message count stats, which
you can also rebuild and store periodically. Both of these can be stale, that's
not a huge issue.

## Repos

- Raw logs: https://github.com/plexus/clojurians-log [private]
- Demo logs, for local dev: https://github.com/clojureverse/clojurians-log-demo-data
- Clojure App: https://github.com/clojureverse/clojurians-log-app
- clj-slack supporting library: https://github.com/GaiwanTeam/clj-slack
- Mitesh's v2: https://github.com/GaiwanTeam/clojurians-log-v2
- RTMBot (python): https://github.com/clojureverse/rtmbot
- Slack Event Sink: https://github.com/GaiwanTeam/slack-event-sink
- Slack Backfill: https://github.com/lambdaisland/slack-backfill
