# TCP to Californium proxy

A TCP server acting as a proxy to
[Californium](https://github.com/mkovatsc/Californium).
It's a companion to the node.js client available at
[californium-proxy-node](https://github.com/morkai/californium-proxy-node).

## License

This project is released under the [MIT License](http://opensource.org/licenses/mit-license.php).

## Requirements

### Java

Version 1.6.0_37 or later available from [java.com](http://java.com/en/download/manual.jsp).

### [Naga - Simplified Java NIO asynchronous sockets](http://code.google.com/p/naga/)

Version 3.0 without EventMachine. JAR is included in the repository
for convenience (`lib/naga-no-em-3_0.jar`).

### [My fork](https://github.com/morkai/Californium) of the [Californium - CoAP framework in Java](https://github.com/mkovatsc/Californium)

JAR is included in the repository for convenience
(`lib/morkai-californium-0.8.4-SNAPSHOT.jar`).

## Usage

Build the JAR by executing the `californium-proxy-java/build`
script to create the `californium-proxy-java/run/cf-proxy.jar` file.

Run the server by executing:
```
cd califorium-proxy-java
java -cp run/cf-proxy.jar;lib/naga-no-em-3_0.jar;lib/morkai-californium-0.8.4-SNAPSHOT.jar CoapProxyServer
```
on Windows, or:
```
cd califorium-proxy-java
java -cp run/cf-proxy.jar:lib/naga-no-em-3_0.jar:lib/morkai-californium-0.8.4-SNAPSHOT.jar CoapProxyServer
```
on Linux.

Alternatively, use the bundled executable JAR:
```
java -jar califorium-proxy-java/run/cf-proxy-0.0.1.jar
```

By default, the TCP server will listen on port `1337` and output
all log messages. To change that, use the `--port` and `--log`
arguments.

The log argument expects a one of the [`java.util.logging.Level`](http://docs.oracle.com/javase/7/docs/api/java/util/logging/Level.html#field_summary)
values (case insensitive).
Use `OFF` to disable all messages.

For example, to silently listen on port `3000`:
```
java -jar califorium-proxy-java/run/cf-proxy-0.0.1.jar --port 3000 --log OFF
```

## Protocol

The server expects and sends packets of the following format:
```
<PACKET LENGTH><REQUEST ID><COAP FRAME>
```
Where:
  * `PACKET LENGTH` is a 32-bit unsigned integer and must be equal
    to a length of the `COAP FRAME+2`.
  * `REQUEST ID` is a 16-bit unsigned integer identifying this CoAP
    request (or a CoAP response to a request if send by the server).
    It's used by the client to easily match the responses to
    the requests.
  * `COAP FRAME` is a variable length, valid CoAP frame
    (request frame, if send by the client; response frame if send by
    the server).

See [Proxy.js](https://github.com/morkai/californium-proxy-node/blob/master/lib/Proxy.js)
from the [californium-proxy-node](https://github.com/morkai/californium-proxy-node)
project for an example implementation.
