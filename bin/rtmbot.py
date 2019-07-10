#!/usr/bin/env python3

# Script that logs slack messages to text files. Configure with a "rtmbot.conf"
# text file like this
#
#     SLACK_TOKEN: "xox-...."
#     LOGFILE: /var/log/slackbot.log
#
# Will create files like logs/2018-03-10.txt, containing one JSON object per
# line, for each event. Will log all events received from the Slack RTM API
# except for "pong", "hello", and "user_typing".

import sys
sys.dont_write_bytecode = True

import yaml
import json
import os
import sys
import logging

import datetime
import codecs

import slack

@slack.RTMClient.run_on(event='message')
def process_message(**payload):
    logging.info('got a message')
    data = payload['data']
    data['type'] = 'message'
    with codecs.open(datetime.date.today().strftime('logs/%Y-%m-%d.txt'), 'ab', 'utf-8') as f:
        f.write(json.dumps(data))
        f.write("\n")

def main_loop():
    if "LOGFILE" in config:
        logging.basicConfig(filename=config["LOGFILE"], level=logging.INFO, format='%(asctime)s %(message)s')
    logging.info('rtmbot started')
    try:
        rtm_client.start()
    except KeyboardInterrupt:
        sys.exit(0)
    except:
        logging.exception('Caught rtmbot exception.')

if __name__ == "__main__":
    config = yaml.load(open('rtmbot.conf', 'r'))
    slack_token = config["SLACK_TOKEN"]
    rtm_client = slack.RTMClient(token=slack_token)
    main_loop()
