package org.altmail.dicttextviewlistener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import static org.altmail.dicttextviewlistener.DictTouchListener.MESSAGE_TAG;
import static org.altmail.dicttextviewlistener.DictTouchListener.RESULT_TAG;

/**
 * DefineTask class extends Android's AsyncTask to allow
 * background-thread tasks. In this case, the task is a network
 * connection and communication with a DICT server.
 *
 * DefineTask<Params, Progress, Result>
 * Params:
 * Progress:
 * Result: LinkedList<String> holding lines of response.
 */

public class DefineTask extends AsyncTask<Void, Void, LinkedList<String> > {

    private final Handler handler;
    private final String server;
    private final int port;
    private final LinkedList<String> commands;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private String message = null;

    DefineTask(Handler handler, String server, int port, LinkedList<String> commands) {
        super();

        this.handler = handler;
        this.server = server;
        this.port = port;
        this.commands = commands;
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected LinkedList<String> doInBackground(Void... v) {
        // Check that there are indeed commands to be sent to a server
        if (commands.isEmpty()) {

            message = Message.INVALID_COMMANDS;

            cancel(true);

            return null;
        }

        // Check that task has not been cancelled
        if (isCancelled())
            return null;

        // Create the socket
        try {

            socket = new Socket(server, port);

        } catch (UnknownHostException e) {

            message = Message.UNKNOWN_HOST + server + ":" + port;

            cancel(true);

            return null;

        } catch (IOException e) {

            message = Message.NETWORK_ERROR;

            cancel(true);

            return null;
        }

        // Check that socket is connected
        if (! socket.isConnected()) {

            message = Message.CANNOT_CONNECT + server + ":" + port;

            cancel(true);

            return null;
        }

        // Check that task has not been cancelled
        if (isCancelled())
            return null;

        LinkedList<String> response = new LinkedList<>();

        try {
            // Create the input and output streams
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send commands to the socket

            for (String command : commands) output.println(command);

            // Read the server response from socket
            String line;

            while ((line = input.readLine()) != null)
                response.add(line);

        } catch (NullPointerException e) {

            message = Message.CANNOT_CONNECT + server + ":" + port;

            cancel(true);

            return null;

        } catch (IOException e) {

            message = Message.NETWORK_ERROR;

            cancel(true);

            return null;
        }

        // Check that task has not been cancelled
        if (isCancelled())
            return null;

        // Close socket and streams
        try {

            output.close();
            input.close();
            socket.close();

        } catch (IOException e) {

            message = Message.NETWORK_ERROR;

            cancel(true);

            return null;
        }

        return response;
    }

    @Override
    protected void onPostExecute(LinkedList<String> response) {

        if (isCancelled()) {

            Bundle data = new Bundle();

            data.putString(MESSAGE_TAG, message);

            android.os.Message msg = android.os.Message.obtain(handler, 2);

            msg.setData(data);
            handler.sendMessage(msg);
        }
        else {
            //activity.displayDefinitions(new DictParser(response));
            Bundle data = new Bundle();

            data.putParcelable(RESULT_TAG, new ParcelableLinkedList(response));

            android.os.Message msg = android.os.Message.obtain(handler, 1);

            msg.setData(data);
            handler.sendMessage(msg);
        }
    }

    @Override
    protected void onCancelled() {

        try {
            // Close the streams and socket
            output.close();
            input.close();
            socket.close();

        } catch (IOException ignored){}

        // Display error
        Bundle data = new Bundle();

        data.putString(MESSAGE_TAG, message);

        android.os.Message msg = android.os.Message.obtain(handler, 2);
        msg.setData(data);
        handler.sendMessage(msg);
    }

    private static class Message {

        private static final String INVALID_COMMANDS		= "Invalid commands to DICT server.";
        private static final String UNKNOWN_HOST			= "Unknown server: ";
        private static final String NETWORK_ERROR			= "Network error.";
        private static final String CANNOT_CONNECT			= "Cannot connect to: ";
        //private static final String CONNECTED				= "Connected to ";
    }
}

