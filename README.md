# Lucene Analyzer

This script uses SimpleAnalyzer lucene to create indexes for the provided set of html documents. It also uses lucene to score documents for every query provided. This code was build on top of the HW4.java provided in the assignment itself.

## SETUP

1. Install Eclipse
2. Use any version of Java after 1.5
3. Import SmallSearchEngine.  
4. Add the required libraries to the build path. The required libraries are - lucene-core-4.7.2.jar, lucene-analyzers-common-4.7.2.jar and lucene-queryparser-4.7.2.jar. These libraries are inside the project as well.

## ABOUT THE CODE

1. On running the code, first we would need to provide the Location where indexes created can be stored.

2. Then we would need to provide the location (full path) of the corpus or the files to be indexed.

3. A file named "term and term-freq.txt" is created by the code in the project folder. This file contains the list of all the terms and their frequency of occurance in  all the documents.

4. A file named "Zipf's law values.txt" is created by the code in the project folder. This file contains the rank of each term and its probability of occurance. On plotting these values in Microsoft Excel we can see the graph proving Zipf's law. A file named "Zipf's law" has already been provided showing the same.

5. Then query for which scoring needs to be done is provided.

6. For every query a file name "query <given query>.txt" is created. It contains the top 100 documents ranked by their respective scores. Snippets are also generated for these documents. A snippet is generated starting with the first word occurance of any term of the query. The snippet is 200 characters long unless the document length does not support 200 characters. In that case the snippet goes to the end of the document.

7. Total hits for any query can be checked by changing the value in TopScoreDocCollector to a very high value. The comparison of the total hits is provided in the file - TotalHitsComparison.doc.

8. Press 'q' to quit/exit the program.

## CONTACT

Please contact 'Anirudh Devgun' at 'devgun.a@husky.neu.edu' in case of any issues.