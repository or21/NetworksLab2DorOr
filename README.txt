=====================================================================================

		Computer Networks Lab #2 - Or Brand, Dor Samet

=====================================================================================


Welcome to our

             |
             |
             |
         /   |   \
         \   |   /
       .  --\|/--  ,
        '--|___|--'
        ,--|___|--,
       '  /\o o/\  `
         +   +   +   crawler,

A) Steps for success:
   ================== 
	
	1. compile.bat compiles our java files to binary files with external jar files for our email service, and copies the "static" directory to the serverroot directory.
	2. run.bat runs the crawler on the port given by the config.ini file, and supplies the crawler with its initial variables

	Be aware that the crawler relies on its "static" files that will be in the serverroot directory, as well as its "jars" files that will be compiled alongside
	with the .java files. The compile.bat file copies the files to their respective directory. 
	Note that the usage of this crawler relies on the copying of these files from its directory. 

B) What has been implemented and added on top of lab 1's server:
   =============================================================
   	
	Main (flow) Classes:
	====================
   	Crawler - Starts the process, and manages the whole flow. It manages its child threads in two arrays (both the Downloaders and the Parsers) by callback events:
	(onAddedResponse, and onAddedUrl). It also is responsible for extracting the information from the HtmlRepository once the worker threads are done.
	HtmlRepository - A singleton class responsible for holding all of the responses, requests about to be sent and their information. It is also responsible for
	creating the statistics data that will be sent to the user.
	Downloader - A thread-class responsible for sending HttpCrawlerRequests as long as there are Urls to be sent, and add the response to the response queue to be parsed.
	Parser - A thread-class responsible for parsing responses, and extracting urls to add to the url queue, as long as there are responses to parse.

	Utility Classes:
	=================
	HtmlParser - takes the content from the given response, and extracts all urls that are inside of the given response. 
	Note that it relies on <img and <a html tags.
	HttpCrawlerRequest - Takes a url, and depending on its type, sends a HEAD (for images, Documents and videos) or a GET (HTML and ASP files) 
	request, and creates a response from it. Note that it deals with 200 OK, and 301, and 302 http redirects accordingly. With other responses,
	it just returns a null response, since the url does not work.
	eLinkType - an enum that represents the link type
	EmailService - Used when sending emails

	Wrapper/Model Classes:
	======================
	Response - A holder/model object for holding 200 OK responses, and its response data. Mainly used as an adapter to pass information around between the downloaders 
	and their parsers

	Old Classes that interact with the crawler:
	===========================================
	GetRequest - Now returns the actual index.html of the crawler. If a get request for the crawler results pages arrives without a referer (meaning it arrived
	straight from the user), returns a 403 Forbidden response.
	PostRequest - Starts the crawler, and returns the appropriate response depending on its given state.
	

C) Main Flow:
   ==========
	Once the user submits the form, the crawler starts up on its own thread (if the crawler was already running, or for some reason failed, 
	an appropriate message will be returned to the user). First, it sets up the file to return its data to the user, and then sends a /robots.txt request, 
	to get its relevant data, and finally starts the producer-consumer cycle, by adding the "/" request to the url queue, and waits for all its
	 child threads to finish. Meanwhile, the Downloaders and Parsers request urls, and parse their responses respectively as follows:
		
		Downloaders will send requests as long as there are urls to be sent. The Downloader, depending on the filetype, will either send a HEAD
		or a GET request. The requests will be sent, and depending on their responses, will be handled differently: If the requets was fine, and 
		a 200 OK was received, the downloader will add the response to the response queue. Otherwise, if the Downloader receives a 301 or 302 http redirect, 
		it will send the request again recursively, until receiving 3 consecutive redirects. If that was the case, it will return a null response, which will
		signify an infinite redirect loop. Otherwise, the Downloader has received a response which was NOT OK, and therefore, will return a null response.
		
		Parsers will handle and parse responses as long as there are responses to be parsed. Links that are external links (see section D) will be sent to a 
		different list to be handled at a later time. The Parsers filter the links according to the definition, meaning that they scan for any links that 
		start with the domain name, or with a /, or are just filenames/folder paths in the server. All of the said links are added to the queue that downloaders
		will later request. Otherwise, these are not relevant links for us to crawl, and are added to the external link queue.

	Once this process is over, meaning there are no threads running, and no urls to be parsed or requested, the crawler turns to the HtmlRepository to aggregate
	all of the statistics. It is important to note here that if a response was null, no parsing was done on it, and therefore it is not added to the statistical report.
	Once the html is ready, it is saved for the user evaluation, and its link is presented in the main page. If the user has chosen to receive the email of the report,
	an email will be sent to the given email address, or the crawler will print out that the email was unreachable, or that there was an internal error (for example
	the libraries and jars were not compiled with the .java files...)

D) Assumptions and other considerations
=======================================
	1) External links were defined as links that are not part of the domain, or that are unreachable	
	2) If a link returns a response that is not 200 OK (404 not found for example), then it is not a valid link to crawl or run statistics.
	3) The crawler only crawls to internal links, as given by the definition above.
	4) All the statistics refer to internal links, as given by the definition above.
	5) The crawler does not count its own domain when presenting domains the crawled domain was connected to
	6) In the port scanner, and initial check to see it a valid domain - if a url is not reachable within 100 ms, we assume it is not a valid url.