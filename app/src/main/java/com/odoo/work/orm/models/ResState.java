package com.odoo.work.orm.models;

import android.content.Context;

import com.odoo.work.orm.ColumnType;
import com.odoo.work.orm.OColumn;
import com.odoo.work.orm.OModel;

public class ResState extends OModel {
    public static final String TAG = ResState.class.getSimpleName();

    OColumn name = new OColumn("Name", ColumnType.VARCHAR);
    OColumn country_id = new OColumn("Country", ColumnType.MANY2ONE, "res.country");

    public ResState(Context context) {
        super(context, "res.country.state");
    }
}
