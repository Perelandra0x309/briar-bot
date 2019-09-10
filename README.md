# BriarBot
Briar Bot will run commands sent from a remote Briar contact to a [briar-headless](https://code.briarproject.org/briar/briar/tree/master/briar-headless) instance running on a PC.  The resulting output of the command is returned to the contact as a message.

Project status: Proof of concept

Current development environment: Linux Mint 19.1, Eclipse IDE, Gradle

##Using BriarBot
First you need to have briar-headless running and add a contact to it from another device running Briar.  You can follow the instruction on the briar-headless Github page to get it running.  You can use the curl command to access the briar-headless API for adding a contact, or you can use some [php scripts](https://github.com/Perelandra0x309/briar-w3-css/tree/master/briar-php-api) I created to make using the API easier.  For example, to add a contact using my script:

```
export AUTH_TOKEN=`cat ~/.briar/auth_token`
php briar-cli.php --action=add_contact --link=[briar link here] --alias=[device name]
```

You can verify the contact was successfully added and send a test message with:

```
php briar-cli.php --action=list_all_contacts
php briar-cli.php --action=write_message --contact_id=[id number] --text=[message]
```

Once you have verified your device and the briar-headless are communicating you can start the BriarBot by cloning this Github to your PC and running this command from a shell in the cloned git repository path:

```
java -jar build/libs/briarbot-0.1.0.jar
```
<blockquote>
Note that currently only contact ID 1 is allowed to send messages to the BriarBot.  Messages from any other contact ID will be rejected.  If you need to change this then find the contact ID you wish to use by listing all contacts from briar-headless and looking for the "contactId" value of the contact you wish to add. Then alter the BriarBot.java class and modify the following line to define your allowed contact ID:

```java
private final int allowedContactId = 1;
```

Then build the jar with gradle:

```
gradle build
```
</blockquote>

Once you run the briar-bot application you should see a successful connection status message:

```
briar-bot$ java -jar build/libs/briarbot-0.1.0.jar 
Sep 09, 2019 8:12:45 PM com.briarbot.client.BriarBot onOpen
INFO: Connected ... 414fdb51-b517-4609-8967-67a7d65a6c48
```

From your other connected device you can now send a message like this:

```
bb [command] [options or other text]
```

Right now the only commands are 'echo' and 'run'.  Using 'echo' will simply send a reply message with any of the text sent in the original message after the 'echo' command'.  Using 'run' will execute the text proceeding the 
run command in a shell.  Any text output by the shell will be returned in a message to your device.  As an example, you can send the message "bb run date" to get the date string from your PC:

![Example](/images/run_example.jpg)

These are only simple examples of what can be sent to a BriarBot enabled PC from a Briar contact.  In a future update I plan to add the ability to have a repository of scripts that can be run from BriarBot. Running scipts will allow much more complicated actions to be performed with a short message from your Briar contact.

Note that one restriction in place now is that the BriarBot will time out and disconnect after 5 minutes.  This version is only meant as a demonstration, not a continually running process.  In future versions this will change.