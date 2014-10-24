import json
import sys

the_dataset_file = sys.argv[1]
the_output_file = sys.argv[2]
tweets = list()
header = True
outfile = file("../../"+the_output_file, 'w')
with file("../../"+the_dataset_file, 'r') as infile:
    for line in infile:
        if header:
            header = False
            #skip the CSV header line
            continue
        line_array = line.strip().split(',')
        user_id = line_array[0]
        item_id = line_array[1]
        rating = line_array[2]
        scraping_time = line_array[3]
        # The json format also contains commas
        tweet = ','.join(line_array[4:])
        # Convert the tweet data string to a JSON object
        json_obj = json.loads(tweet)
        #Use the json_obj to easy access the tweet data
        #e.g. the tweet id: json_obj['id']
        #e.g. the retweet count: json_obj['retweet_count']
        outfile.write('\t'.join([user_id, item_id, rating,
                                 str(int(json_obj['retweet_count']) +
                                     int(json_obj['favorite_count']))]) +
                      "\n")
