The main goal of this code is to parse mbox file and render JSON representation out of it. It is assumed that the JSON file
will be indexed (into Lucene, Elasticsearch, Searchisko, ... etc) thus some content is preprocessed in order to remove
garbage.

It rely on Apache James [Mime4J](http://james.apache.org/mime4j/) when parsing mbox file.

### Possibly similar projects

There are some other libraries that you can use to parse mbox format.

- [Apache Tika](http://tika.apache.org/) (I did not find mbox parser flexible enough for our needs.)
- [ScaleUnlimited/text-similarity](https://github.com/ScaleUnlimited/text-similarity) accompanied by articles:
  [part #1](http://www.scaleunlimited.com/2013/07/10/text-feature-selection-for-machine-learning-part-1/),
  [part #2](http://www.scaleunlimited.com/2013/07/21/text-feature-selection-for-machine-learning-part-2/).
  This is using Tika to parse mbox files as well, however, it discusses interesting related topics.