#! /usr/bin/python
__author__ = 'mandar'
from pymongo import MongoClient
import sys, os
dirname = os.path.dirname
abspath = os.path.abspath

client = MongoClient()
recsys = client.recsys
training = recsys.training

DataPath = os.path.join(dirname(dirname(dirname(dirname(abspath(__file__))))),
                           "data")

movie = sys.argv[1]
tweetCur = training.find({'movieID': long(movie)},
                         fields={'_id': False,
                                 'uID': True,
                                 'engagement': True,
                                 'meta_data': True,
                                 'rating': True})
writeFile = open(os.path.join(DataPath, 'dump_'+movie+'.csv'), 'w')

for tweet in tweetCur:
    line = str(tweet['rating']) + "," + \
           str(tweet['meta_data']['user']['followers_count']) + \
           "," + str(tweet['engagement'])
    writeFile.write(line+'\n')

