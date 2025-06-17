***To view my ER Diagram, please check ERDiagram.pdf***

***To manually export classpath, please use***
export CLASSPATH=lib/sqlite-jdbc-3.45.1.0.jar:lib/slf4j-api-1.7.36.jar:lib/slf4j-nop-1.7.36.jar:lib/junit-4.13.2.jar:lib/hamcrest-all-1.3.jar:lib/javax.json-1.0.jar:lib/json-20250107.jar:.


***To run InitialiseDB.java***

Run:
./initialiseDB.sh

This initialises the database by creating the tables

***To run PopulateDB.java***

Run:
./populate.sh

This populates the tables with the data in the .csv files.

***Quering the database***

Usage: ./query.sh <query_number> [additional_parameters]

***The functions of the various Queries aling with the query number***

1) List the titles of all the movies in the database.
2) List the names of the actors who perform in some specified movie.
3) List the plots of movies with a specified actor in them and directed by some particular director.
4) List the directors of the movies that have a particular actor in them.
5) List movies that have won an Oscar and have a rating between 7.0-9.0 and lists the number of Oscars won.
6) List all actors who have 2 or more awards and have starred movies with ratings of 8 and above.

***Testing the code using Junit***

The java testing source files are added in /src/test directory, but it is recommended to install the Java Tests extension and run the tests in the Testing where you can see all the test results.