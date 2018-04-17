#!/usr/bin/env python

# Script that logs slack messages to text files. Configure with a "rtmbot.conf"
# text file like this
#
#     SLACK_TOKEN: "xox-...."
#     LOGFILE: /var/log/slackbot.log
#     DAEMON: True
#
# Will create files like logs/2018-03-10.txt, containing one JSON object per
# line, for each event. Will log all events received from the Slack RTM API
# except for "pong", "hello", and "user_typing".

import sys
sys.dont_write_bytecode = True

import glob
import yaml
import json
import os
import sys
import time
import logging

from datetime import date
from json import dumps
import codecs

from slackclient import SlackClient

def process_message(data):
    with codecs.open(date.today().strftime('logs/%Y-%m-%d.txt'), 'ab', 'utf-8') as f:
        # print(dumps(data))
        f.write(dumps(data))
        f.write("\n")

class RtmBot(object):
    def __init__(self, token):
        self.last_ping = 0
        self.token = token
        self.slack_client = None
    def connect(self):
        """Convenience method that creates Server instance"""
        self.slack_client = SlackClient(self.token)
        self.slack_client.rtm_connect()
    def start(self):
        self.connect()
        while True:
            for reply in self.slack_client.rtm_read():
                self.input(reply)
            self.autoping()
            time.sleep(.1)
    def autoping(self):
        #hardcode the interval to 3 seconds
        now = int(time.time())
        if now > self.last_ping + 3:
            self.slack_client.server.ping()
            self.last_ping = now
    def input(self, data):
        if "type" in data and not data["type"] in {"pong", "user_typing", "hello"}:
            process_message(data)

def main_loop():
    if "LOGFILE" in config:
        logging.basicConfig(filename=config["LOGFILE"], level=logging.INFO, format='%(asctime)s %(message)s')
    logging.info(directory)
    try:
        bot.start()
    except KeyboardInterrupt:
        sys.exit(0)
    except:
        logging.exception('OOPS')

if __name__ == "__main__":
    directory = os.path.dirname(sys.argv[0])
    if not directory.startswith('/'):
        directory = os.path.abspath("{}/{}".format(os.getcwd(),
                                directory
                                ))

    config = yaml.load(file('rtmbot.conf', 'r'))
    bot = RtmBot(config["SLACK_TOKEN"])
    site_plugins = []
    files_currently_downloading = []
    job_hash = {}

    if config.has_key("DAEMON"):
        if config["DAEMON"]:
            import daemon
            with daemon.DaemonContext():
                main_loop()
    main_loop()
