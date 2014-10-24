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

user = sys.argv[1]
tweetCur = training.find({'uID': long(user)},
                         fields={'_id': False,
                                 'uID': True,
                                 'engagement': True,
                                 'meta_data': True,
                                 'movieID': True,
                                 'rating': True})

user_movies = training.find({'uID':long(user)}, 
                            fields={'_id':False, 
                                    'movieID':True})

user_movies_list = []
print "... Making list of user movies ..." + "\n"
for movie in user_movies:
    user_movies_list.append(long(movie['movieID']))

print user_movies_list
key = [ 'movieID' ]
condition = {'movieID': {'$in': user_movies_list}}
initial = { 'count': 0, 'sum': 0 }
reduce = 'function(doc, out) { out.count++; out.sum += doc.rating; }'
finalize = 'function(out) { out.mean = out.sum / out.count; }'
print "... Finding means of user movies ..." + "\n"
agg_res = training.group(key, condition, initial, reduce, finalize)

agg_dic = {}
print agg_res

print "... Making dict out of movie rating means ..." + "\n"
for agg_res_s in agg_res:
    agg_dic[long(agg_res_s['movieID'])] = float(agg_res_s['mean'])
print str(agg_dic)+"\n"

writeFile = open(os.path.join(DataPath, 'dump_user_'+user+'.csv'), 'w')
writeFile.write("movieID,rating,avg,absdev,engagement\n")
print "... Writing to file ..." + "\n"
for tweet in tweetCur:
    line = str(tweet['movieID']) + "," + str(tweet['rating']) + \
           "," + str(agg_dic[long(tweet['movieID'])]) + "," + \
           str(abs(tweet['rating']-agg_dic[long(tweet['movieID'])])) + \
           "," + str(tweet['engagement'])
    writeFile.write(line+'\n')

