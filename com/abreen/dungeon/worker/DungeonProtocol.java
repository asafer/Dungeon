package com.abreen.dungeon.worker;

import java.util.*;
import com.abreen.dungeon.exceptions.*;
import com.abreen.dungeon.model.*;
import com.abreen.dungeon.DungeonServer;

public class DungeonProtocol {
    public static enum Action {

        /**
         * The action a player issues to move from one room to another.
         */
        MOVE("m", "move", "go", "walk"),

        /**
         * The action a player issues to take an item from the room. The item is
         * then removed from the room and placed in the player's inventory.
         */
        TAKE("t", "take", "get"),

        /**
         * The action a player issues to drop an item from the player's
         * inventory. The item is then placed in the room.
         */
        DROP("d", "drop"),

        /**
         * The action a player issues to give an item to another player. The two
         * players must be in the same room. The item is removed from the giving
         * player's inventory and then placed in the receiving player's
         * inventory.
         */
        GIVE("g", "give"),

        /**
         * The action a player issues to be sent a description of the current
         * room or an item in the room or the player's inventory.
         */
        LOOK("l", "look", "describe"),

        /**
         * The action a player issues to be sent a list of items currently in
         * the player's inventory.
         */
        INVENTORY("i", "inventory"),

        /**
         * The action a player issues to be sent a list of exits out of the
         * current room.
         */
        EXITS("e", "exits"),

        /**
         * The action a player issues to speak. The message is then sent to
         * other players in the current room.
         */
        SAY("s", "say", "talk"),

        /**
         * The action a player issues to yell. The message is sent to players in
         * the current room and adjacent rooms.
         */
        YELL("y", "yell", "shout"),

        /**
         * The action a player issues to whisper to another player. The other
         * player must be in the same room. The message is only seen by the
         * receiving player.
         */
        WHISPER("w", "whisper"),

        /**
         * The action a player issues to use an item in the room or in the
         * player's inventory. The item must be a UseableItem.
         * 
         * @see UseableItem
         */
        USE("u", "use"),

        /**
         * The command a player issues to get a listing of acceptable commands.
         */
        HELP("help"),

        /**
         * The command a player issues to get a listing of currently connected
         * players.
         */
        WHO("who"),

        /**
         * The command a player issues to disconnect.
         */
        QUIT("quit");

        private String[] keys;

        Action(String... keys) {
            this.keys = keys;
        }

        public boolean isAction(String str) {
            for (String key : keys)
                if (key.equalsIgnoreCase(str))
                    return true;
            return false;
        }
        
        public static boolean isValidKey(String str) {
            for (Action a : Action.values())
                if (a.isAction(str))
                    return true;
           
            return false;
        }
    }

    private static DungeonUniverse u = DungeonServer.universe;
    private static DungeonDispatcher d = DungeonServer.events;
    private static DungeonNarrator n = DungeonServer.narrator;

    /**
     * Processes the supplied input from the supplied player's point of view.
     * This method calls mutating methods in the DungeonUniverse class. It is
     * the way connection threads attempt to modify or otherwise access the
     * universe.
     * 
     * @param p
     *            The player object who sent the input string
     * @param input
     *            The input string received from the connected player
     * @throws PlayerIsQuittingException
     * @see DungeonUniverse
     */
    public static void process(Player p, String input)
            throws PlayerIsQuittingException {

        if (input.isEmpty())
            return;

        p.updateLastAction();

        String[] tokens = input.split("\\s");

        Action action = null;
        for (Action a : Action.values())
            if (a.isAction(tokens[0])) {
                action = a;
                break;
            }

        if (action == null) {
            String unsure = "Unsure what is meant by '" + tokens[0] + "'. Try "
                    + "'help' to get a list of valid actions.";
            d.addNotificationEvent(p.getWriter(), unsure);
            return;
        }

        switch (action) {
        case QUIT:
            throw new PlayerIsQuittingException();
        case MOVE:
            processMove(p, tokens);
            return;
        case TAKE:
            processTake(p, tokens);
            return;
        case DROP:
            processDrop(p, tokens);
            return;
        case GIVE:
            processGive(p, tokens);
            return;
        case LOOK:
            processLook(p, tokens);
            return;
        case INVENTORY:
            processInventory(p, tokens);
            return;
        case EXITS:
            processExits(p, tokens);
            return;
        case SAY:
            processSay(p, tokens);
            return;
        case YELL:
            processYell(p, tokens);
            return;
        case WHISPER:
            processWhisper(p, tokens);
            return;
        case USE:
            processUse(p, tokens);
            return;
        case WHO:
            processWho(p, tokens);
            return;
        case HELP:
        default:
            for (String s : usage())
                d.addNotificationEvent(p.getWriter(), s);
            return;
        }

    }

    private static void processDrop(Player p, String[] tokens) {
        String s = getTokensAfterAction(tokens);

        try {
            Item i = u.drop(p, s);

            String narr = n.narrateDrop(DungeonNarrator.toString(p),
                    DungeonNarrator.toString(i,
                            DungeonNarrator.StringType.WITH_ARTICLE));

            Iterator<Player> players = p.here().getPlayers();
            int size = p.here().getNumberOfPlayers();

            d.addNarrationEvent(DungeonDispatcher.playerIteratorToWriterArray(
                    players, size), narr);

        } catch (NoSuchItemException e) {
            String oops = "You do not have an item known as '" + s + "'.";
            d.addNotificationEvent(p.getWriter(), oops);
        }
    }

    private static void processExits(Player p, String[] tokens) {
        String desc = DungeonNarrator.describeExits(p.here());
        d.addNotificationEvent(p.getWriter(), desc);
    }

    private static void processGive(Player p, String[] tokens) {
        String s = getTokensAfterAction(tokens);

        if (s == null) {
            String oops = "Specify an item from your inventory to give, followed "
                    + "by 'to' and the name of the recipient.";
            d.addNotificationEvent(p.getWriter(), oops);
            return;
        }

        int indirectIndex = s.lastIndexOf(" to ");

        if (indirectIndex == -1) {
            String oops = "You must specify a recipient.";
            d.addNotificationEvent(p.getWriter(), oops);
            return;
        }

        String object = s.substring(0, indirectIndex);
        String indirectObject = s.substring(indirectIndex + 4).trim();

        try {
            Item i = u.give(p, object, indirectObject);

            String narr = n.narrateGive(DungeonNarrator.toString(p), object,
                    indirectObject);

            Iterator<Player> ps = u.getPlayersInRoom(p.here());
            int size = u.getNumberOfPlayersInRoom(p.here());

            d.addNarrationEvent(
                    DungeonDispatcher.playerIteratorToWriterArray(ps, size),
                    narr);

        } catch (NoSuchItemException e) {
            String oops = "You do not have an item known as '" + object + "'.";
            d.addNotificationEvent(p.getWriter(), oops);
        } catch (NoSuchPlayerException e) {
            String oops = "There is no such player '" + indirectObject
                    + "' in this room.";
            d.addNotificationEvent(p.getWriter(), oops);
        }
    }

    private static void processInventory(Player p, String[] tokens) {
        String desc = DungeonNarrator.describeInventory(p);
        d.addNotificationEvent(p.getWriter(), desc);
    }

    private static void processLook(Player p, String[] tokens) {
        String tokensAfter = getTokensAfterAction(tokens);

        try {

            if (tokensAfter == null)
                u.look(p, "here");
            else
                u.look(p, tokensAfter);

        } catch (NoSuchItemException e) {
            String oops = "There's no such item by the name '" + tokensAfter
                    + "' in the room or your inventory.";
            d.addNotificationEvent(p.getWriter(), oops);
        }
    }

    private static void processMove(Player p, String[] tokens) {
        try {
            Room here = p.here();
            Room there = u.movePlayer(p, tokens[1]);

            /*
             * Do narration for players watching this player leave. Because
             * getPlayersInRoom would include the moving player if it were
             * called before the player moves, we send the narration here.
             */
            Iterator<Player> playersHere = u.getPlayersInRoom(here);
            int numPlayersHere = u.getNumberOfPlayersInRoom(here);

            String playerString = DungeonNarrator.toString(p);
            String roomString = DungeonNarrator.toString(there,
                    DungeonNarrator.StringType.WITH_ARTICLE);
            String moveTo = n.narrateMoveToRoom(playerString, roomString);
            d.addNarrationEvent(DungeonDispatcher.playerIteratorToWriterArray(
                    playersHere, numPlayersHere), moveTo);

            /*
             * Finally, give the player a description of the new room.
             */
            String[] fakeTokens = { "look", "here" };
            processLook(p, fakeTokens);

        } catch (NoSuchDirectionException e) {
            String oops = "Unsure which direction is meant " + "by '"
                    + tokens[1] + "'. The following directions "
                    + "are recognized: " + Space.listValidDirections();
            d.addNotificationEvent(p.getWriter(), oops);
        } catch (NoSuchExitException e) {
            String oops = "That's not an exit. Try 'exits' for a list of ways "
                    + "out.";
            d.addNotificationEvent(p.getWriter(), oops);
        } catch (LockedDoorException e) {
            String oops = "The door is locked, and you don't have the key.";
            d.addNotificationEvent(p.getWriter(), oops);
        } catch (ArrayIndexOutOfBoundsException e) {
            String oops = "Specify a direction in which to move.";
            d.addNotificationEvent(p.getWriter(), oops);
        }
    }

    private static void processSay(Player p, String[] tokens) {
        String tokensAfter = getTokensAfterAction(tokens);
        u.say(p, tokensAfter);
    }

    private static void processTake(Player p, String[] tokens) {
        String s = getTokensAfterAction(tokens);

        try {
            Item i = u.take(p, s);

            Iterator<Player> ps = u.getPlayersInRoom(p.here());
            int size = u.getNumberOfPlayersInRoom(p.here());
            String narr = n.narrateTake(DungeonNarrator.toString(p),
                    DungeonNarrator.toString(i,
                            DungeonNarrator.StringType.WITH_ARTICLE));
            d.addNarrationEvent(
                    DungeonDispatcher.playerIteratorToWriterArray(ps, size),
                    narr);

        } catch (NoSuchItemException e) {
            String oops = "There is no item '" + s + "' in the room.";
            d.addNotificationEvent(p.getWriter(), oops);
        }
    }

    private static void processUse(Player p, String[] tokens) {
        String oops = "That cannot be used.";
        d.addNotificationEvent(p.getWriter(), oops);
    }

    private static void processWhisper(Player p, String[] tokens) {
        String s = getTokensAfterAction(tokens);

        if (s == null) {
            String oops = "Write a secret message, followed by 'to' and the name "
                    + "of the recipient.";
            d.addNotificationEvent(p.getWriter(), oops);
            return;
        }

        int indirectIndex = s.lastIndexOf(" to ");

        if (indirectIndex == -1) {
            String oops = "You didn't specify a recipient, so you whipsered to "
                    + "yourself.";
            d.addNotificationEvent(p.getWriter(), oops);
            return;
        }

        String message = s.substring(0, indirectIndex);
        String recipient = s.substring(indirectIndex + 4).trim();

        if (p.getName().equals(recipient)) {
            String oops = "OK, you murmur something completely inaudible.";
            d.addNarrationEvent(p.getWriter(), oops);
            return;
        }

        try {
            u.whisper(p, message, recipient);
        } catch (NoSuchPlayerException e) {
            String oops = "There is no such player '" + recipient
                    + "' in this room.";
            d.addNotificationEvent(p.getWriter(), oops);
        }
    }

    private static void processWho(Player p, String[] tokens) {
        Iterator<Player> ps = u.getPlayers();
        int numPlayers = u.getNumberOfPlayers();

        String[] lines = new String[numPlayers + 1];
        
        String fmt = "%-32s%s";

        lines[0] = String.format(fmt, "PLAYER", "LAST HEARD FROM");

        int i = 1;
        while (ps.hasNext()) {
            Player thisPlayer = ps.next();

            String name;
            if (thisPlayer == p)
                name = DungeonNarrator.toString(thisPlayer) + " (you)";
            else
                name = DungeonNarrator.toString(thisPlayer);

            lines[i++] = String.format(fmt, name,
                    thisPlayer.getTimeSinceLastAction());
        }

        for (String line : lines)
            d.addNotificationEvent(p.getWriter(), line);

    }

    private static void processYell(Player p, String[] tokens) {
        String tokensAfter = getTokensAfterAction(tokens);

        if (tokensAfter == null) {
            String oops = "Supply something to yell.";
            d.addNotificationEvent(p.getWriter(), oops);
            return;
        }

        u.yell(p, tokensAfter);
    }

    /*
     * Returns all the tokens after the action as a space-separated string.
     * Returns null if there are no tokens after the action.
     */
    private static String getTokensAfterAction(String[] tokens) {
        if (tokens.length < 2)
            return null;

        int i;
        if (tokens[1].equalsIgnoreCase("the"))
            i = 2;
        else
            i = 1;

        String obj = "";
        for (; i < tokens.length; i++) {
            obj += tokens[i];

            if (i != tokens.length - 1)
                obj += " ";
        }

        return obj;
    }

    private static String[] usage() {
        String[] z = { "ACTION               OBJECT        INDIRECT OBJECT",
                "[{m,move,go,walk}]   <direction>",
                "{t,take,get}         <object>", "{d,drop}",
                "{g,give}             <object>      to <player>",
                "{l,look,describe}    [<object>]", "{i,inventory}",
                "{e,exits}", "{s,say,talk}         [<string>]",
                "{y,yell,shout}       <string>",
                "{w,whisper}          <string>      to <player>",
                "{u,use}              <object in inventory>", "SERVER ACTION",
                "help", "who", "quit" };
        return z;
    }
}
