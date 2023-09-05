<!--
Copyright (c) 2010 Yahoo! Inc., 2012 - 2016 YCSB contributors.
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You
may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. See accompanying
LICENSE file.
-->

YCSB+T
====================================

Background
-----
This project is based on version 0.17.0 of [YCSB](https://github.com/brianfrankcooper/YCSB/releases/tag/0.17.0), with additional support for transaction features as discussed in [Dr. Akon Dey](https://www.linkedin.com/in/akon-dey)'s 2014 IEEE [paper](https://ieeexplore.ieee.org/document/6818330). 

Links
-----
* To get here, use https://ycsb.site
* [Our project docs](https://github.com/brianfrankcooper/YCSB/wiki)
* [The original announcement from Yahoo!](https://labs.yahoo.com/news/yahoo-cloud-serving-benchmark/)

Getting Started
---------------

1. Download the [latest release of YCSB](https://github.com/brianfrankcooper/YCSB/releases/latest):

    ```sh
    curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz
    tar xfvz ycsb-0.17.0.tar.gz
    cd ycsb-0.17.0
    ```
    
2. Set up a database to benchmark. There is a README file under each binding directory. NOTE: Also see "Post-setup with IntelliJ" section below.

3. Run YCSB command. 

    On Linux:
    ```sh
    bin/ycsb.sh load basic -P workloads/workloada
    bin/ycsb.sh run basic -P workloads/workloada
    ```

    On Windows:
    ```bat
    bin/ycsb.bat load basic -P workloads\workloada
    bin/ycsb.bat run basic -P workloads\workloada
    ```

  Running the `ycsb` command without any argument will print the usage. 
   
  See https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload
  for a detailed documentation on how to run a workload.

  See https://github.com/brianfrankcooper/YCSB/wiki/Core-Properties for 
  the list of available workload properties.


Building from source
--------------------

YCSB requires the use of Maven 3; if you use Maven 2, you may see [errors
such as these](https://github.com/brianfrankcooper/YCSB/issues/406).

To build the full distribution, with all database bindings:

    mvn clean package

To build a single database binding:

    mvn -pl mongodb-binding -am clean package
    

Post-setup with IntelliJ
----------------------

YCSB provides a mock database, BasicDB, that merely prints any input to the console; this could be used to determine whether YCSB is set up correctly.

After finishing initial setup, IntelliJ run configurations for loading and running (as described in item 3 of "Getting Started") could be created as follows:

<img width="600" alt="ij_load" src="https://github.com/ycsb-with-transactions/ycsb-0.17.0/assets/35314048/96d87fc9-51ee-48c2-9ab6-54850f47a6fd">
<img width="600" alt="ij_run" src="https://github.com/ycsb-with-transactions/ycsb-0.17.0/assets/35314048/d22e361c-22b6-4041-a3d3-ac7d6494d94f">

This allows for easy debugging with IntelliJ, as any breakpoints will be properly recognized (as opposed to loading and running via bash script).
