package com.odoo.work;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.handler.OdooVersionException;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.rpc.listeners.OdooResponse;
import com.odoo.core.support.OUser;
import com.odoo.work.addons.teams.models.ProjectTeams;
import com.odoo.work.core.support.OdooActivity;

public class WizardNewTeam extends OdooActivity implements View.OnClickListener {

    private Odoo odoo;
    private EditText editTeamName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_name);

        try {
            odoo = Odoo.createWithUser(this, OUser.current(this));
        } catch (OdooVersionException e) {
            e.printStackTrace();
        }

        editTeamName = (EditText) findViewById(R.id.team_name);
        findViewById(R.id.btn_team_create).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (!editTeamName.getText().toString().isEmpty()) {
            OUser user = OUser.current(this);
            assert user != null;
            final ORecordValues values = new ORecordValues();
            values.put("name", editTeamName.getText().toString());
            odoo.createRecord("project.teams", values, new OdooResponse() {
                @Override
                public void onResponse(OdooResult response) {
                    int team_id = response.getInt("result");
                    ProjectTeams teams = new ProjectTeams(WizardNewTeam.this);
                    ContentValues data = new ContentValues();
                    data.put("id", team_id);
                    data.put("name", values.getString("name"));
                    teams.create(data);
                    addNewMembers(team_id);
                }
            });
        }
    }

    private void addNewMembers(int team_id) {
        Intent intent = new Intent(this, WizardAddTeamMembers.class);
        intent.putExtra(WizardAddTeamMembers.TEAM_ID, team_id);
        startActivity(intent);
        finish();
    }
}