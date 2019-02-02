# Streaming Anomalies Demos #


## Description ##

The scope of this project is to show basic use cases on how the online statistics library can be used to detect
anomalies in data streams.
More information can be found in the [demos](docs/demos.md).

The main advantage of this approach is the small time and space complexity of producing this statistical data, as shown
in the main [**`online-stats`**](https://github.com/tupol/online-stats) library.

In simple terms, each record is processed once and only once and no real history is maintained for the past records.
The only thing that is maintained is the state of each feature from each record, which contains a statistical summary
for that feature.


## Prerequisites ##

* Java 6 or higher
* Scala 2.11 or 2.12
* Apache Spark 2.3.X


## What's new? ##

### 0.0.1 ###
 - project creation
 - basic use cases and demos


## License ##

This code is open source software licensed under the [MIT License](LICENSE).
