Bug fixes
=========

The following is a list of outstanding bugs that need fixing.

High priority
-------------
*   Fully implement actions by implementing and using new concurrent methods
    in `DungeonUniverse`
*   "Say" command starting with "the" will drop the "the"
*   Whispering to invalid recipient causes server crash


Low priority
------------
*   The `toString` method in the `Item` class should concatenate article
    and noun
*   Terminal resizing causes crash
*   'who' command does not pluralize multiple minutes
*   No action name should be interpreted as "move" action



Planned features
================
*   Server/client should exchange versions before connecting
*   Server should serialize/deserialize saved worlds
*   Player states should be saved between logins
*   Players should be able to author descriptions of their character
*   "look" action should respond to requests to describe a player in the room
*   Add server logging features
*   Add sentence analysis techniques to choose say-verbs based on vocabulary
    in sentence (e.g., 'think' should choose from 'suggests' or 'surmises')
*   'continues' should be used when a player continues speaking, or the verb
    could be omitted entirely
*   `UseableItem`s should be able to modify the universe and add events
    to the dispatcher
*   There should be pedagogical `UseableItem` implementations like maps,
    compasses, or two-way radios


Feature wishlist
================
*   Servers should have customizable welcome and farewell messages
*   Servers should have a timer to support time-based actions
    -   In-game weather
    -   In-game day and night
*   Servers should send text formatting codes to the client
