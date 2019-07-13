#!/usr/bin/env python3

# Script that logs slack messages to text files. Configure with a "rtmbot.conf"
# text file like this
#
#     SLACK_TOKEN: "xox-...."
#     LOGFILE: /var/log/slackbot.log
#
# Will create files like logs/2018-03-10.txt, containing one JSON object per
# line, for each event. Will log all events in the channels except for
# "hello", "pong", and "user_typing". 

import sys
sys.dont_write_bytecode = True

import yaml
import json
import logging

import datetime
import codecs

import functools

import slack

rtm_event_list = [
    'accounts_changed',
    'bot_added',
    'bot_changed',
    'channel_archive',
    'channel_created',
    'channel_deleted',
    'channel_history_changed',
    'channel_joined',
    'channel_left',
    'channel_marked',
    'channel_rename',
    'channel_unarchive',
    'commands_changed',
    'dnd_updated',
    'dnd_updated_user',
    'email_domain_changed',
    'emoji_changed',
    'external_org_migration_finished',
    'external_org_migration_started',
    'file_change',
    'file_comment_added',
    'file_comment_deleted',
    'file_comment_edited',
    'file_created',
    'file_deleted',
    'file_public',
    'file_shared',
    'file_unshared',
    'goodbye',
    'group_archive',
    'group_close',
    'group_deleted',
    'group_history_changed',
    'group_joined',
    'group_left',
    'group_marked',
    'group_open',
    'group_rename',
    'group_unarchive',
#    'hello',
    'im_close',
    'im_created',
    'im_history_changed',
    'im_marked',
    'im_open',
    'manual_presence_change',
    'member_joined_channel',
    'member_left_channel',
    'message',
    'pin_added',
    'pin_removed',
    'pref_change',
    'presence_change',
    'presence_query',
    'presence_sub',
    'reaction_added',
    'reaction_removed',
    'reconnect_url',
    'star_added',
    'star_removed',
    'subteam_created',
    'subteam_members_changed',
    'subteam_self_added',
    'subteam_self_removed',
    'subteam_updated',
    'team_domain_change',
    'team_join',
    'team_migration_started',
    'team_plan_change',
    'team_pref_change',
    'team_profile_change',
    'team_profile_delete',
    'team_profile_reorder',
    'team_rename',
    'user_change',
#    'user_typing',
    ]

def process_event(event_type, **payload):
    data = payload['data']
    data['type'] = event_type
    with codecs.open(datetime.date.today().strftime('logs/%Y-%m-%d.txt'), 'ab', 'utf-8') as f:
        f.write(json.dumps(data))
        f.write("\n")

def main_loop():
    if "LOGFILE" in config:
        logging.basicConfig(filename=config["LOGFILE"], level=logging.INFO, format='%(asctime)s %(message)s')
    logging.info('rtmbot started.')
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
    for e in rtm_event_list:
        slack.RTMClient.on(event=e, callback=functools.partial(process_event, e))
    main_loop()
