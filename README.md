Meshmail is an application and protocol for sending and receiving email messages over a mesh network. It utilizes inexpensive LoRa radios running Meshtastic.

The app can be run in either client or relay mode: a relay has internet access and manages connections to IMAP and SMTP servers, while the client has only a connection to the mesh.

When a new message arrives at the relay, it is encoded into a protobuf and then split into numerous fragments--binary blobs that are later reconstituted on the client. After generating the fragments, it sends a /message shadow/ to the mesh which informs clients of the existence of the message, as well as a /fingerprint/ (unique hashcode id) and the number of fragments needed. The client then sends fragment requests and the server responds by sending fragment blobs.

