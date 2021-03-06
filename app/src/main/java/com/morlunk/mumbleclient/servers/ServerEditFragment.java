/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.morlunk.mumbleclient.servers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.morlunk.jumble.Constants;
import com.morlunk.jumble.model.Server;
import com.morlunk.mumbleclient.R;
import com.morlunk.mumbleclient.Settings;
import com.morlunk.mumbleclient.db.DatabaseProvider;

public class ServerEditFragment extends DialogFragment {

    public interface ServerEditListener {
        public void serverInfoUpdated();
        public void connectToServer(Server server);
    }

    private TextView mNameTitle;
	private EditText mNameEdit;
	private EditText mHostEdit;
	private EditText mPortEdit;
	private EditText mUsernameEdit;
    private EditText mPasswordEdit;
	
	private ServerEditListener mListener;
    private DatabaseProvider mDatabaseProvider;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

        try {
            mDatabaseProvider = (DatabaseProvider) activity;
            mListener = (ServerEditListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()+" must implement DatabaseProvider and ServerEditListener!");
        }
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());

        Settings settings = Settings.getInstance(getActivity());

		View view = inflater.inflate(R.layout.dialog_server_edit, null, false);
		alertBuilder.setView(view);

        mNameTitle = (TextView) view.findViewById(R.id.server_edit_name_title);
		mNameEdit = (EditText) view.findViewById(R.id.server_edit_name);
		mHostEdit = (EditText) view.findViewById(R.id.server_edit_host);
		mPortEdit = (EditText) view.findViewById(R.id.server_edit_port);
		mUsernameEdit = (EditText) view.findViewById(R.id.server_edit_username);
        mUsernameEdit.setHint(settings.getDefaultUsername());
        mPasswordEdit = (EditText) view.findViewById(R.id.server_edit_password);
		if (getServer() != null) {
            Server oldServer = getServer();
			mNameEdit.setText(oldServer.getName());
			mHostEdit.setText(oldServer.getHost());
			mPortEdit.setText(String.valueOf(oldServer.getPort()));
			mUsernameEdit.setText(oldServer.getUsername());
            mPasswordEdit.setText(oldServer.getPassword());
		}

        if(!shouldSave()) {
            mNameTitle.setVisibility(View.GONE);
            mNameEdit.setVisibility(View.GONE);
        }

        int actionTitle;
        if(shouldSave() && getServer() == null) {
            actionTitle = R.string.add;
        } else if(shouldSave()) {
            actionTitle = R.string.save;
        } else {
            actionTitle = R.string.connect;
        }
		
		alertBuilder.setPositiveButton(actionTitle, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Server server = createServer(shouldSave());

                // If we're not committing this server, connect immediately.
                if(!shouldSave()) mListener.connectToServer(server);

                dismiss();
            }
        });

		alertBuilder.setNegativeButton(android.R.string.cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
		
		alertBuilder.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dismiss();
            }
        });

		return alertBuilder.create();
	}

    private boolean shouldSave() {
        return getArguments() == null || getArguments().getBoolean("save", true);
    }

    private Server getServer() {
        return getArguments() != null ? (Server) getArguments().getParcelable("server") : null;
    }

    /**
     * Creates or updates a server with the information in this fragment.
     * @param shouldCommit Whether to commit the created service to the DB.
     * @return The new or updated server.
     */
	public Server createServer(boolean shouldCommit) {
		String name = (mNameEdit).getText().toString().trim();
		String host = (mHostEdit).getText().toString().trim();

		int port;
		try {
			port = Integer.parseInt((mPortEdit).getText().toString());
		} catch (final NumberFormatException ex) {
			port = Constants.DEFAULT_PORT;
		}

		String username = (mUsernameEdit).getText().toString().trim();
        String password = mPasswordEdit.getText().toString();

        if(username.equals(""))
            username = mUsernameEdit.getHint().toString();

        Server server;

		if (getServer() != null) {
            server = getServer();
            server.setName(name);
            server.setHost(host);
            server.setPort(port);
            server.setUsername(username);
            server.setPassword(password);
			if(shouldCommit) mDatabaseProvider.getDatabase().updateServer(server);
		} else {
            server = new Server(-1, name, host, port, username, password);
			if(shouldCommit) mDatabaseProvider.getDatabase().addServer(server);
		}

        if(shouldCommit) mListener.serverInfoUpdated();

        return server;
	}
}
