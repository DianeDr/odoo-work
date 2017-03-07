package com.odoo.work.orm.sync;

import android.content.ContentValues;
import android.content.SyncResult;

import com.odoo.core.rpc.helper.utils.gson.OdooRecord;
import com.odoo.work.orm.OColumn;
import com.odoo.work.orm.OModel;
import com.odoo.work.utils.DataUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class OdooRecordUtils {

    private OModel model;
    private SyncResult syncResult;
    private List<OColumn> columns = new ArrayList<>();
    private List<ContentValues> recordValuesToUpdate = new ArrayList<>();
    private List<ContentValues> recordValuesToInsert = new ArrayList<>();
    private HashMap<Integer, HashMap<String, List<Integer>>> recordToMap = new HashMap<>();
    private HashMap<String, HashSet<Integer>> relationRecordToSync = new HashMap<>();

    public static OdooRecordUtils getInstance(OModel model, SyncResult syncResult) {
        return new OdooRecordUtils(model, syncResult);
    }

    private OdooRecordUtils(OModel model, SyncResult syncResult) {
        this.model = model;
        this.syncResult = syncResult;
        columns.addAll(model.getColumns());
    }

    public void processRecord(OdooRecord record) {
        ContentValues values = new ContentValues();
        for (OColumn column : columns) {
            if (!column.isLocal) {
                switch (column.columnType) {
                    case DATETIME:
                    case VARCHAR:
                    case BLOB:
                        values.put(column.name, record.getString(column.name));
                        break;
                    case BOOLEAN:
                        break;
                    case FLOAT:
                        values.put(column.name, record.getDouble(column.name));
                        break;
                    case INTEGER:
                        values.put(column.name, record.getDouble(column.name).intValue());
                        break;
                    case MANY2ONE:
                        OdooRecord m2o = record.getM20(column.name);
                        Integer m2o_row_id = null;
                        if (m2o != null) {
                            OModel m2oModel = model.createModel(column.relModel);
                            ContentValues m2oValues = new ContentValues();
                            m2oValues.put("id", m2o.getDouble("id").intValue());
                            m2oValues.put("name", m2o.getString("name"));
                            m2o_row_id = m2oModel.updateOrCreate(m2oValues, "id = ? ", m2o.getInt("id") + "");
                            if (m2oModel.getServerColumns().length > 2) {
                                addRelRecordToSync(column.relModel, m2o.getDouble("id").intValue());
                            }
                        }
                        values.put(column.name, m2o_row_id);
                        break;
                    case MANY2MANY:
                        List<Integer> ids = DataUtils.getDoubletToInt(record.getArray(column.name));
                        for (int id : ids) {
                            addRelRecordToSync(column.relModel, id);
                        }
                        HashMap<String, List<Integer>> idsMap = new HashMap<>();
                        idsMap.put(column.name, ids);
                        recordToMap.put(record.getInt("id"), idsMap);
                        break;
                    // TODO : Many to Many and One To Many
                }
            }
        }
        int row_id = model.selectRowId(record.getInt("id"));
        if (row_id == OModel.INVALID_ROW_ID) {
            recordValuesToInsert.add(values);
        } else {
            values.put("_id", row_id);
            recordValuesToUpdate.add(values);
        }
    }

    private void addRelRecordToSync(String model, int server_id) {
        HashSet<Integer> ids = new HashSet<>();
        if (relationRecordToSync.containsKey(model)) {
            ids.addAll(relationRecordToSync.get(model));
        }
        ids.add(server_id);
        relationRecordToSync.put(model, ids);
    }

    public List<ContentValues> getRecordValuesToInsert() {
        return recordValuesToInsert;
    }

    public List<ContentValues> getRecordValuesToUpdate() {
        return recordValuesToUpdate;
    }

    public HashMap<String, HashSet<Integer>> getRelationRecordToSync() {
        return relationRecordToSync;
    }

    public HashMap<Integer, HashMap<String, List<Integer>>> getRecordToMap() {
        return recordToMap;
    }
}
