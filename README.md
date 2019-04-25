# News Trends Demo

The News Trends Project processes news content from sources around the United States and presents a graphical representation of trending words to the application user. The project uses thread-level and data-level parallelism to retrieve, process and combine the results. 

During processing, words which are common and not meaningful for establishing word trends are removed (these settings be can changed in the application properties to further restrict the results or to broaden them).

Sample words that are removed: I, at, there, the, of, on etc.

News content is retrieved from https://newsapi.org/.
Developmer Documentation: https://newsapi.org/docs

Getting Started
==========================

There are a few steps that you need to take before you can start processing data and analyzing trends.

Creating a NewsAPI account
--------------------------
Before you can begin running the application, you will need to create a developer account with NewsAPI and get an apiKey so that you can pull down data.

To create an account go to: https://newsapi.org/register
Keep track of the API Key that they give you to use in step #2.

**Important Note: Only 500 requests per day are allowed from a developer account. If you start seeing 429s this means that you have exceeded the allowed limits for the day.

Cloning, Compiling, and Running
--------------------------------

1. First, clone down the repository. 
```sh
$ git clone <paste github URL from git>
$ cd news-trends
```
2. Edit the application properties so that it points to your local folder and api key
In the `news-trends/src/main/resources/Application.properties` folder, edit the following properties:

```
api.key={your_api_key}
path.to.data={path_to_project}/news-trends/news/
path.to.reports={path_to_project}/news-trends/src/main/static
```
Examples have been to the file already to make it easier to understand the format.

3. From the news-trends directory compile the project
```sh
$ cd news-trends
$ mvn clean compile assembly:single
```

4. After the project has compiled successfully enter this command to run:
```sh
$ java -jar target/news-trends-1.0-jar-with-dependencies.jar 
```

At the prompt, enter the number of days to process: 
```sh
$ java -jar target/news-trends-1.0-jar-with-dependencies.jar
Please enter the number of days to process (1 to 5): 
5
```

After the data has processed, you will be given a link to view the calculated results.
