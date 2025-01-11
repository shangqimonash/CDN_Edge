## Compile and Run Edge Server
### Build
Run the following command to build the jar file:
```
git clone https://github.com/vietvomonash/CDN_Edge.git
cd CDN_Edge
mvn package
```

### Usage
During the building process, Maven automatically creates a folder `cdn-edge-dist`
* **Preparation**
Create a folder named `data` under `cdn-edge-dist`.

* **Start the Server**
Start the server with the following command:
  ```
  cd cdn-edge-dist
  java -cp "libs/*:EdgeServer-1.0-SNAPSHOT.jar" EdgeServer 0 200 32
  ```

## Build Apache Thrift Interface

### Install Apache Thrift Library
We use Apache Thrift 0.15.0 to implement the client-server communication between Edge and
CS. Please follow the instruction in their official webpage ([link](https://thrift.apache.org/docs/BuildingFromSource))
to build and install the library.

### Build the C Interface
Run the following command to build the C interface

```
cd Thrift-Server/src/main/thrift
thrift --gen cpp -o . server.thrift
```

The above commands generate `gen-cpp` folder under `Thrift-Server/src/main/thrift`. 
You need to copy the `gen-cpp` folder to the CS project.
