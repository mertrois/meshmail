![Alt text](/images/logo_small.png?raw=true "logo")

Meshmail is an application and protocol for sending and receiving email messages over a mesh network. It utilizes inexpensive LoRa radios running Meshtastic. 

The app can be run in either client or relay mode: a relay has internet access and manages connections to IMAP and SMTP servers, while the client has only a connection to the mesh.

When a new message arrives at the relay, it is encoded into a protobuf and then split into numerous fragments--binary blobs that are later reconstituted on the client. After generating the fragments, the relay sends a *message shadow* to the mesh which informs clients of the existence of the message. The shadow contains a preview of the content (subject and sender), as well as a *fingerprint* (a unique hashcode id), and the number of fragments used. The client then proceeds to send fragment requests and the relay responds by sending fragment blobs. Once the client has all the expected fragments, it concatenates them and reconstitutes the protobuf. Outbound mail is handled by the same protocol, but with the tag of *OUTBOUND* indicating to the relay that it needs to be  transmitted over SMTP.

![Alt text](/images/relay.png?raw=true "Relay Dashboard")
![Alt text](/images/incoming.png?raw=true "Inbox")
![Alt text](/images/forwarding.png?raw=true "Responding")
![Alt text](/images/settings.png?raw=true "Settings")
