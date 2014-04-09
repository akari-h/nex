This is NEX project: Naru's EXI proxy for XMPP

by Akari Harada naru@sfc.wide.ad.jp

prerequisite:
=============

- java 6 and later
- ant

compile:
=============

% ant jar

launch translator

    % java -jar build/jar/nex.jar

by default, it targets port 7 of localhost (echo server). To make it pointed on XMPP server, you may invoke nex.jar with

    % java -jar build/jar/nex.jar -p 5222 

to enable EXI (and to specify which schema to be used)

    % java -jar build/jar/nex.jar -p 5222 -s sample/schema_localized/00_canonical_example01.xsd

Of course, schema should match with the client and server.


Note on EXIficient:
=============

- this version of EXIficient is patched to support continuous stream
- patch is against rev.374 of EXIficient trunk (quite old! sorry)


License:
=============
GPLv2

