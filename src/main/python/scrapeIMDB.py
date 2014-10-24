#! /usr/bin/python
import sys 
import requests 
import lxml.html
from pymongo import MongoClient 

def getMovie(id): 
    try:
        hxs = lxml.html.document_fromstring(requests.get("http://www.imdb.com/title/" + id).content)
    except requests.exceptions.ConnectionError:
        return {}
    movie = {} 
    try: 
        movie['title'] = hxs.xpath('//*[@id="overview-top"]/h1/span[1]/text()')[0].strip() 
    except IndexError: 
        movie['title'] = '' 
    try: 
        movie['year'] = hxs.xpath('//*[@id="overview-top"]/h1/span[2]/a/text()')[0].strip() 
    except IndexError: 
        try: 
            movie['year'] = hxs.xpath('//*[@id="overview-top"]/h1/span[3]/a/text()')[0].strip() 
        except IndexError: 
            movie['year'] = "" 
    try: 
        movie['certification'] = hxs.xpath('//*[@id="overview-top"]/div[2]/span[1]/@title')[0].strip() 
    except IndexError: 
        movie['certification'] = "" 
    try: 
        movie['running_time'] = hxs.xpath('//*[@id="overview-top"]/div[2]/time/text()')[0].strip() 
    except IndexError: 
        movie['running_time'] = "" 
    try: 
        movie['genre'] = hxs.xpath('//*[@id="overview-top"]/div[2]/a/span/text()') 
    except IndexError: 
        movie['genre'] = [] 
    try: 
        movie['release_date'] = hxs.xpath('//*[@id="overview-top"]/div[2]/span[3]/a/text()')[0].strip() 
    except IndexError: 
        try: 
            movie['release_date'] = hxs.xpath('//*[@id="overview-top"]/div[2]/span[4]/a/text()')[0].strip() 
        except Exception: 
            movie['release_date'] = "" 
    try: 
        movie['rating'] = hxs.xpath('//*[@id="overview-top"]/div[3]/div[3]/strong/span/text()')[0] 
    except IndexError: 
        movie['rating'] = "" 
    try: 
        movie['metascore'] = hxs.xpath('//*[@id="overview-top"]/div[3]/div[3]/a[2]/text()')[0].strip().split('/')[0] 
    except IndexError: 
        movie['metascore'] = 0 
    try: 
        movie['description'] = hxs.xpath('//*[@id="overview-top"]/p[2]/text()')[0].strip() 
    except IndexError: 
        movie['description'] = "" 
    try: 
        movie['director'] = hxs.xpath('//*[@id="overview-top"]/div[4]/a/span/text()')[0].strip() 
    except IndexError: 
        movie['director'] = "" 
    try: 
        movie['stars'] = hxs.xpath('//*[@id="overview-top"]/div[6]/a/span/text()') 
    except IndexError: 
        movie['stars'] = "" 
    try: 
        movie['poster'] = hxs.xpath('//*[@id="img_primary"]/div/a/img/@src')[0] 
    except IndexError: 
        movie['poster'] = "" 
    try: 
        movie['gallery'] = hxs.xpath('//*[@id="combined-photos"]/div/a/img/@src') 
    except IndexError: 
        movie['gallery'] = "" 
    try: 
        movie['storyline'] = hxs.xpath('//*[@id="titleStoryLine"]/div[1]/p/text()')[0].strip() 
    except IndexError: 
        movie['storyline'] = "" 
    try: 
        movie['votes'] = hxs.xpath('//*[@id="overview-top"]/div[3]/div[3]/a[1]/span/text()')[0].strip() 
    except IndexError: 
        movie['votes'] = "" 
    return movie


client = MongoClient()
recsys = client.recsys
training = recsys.training
movies = recsys.movies
test = recsys.test

print "... Making list of movies already scraped ..."
# Find out movies already scraped so we dont re-scrape them
movies_existing = movies.distinct('movieID')

print "Movie count: "+str(len(movies_existing))

# Now find out the movies that need to be scraped
movieslist = test.distinct('movieID')

# Find out the subtraction of sets 
# i.e. movies which are to be scraped
# as well as not in the ones already
# in Mongo

movies_to_scrape = [item for item in movieslist if item not in movies_existing]
print "... Getting movies remaining to Scrape..."+"\n"
print "Movies left: "+str(len(movies_to_scrape))
movies_excluded = []
for sample_movie in movies_to_scrape:
    print "... Getting movie "+str(sample_movie)+" ..."+"\n"
    movieJson = getMovie("tt"+str(sample_movie))
    if any(movieJson):
        movieJson['movieID'] = sample_movie
        print "... Inserting movie data for "+str(sample_movie)+" ..."+"\n"
        print movieJson
        movies.insert(movieJson)
    else :
        print "... Could not retrieve page for movie "+str(sample_movie)+" ..."
        movies_excluded.append(long(sample_movie))
print movies_exlcuded
