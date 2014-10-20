recsys2014
==========

Project based on the data provided in the Recommendation Systems 2014 conference. Uses Lenskit and Lucene to generate recommendations.

Introduction
============

This repository is a recommender algorithm evaluator based on the RecSys Challenge 2014. It uses the Twitter data set and generates recommendations as well as ranks tweets from the test set based on their predicted 'engagement' count. 

Technologies
============
Lenskit coupled with Lucene are used to generate the recommendations and predict the engagement scores. MongoDB is used to store the training and test data sets. Evaluation runs of various algorithms are also stored so that we can compare the performance (nDCG@10) of 1) User User 2) Lucene Item Item boosted by IMDB movie meta data. 3) Singular Value Decomposition, for various values of algorithm parameters.

A Mahout implementation of collaborative filtering algorithms is also experimented with.

Requirements
=============
1) Lenskit 2.0.1-M4 or higher (https://github.com/lenskit/lenskit)
2) Apache Lucene 3.8 (https://github.com/apache/lucene)
3) Apache Mahout (https://github.com/apache/mahout)
4) MongoDB and MongoDB Java driver (http://www.mongodb.org/)
