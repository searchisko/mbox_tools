# "mbox" Integration

Collection of tools that can be used to:

1. parse mbox files and turn them into JSON objects
2. enhance or override JSON objects with custom metadata
3. send JSON object via HTTP

We use these tools to parse mbox files and archives from services like <https://lists.jboss.org/mailman/listinfo>, we add specific metadata to each JSON and, finally, we send resulting JSON objects to correctly configured Searchisko for indexing.

The code has been tailored specifically for Searchisko API but generally it shouldn't be hard to use these tools in connection with other JSON service/servers, like Elasticsearch, MongoDB, … etc.

## How to use it

Get the code and build it (Java 1.7 required):

    git clone https://github.com/searchisko/mbox_integration;
    cd mbox_integration;
    mvn clean package;
    
Get some mbox files:

    --TBD--
    
Given Searchisko is properly configured and running at `http:localhost:8080` you can parse and send mbox data to it using the following approach:

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


