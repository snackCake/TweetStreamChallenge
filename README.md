#TweetStreamChallenge

This is a Play 2.5 application that uses Akka Streams and Twitter4J to stream Twitter data via comet. It also stores
all streamed tweets into a database (MySQL).

##Setup

To initially setup the database in your local environment, install Vagrant (https://www.vagrantup.com/) and
VirtualBox (https://www.virtualbox.org/). Then, from the `mysql-vagrant` directory, run the following command:
`vagrant up`

After downloading and provisioning, the Vagrant/VirtualBox instance should be up and running with MySQL. The database
`play` should have been created (but will be empty). The needed table(s) will be created automatically on startup of
the application.

##Running

Use these instructions to get Twitter app keys and tokens: https://dev.twitter.com/oauth/overview/application-owner-access-tokens

To run, make sure the JVM is running with the appropriate Twitter4J system properties:
`./activator run -Dtwitter4j.debug=true -Dtwitter4j.oauth.consumerKey=*************** -Dtwitter4j.oauth.consumerSecret=*************** -Dtwitter4j.oauth.accessToken=*************** -Dtwitter4j.oauth.accessTokenSecret=***************`

Then, view the site at: http://localhost:9000

## Persisted Tweets

All streamed tweets will be persisted to the `play.tweet` table. You can view these by connecting to the running MySQL
instance in your Vagrant box. The database connection information can be seen by looking at the appropriate section
in the Play `application.conf`.

## Challenge 1

After 7 tweets, the 8th tweet will cause an error. Your challenge: fix it.

`[error] a.a.OneForOneStrategy - It is illegal to throw exceptions from request(), rule 3.16
akka.stream.impl.ReactiveStreamsCompliance$SignalThrewException: It is illegal to throw exceptions from request(), rule 3.16
	at akka.stream.impl.ReactiveStreamsCompliance$.tryRequest(ReactiveStreamsCompliance.scala:111)
	at akka.stream.impl.fusing.ActorGraphInterpreter$BatchingActorInputBoundary.akka$stream$impl$fusing$ActorGraphInterpreter$BatchingActorInputBoundary$$dequeue(ActorGraphInterpreter.scala:118)
	at akka.stream.impl.fusing.ActorGraphInterpreter$BatchingActorInputBoundary.onNext(ActorGraphInterpreter.scala:145)
	at akka.stream.impl.fusing.GraphInterpreterShell.receive(ActorGraphInterpreter.scala:386)
	at akka.stream.impl.fusing.ActorGraphInterpreter$$anonfun$receive$1.applyOrElse(ActorGraphInterpreter.scala:547)
	at akka.actor.Actor$class.aroundReceive(Actor.scala:480)
	at akka.stream.impl.fusing.ActorGraphInterpreter.aroundReceive(ActorGraphInterpreter.scala:493)
	at akka.actor.ActorCell.receiveMessage(ActorCell.scala:526)
	at akka.actor.ActorCell.invoke(ActorCell.scala:495)
	at akka.dispatch.Mailbox.processMailbox(Mailbox.scala:257)
Caused by: java.lang.NullPointerException: null
	at akka.stream.impl.ReactiveStreamsCompliance$.tryRequest(ReactiveStreamsCompliance.scala:110)
	at akka.stream.impl.fusing.ActorGraphInterpreter$BatchingActorInputBoundary.akka$stream$impl$fusing$ActorGraphInterpreter$BatchingActorInputBoundary$$dequeue(ActorGraphInterpreter.scala:118)
	at akka.stream.impl.fusing.ActorGraphInterpreter$BatchingActorInputBoundary.onNext(ActorGraphInterpreter.scala:145)
	at akka.stream.impl.fusing.GraphInterpreterShell.receive(ActorGraphInterpreter.scala:386)
	at akka.stream.impl.fusing.ActorGraphInterpreter$$anonfun$receive$1.applyOrElse(ActorGraphInterpreter.scala:547)
	at akka.actor.Actor$class.aroundReceive(Actor.scala:480)
	at akka.stream.impl.fusing.ActorGraphInterpreter.aroundReceive(ActorGraphInterpreter.scala:493)
	at akka.actor.ActorCell.receiveMessage(ActorCell.scala:526)
	at akka.actor.ActorCell.invoke(ActorCell.scala:495)
	at akka.dispatch.Mailbox.processMailbox(Mailbox.scala:257)`
