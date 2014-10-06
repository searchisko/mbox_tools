# mbox Tools

Collection of tools that can be used to:

1. parse mbox files and turn them into JSON objects
2. enhance or override JSON fields with custom metadata
3. send JSON object via HTTP

We use these tools to parse mbox files and archives from services like <https://lists.jboss.org/mailman/listinfo>, we add specific metadata to each JSON and, finally, we send resulting JSON objects to correctly configured Searchisko for indexing.

The code has been tailored specifically for Searchisko API but generally it shouldn't be hard to use these tools in connection with other JSON consuming service/servers, like Elasticsearch or MongoDB.

## How to use it

We will show how to build a command line tool that is used to run sequence of steps to process mbox files in specific way. We use it for indexing mails from Mailman. However, it should be noted that both [mbox parser](mbox_parser) and [mbox indexer](mbox_indexer) can be used on its own and they are available as standalone artefacts.

### Build from source code (Java 1.7 required)

    $ git clone https://github.com/searchisko/mbox_tools;
    $ cd mbox_tools;
    $ mvn clean package;
    
Note: final artefacts will be soon available in mvn repo as well.

### Install

Copy final artefact to `~/mbox_tools` folder and unzip it:

    $ mkdir ~/mbox_tools;
    $ cp assembly/target/mailman_searchisko_integration-bin.zip ~/mbox_tools;
    $ cd ~/mbox_tools;
    $ unzip mailman_searchisko_integration-bin.zip;
    
### Run it

    $ java -jar mailman_searchisko_integration.jar
    
    Invalid parameters!
    Usage: [ -delta ] options...

This tool has two execution modes:

 - Normal mode
 - Delta mode

#### Normal mode

Normal mode is used to parse and push content of a single cumulative mbox archive file into Searchisko. This is used for (re-)indexing from Mailman archive.

    $ java -jar mailman_searchisko_integration.jar -?
    
    java application.jar [options...] arguments...
     -contentType VAL                 : Searchisko provider sys_content_type
     -excludeMessageIdListPath <path> : [optional] path to properties file
                                        containing list of Message-Ids to skip
     -mailListCategory VAL            : mail_list category [dev,users,announce,...et
                                        c]
     -mailListName VAL                : name of mail_list, it is needed for
                                        document URL creation
     -mboxFilePath <path>             : path to mbox file
     -numberOfThreads N               : max threads used for processing tasks
     -numberOffset N                  : [optional] public URL numbering offset
     -password VAL                    : Searchisko provider password (plaintext)
     -serviceHost URI                 : service host URL
     -servicePath VAL                 : service path
     -username VAL                    : Searchisko provider username (plaintext)
    
      Example: java application.jar  -contentType VAL -excludeMessageIdListPath <path> -mailListCategory VAL -mailListName VAL -mboxFilePath <path> -numberOfThreads N -numberOffset N -password VAL -serviceHost URI -servicePath VAL -username VAL

Consult Javadoc for parameters details: [IndexMBoxArchive.java](mbox_indexer/src/main/java/org/searchisko/mbox/task/IndexMboxArchive.java).
    
#### Delta mode

Delta mode is used to index individual message files from given folder and delete those message files that were processed. This is used for indexing of new mails that have been added to the Mailman archive since some time. Typically, this job is started from cron every few minutes. It requires Mailman to mirror a copy of every new incoming mail into specific folder (one can implement a simple Mailman plugin for this).

    $ java -jar mailman_searchisko_integration.jar -delta
    
    java application.jar [options...] arguments...
     -activeMailListsConf VAL : conf file with list of mail lists to include into
                                delta indexing (other files are still deleted!)
     -contentType VAL         : Searchisko provider sys_content_type
     -numberOfThreads N       : max threads used for processing tasks
     -password VAL            : Searchisko provider password (plaintext)
     -pathToDeltaArchive VAL  : path to folder with delta mbox files
     -serviceHost URI         : service host URL
     -servicePath VAL         : service path
     -username VAL            : Searchisko provider username (plaintext)
    
      Example: java application.jar  -activeMailListsConf VAL -contentType VAL -numberOfThreads N -password VAL -pathToDeltaArchive VAL -serviceHost URI -servicePath VAL -username VAL
    
Consult Javadoc for parameters details: [IndexDeltaFolder.java](mbox_indexer/src/main/java/org/searchisko/mbox/task/IndexDeltaFolder.java).
    
## Quick Example of Normal Mode 

The following is example how to build and use prepared command line utility to run sequence of steps which will parse mbox archive, enhance each individual JSON object and send the data to REST service in several parallel threads.  
    
Get some mbox files:

    $ wget http://mail-archives.apache.org/mod_mbox/lucene-java-user/201301.mbox
    
Given Searchisko is properly configured and running at `http://localhost:8080` you can parse and send mbox data to it using the following approach:

    $ java -jar mailman_searchisko_integration.jar \
      -mboxFilePath ./201301.mbox \
      -numberOfThreads 3 \
      -serviceHost http://localhost:8080 \
      -servicePath /v1/rest/content \
      -contentType jbossorg_mailing_list \
      -username jbossorg \
      -password jbossorgjbossorg \
      -mailListName lucene-java \
      -mailListCategory user

## More about mbox format

mbox ([RFC 4155](http://tools.ietf.org/html/rfc4155)) stores mailbox messages in their original
Internet Message ([RFC 2822](http://tools.ietf.org/html/rfc2822)) format, usually in files directly accessible to users.

## License

    Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


