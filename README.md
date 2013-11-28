# "mbox" Integration

Collection of tools that can be used to:

1. parse mbox files and turn them into JSON objects
2. enhance or override JSON fields with custom metadata
3. send JSON object via HTTP

We use these tools to parse mbox files and archives from services like <https://lists.jboss.org/mailman/listinfo>, we add specific metadata to each JSON and, finally, we send resulting JSON objects to correctly configured Searchisko for indexing.

The code has been tailored specifically for Searchisko API but generally it shouldn't be hard to use these tools in connection with other JSON consuming service/servers, like Elasticsearch or MongoDB.

## How to use it

We will show how to build a command line tool that is used to run sequence of steps to process mbox files in specific way. We use it for indexing mails from Mailman. However, it should be noted that both [mbox parser](mbox_parser) and [mbox indexer](mbox_indexer) can be used on its own and they are available as standalone artefacts.

### Build from source code (Java 1.7 required)

    git clone https://github.com/searchisko/mbox_integration;
    cd mbox_integration;
    mvn clean package;
    
Note: final artefacts will be soon available in mvn repo as well.

### Install

Copy final artefact to `~/mbox_tools` folder and unzip it:

    mkdir ~/mbox_tools;
    cp assembly/target/mailman_searchisko_integration-bin.zip ~/mbox_tools;
    cd ~/mbox_tools;
    unzip mailman_searchisko_integration-bin.zip;
    
### Run it

    java -jar mailman_searchisko_integration.jar
    
    # Invalid parameters!
	# Usage: Starter [ -delta | other_params ]

This tool has two execution modes:

 - Normal mode
 - Delta mode

#### Normal mode

Normal mode is used to parse and push content of a single cumulative mbox archive file into Searchisko. This is used for (re-)indexing from Mailman archive.

    java -jar mailman_searchisko_integration.jar -?
    
    # Parameters: mboxFilePath numberOfThreads serviceHost servicePath contentType username password mailListName mailListCategory [numberOffset]
    #
    # mboxFilePath - path to mbox file
    # numberOfThreads - max threads used for processing tasks
    # serviceHost - service host URL
    # servicePath - service path
    # contentType - Searchisko provider sys_content_type
    # username - Searchisko provider username (plaintext)
    # password - Searchisko provider password (plaintext)
    # mailListName - name of mail_list, it is needed for document URL creation
    # mailListCategorygory - mail_list category [dev,users,announce,...etc]
    # numberOffset - public URL numbering offset 

Consult Javadoc for parameters details: [IndexMBoxArchive.java](blob/master/mbox_indexer/src/main/java/org/searchisko/mbox/task/IndexMboxArchive.java).
    
#### Delta mode

Delta mode is used to index individual message files from given folder and delete those message files that were processed. This is used for indexing of new mails that have been added to the Mailman archive since some time. Typically, this job is started from cron every few minutes. It requires Mailman to mirror a copy of every new incoming mail into specific folder (one can implement a simple Mailman plugin for this).

    java -jar mailman_searchisko_integration.jar -delta -?
    
    # Parameters: pathToDeltaArchive numberOfThreads serviceHost servicePath contentType username password activeMailListsConf
    # 
    # pathToDeltaArchive - path to folder with delta mbox files
    # numberOfThreads - max threads used for processing tasks
    # serviceHost - service host URL
    # servicePath - service path
    # contentType - Searchisko provider sys_content_type
    # username - Searchisko provider username (plaintext)
    # password - Searchisko provider password (plaintext)
    # activeMailListsConf - conf file with list of mail lists to include into delta indexing (other files are still deleted!)
    
Consult Javadoc for parameters details: [IndexDeltaFolder.java](blob/master/mbox_indexer/src/main/java/org/searchisko/mbox/task/IndexDeltaFolder.java).
    
## Quick Example of Normal Mode 

The following is example how to build and use prepared command line utility to run sequence of steps which will parse mbox archive, enhance each individual JSON object and send the data to REST service in several parallel threads.  
    
Get some mbox files:

    wget http://mail-archives.apache.org/mod_mbox/lucene-java-user/201301.mbox
    
Given Searchisko is properly configured and running at `http://localhost:8080` you can parse and send mbox data to it using the following approach:

    java -jar mailman_searchisko_integration.jar
    --TBD--

## More about "mbox" format

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


