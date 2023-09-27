package org.altmail.dicttextviewlistener;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * DictParser
 * **********
 * Receives full server response as List, parses the results,
 * and returns the information that should be displayed to the user by the Activity.
 */

public class DictParser {

    private boolean error = false;
    private String message = null;
    private LinkedList<String> result = new LinkedList<>();

    /**
     * Constructor; expects the server's side of the conversation in the form:
     *
     * 220 [ connected ]
     * nnn [ server response code ]
     * [ possible text responses, separated by '\r\n.\r\n' ]
     * 250 [ command ok ]
     * 221 [ server disconnect ]
     *
     * @param conversation - LinkedList containing lines of the server conversation
     */

    DictParser(LinkedList<String> conversation) {

        // Process the conversation
        if (conversation == null) {

            // Network error
            error = true;
            message = Message.NETWORK_ERROR;

            return;

        } else if (conversation.isEmpty()) {
            // No server response
            error = true;
            message = Message.NO_SERVER_RESPONSE;

            return;
        }

        final ListIterator<String> i = conversation.listIterator();

        // Get the first line of the List.
        // This should be the successful/unsuccessful connection status.
        String line = i.next();
        int code;

        try {

            code = Integer.parseInt(line.substring(0, Code.LENGTH));

        } catch (NumberFormatException e) {
            // Bad code in first line of response
            error = true;
            message = Message.BAD_SERVER_RESPONSE;

            return;
        }
        // Check possible server responses
        switch (code) {
            // Connection successful
            case Code.CONNECTED:

                error = false;

                break;
            // Negative responses
            case Code.ACCESS_DENIED:

                error = true;
                message = Message.ACCESS_DENIED;

                return;

            case Code.SERVER_UNAVAILABLE:

                error = true;
                message = Message.SERVER_UNAVAILABLE;

                return;

            case Code.SERVER_SHUTTING_DOWN:

                error = true;
                message = Message.SERVER_SHUTTING_DOWN;

                return;

            default:

                error = true;
                message = Message.UNKNOWN_CODE + code;

                return;
        }

        // Connected to server successfully
        // Parse responses
        while (i.hasNext()) {

            line = i.next();

            try {

                code = Integer.parseInt(line.substring(0, Code.LENGTH));

            } catch (NumberFormatException e) {
                // Bad code in server response
                error = true;
                message = Message.BAD_SERVER_RESPONSE;

                return;
            }
            // Check server response
            switch (code) {
                // Positive responses
                case Code.OK:						// 250
                    // Command succeeded; do nothing and parse next line
                    break;

                case Code.NUMBER_DEFINITIONS:		// 150
                    // 150 n definitions retrieved
                    int num;

                    try {
                        // Extract the first parameter of the 150 response
                        num = Integer.parseInt(line.split(" ")[1]);

                    } catch (NumberFormatException e) {
                        // Bad info in server response
                        error = true;
                        message = Message.BAD_SERVER_RESPONSE;

                        return;
                    }

                    // Process the definition lines one by one
                    for (int j = 0; j < num; ++j) {

                        line = i.next();

                        try {
                            // Check that definition begins here
                            code = Integer.parseInt(line.substring(0, Code.LENGTH));

                        } catch (NumberFormatException e) {
                            // Bad server response
                            error = true;
                            message = Message.BAD_SERVER_RESPONSE;

                            return;
                        }

                        if (code != Code.DEFINITION) {

                            error = true;
                            message = Message.UNEXPECTED_CODE + String.valueOf(code);

                            return;
                        }

                        // Process definition
                        // First line has database info
                        // 151 "word" database "Database description"
                        result.add("From " + line.split(" ", 4)[3]);

                        final StringBuilder definition = new StringBuilder();

                        while (i.hasNext()) {

                            line = i.next();
                            // Check for end of definition, signified by a single period
                            // on a line by itself (".")
                            // Note that lines part of a definition that begin with a period
                            // will have that period doubled (see rfc 2229)
                            if (line.length() == 1 && line.charAt(0) == '.') {
                                // Append definition to the end of the list
                                definition.append(SEPARATOR + NEWLINE);

                                result.add(definition.toString());
                                // Process next definition
                                break;
                            }
                            // Append line to the definition
                            definition.append(line).append(NEWLINE);
                        }
                    }

                    break;

                case Code.CONNECTION_CLOSED:		// 221
                    // Finished parsing successfully
                    return;

                case Code.NO_MATCH:					// 552

                    result.add(Message.NO_MATCH);

                    break;

                /* UNIMPLEMENTED */
                case Code.NUMBER_DATABASES:			// 110
                case Code.NUMBER_STRATEGIES:		// 111
                case Code.DATABASE_INFORMATION:		// 112
                case Code.HELP_INFORMATION:			// 113
                case Code.SERVER_INFORMATION:		// 114
                case Code.CHALLENGE:				// 130
                case Code.NUMBER_MATCHES:			// 152
                    // Negative responses
                case Code.SERVER_UNAVAILABLE:		// 420
                case Code.SERVER_SHUTTING_DOWN:		// 421
                case Code.UNKNOWN_COMMAND:			// 500
                case Code.ILLEGAL_PARAMETERS:		// 501
                case Code.COMMAND_NOT_IMPLEMENTED:	// 502
                case Code.COMMAND_PARAM_NOT_IMPLEMENTED:// 503
                case Code.ACCESS_DENIED:			// 530
                case Code.ACCESS_DENIED_AUTH:		// 531
                case Code.ACCESS_DENIED_MECHANISM:	// 532
                case Code.INVALID_DATABASE:			// 550
                case Code.INVALID_STRATEGY:			// 551
                case Code.NO_DATABASES:				// 554
                case Code.NO_STRATEGIES:			// 555
                default:

                    error = true;
                    message = Message.UNIMPLEMENTED_CODE + code;

                    return;
            }
        }
    }

    public DictParser(String raw) {}

    /* Accessors */

    /**
     * Reports whether there was an error parsing the server response.
     * @return true if error, false otherwise.
     */
    public boolean parseError() { return error; }

    /**
     * Reports the type of error that occured. For debugging.
     * @return String containing the parser error message.
     */
    public String errorMessage() { return message; }

    /**
     * Access to the parsed results list.
     * @return List of the parsed results, each result a String.
     */
    LinkedList<String> result() { return result; }

    private static final String SEPARATOR				= "--------------------";
    static final String NEWLINE				= "\n";

    private static class Code {

        private static final int LENGTH					= 3;	// the standard length of all codes (ie. 3-digit number)

        /* Response codes from the server */
        // Positive Preliminary reply
        private static final int NUMBER_DATABASES		= 110;	// <N> databases present - text follows
        private static final int NUMBER_STRATEGIES		= 111;	// <N> strategies available - text follows
        private static final int DATABASE_INFORMATION	= 112;	// Database information follows
        private static final int HELP_INFORMATION		= 113;	// Help text follows
        private static final int SERVER_INFORMATION		= 114;	// Server information follows
        private static final int CHALLENGE				= 130;	// Challenge follows
        private static final int NUMBER_DEFINITIONS		= 150;	// <N> definitions retrieved - definitions follow
        private static final int DEFINITION				= 151;	// <Word> <database> <name> - text follows
        private static final int NUMBER_MATCHES			= 152;	// <N> matches found - text follows
        // Positive Completion reply
//		private static final int STATUS_INFORMATION		= 210;	// (Optional timing and statistical information here)
        private static final int CONNECTED				= 220;	// <Text> <capabilities> <msg-id>
        private static final int CONNECTION_CLOSED		= 221;	// Closing connection
        //		private static final int AUTH_SUCCESSFUL		= 230;	// Authentication successful
        private static final int OK						= 250;	// Ok (optional timing information here)
        // Positive Intermediate reply
//		private static final int SEND_RESPONSE			= 330;	// Send response
        // Transient Negative Completion reply
        private static final int SERVER_UNAVAILABLE		= 420;	// Server temporarily unavailable
        private static final int SERVER_SHUTTING_DOWN	= 421;	// Server shutting down at operator request
        // Permanent Negative Completion reply
        private static final int UNKNOWN_COMMAND		= 500;	// Syntax error, command not recognized
        private static final int ILLEGAL_PARAMETERS		= 501;	// Syntax error, illegal parameters
        private static final int COMMAND_NOT_IMPLEMENTED= 502;	// Command not implemented
        private static final int COMMAND_PARAM_NOT_IMPLEMENTED = 503;	// Command parameter not implemented
        private static final int ACCESS_DENIED			= 530;	// Access denied
        private static final int ACCESS_DENIED_AUTH		= 531;	// Access denied, use "SHOW INFO" for server information
        private static final int ACCESS_DENIED_MECHANISM= 532;	// Access denied, unknown mechanism
        private static final int INVALID_DATABASE		= 550;	// Invalid database, use "SHOW DB" for list of databases
        private static final int INVALID_STRATEGY		= 551;	// Invalid strategy, use "SHOW STRAT" for a list of strategies
        private static final int NO_MATCH				= 552;	// No match
        private static final int NO_DATABASES			= 554;	// No databases present
        private static final int NO_STRATEGIES			= 555;	// No strategies available
    }

    private static class Message {

        /* Parser error messages */
        private static final String NETWORK_ERROR			= "Network error";
        private static final String NO_SERVER_RESPONSE		= "No server response";
        private static final String BAD_SERVER_RESPONSE		= "Bad server response";
        private static final String UNKNOWN_CODE			= "Unknown code: ";
        private static final String UNEXPECTED_CODE			= "Unexpected code: ";
        private static final String UNIMPLEMENTED_CODE		= "Unimplemented code: ";
        /* Server response messages */

        // Positive Completion reply
        // Transient Negative Completion reply
        private static final String SERVER_UNAVAILABLE		= "Server temporarily unavailable";										// 420
        private static final String SERVER_SHUTTING_DOWN	= "Server shutting down at operator request";							// 421
        // Permanent Negative Completion reply
        private static final String ACCESS_DENIED			= "Access denied";														// 530
        private static final String NO_MATCH				= "No match";															// 552
    }
}